package com.lsl.irc_android.ui.home

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lsl.irc_android.data.ImageUploadManager
import com.lsl.irc_android.irc.IrcClient
import com.lsl.irc_android.irc.IrcMessage
import com.lsl.irc_android.notification.NotificationHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _connectionState = MutableLiveData<ConnectionState>().apply {
        value = ConnectionState.DISCONNECTED
    }
    val connectionState: LiveData<ConnectionState> = _connectionState
    
    private val _messages = MutableLiveData<MutableList<ChatMessage>>().apply {
        value = mutableListOf()
    }
    val messages: LiveData<MutableList<ChatMessage>> = _messages
    
    private val _statusMessage = MutableLiveData<String>().apply {
        value = "未连接"
    }
    val statusMessage: LiveData<String> = _statusMessage
    
    private val _currentChannel = MutableLiveData<String?>()
    val currentChannel: LiveData<String?> = _currentChannel
    
    // 频道列表数据
    private val _channelList = MutableLiveData<List<ChannelInfo>>()
    val channelList: LiveData<List<ChannelInfo>> = _channelList
    
    private var ircClient: IrcClient? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val channelListBuffer = mutableListOf<ChannelInfo>()
    
    // 通知辅助类
    private val notificationHelper = NotificationHelper(application)
    
    // 图片上传管理器
    private val imageUploadManager = ImageUploadManager(application)
    
    // 图片上传状态
    private val _uploadingImage = MutableLiveData<Boolean>().apply {
        value = false
    }
    val uploadingImage: LiveData<Boolean> = _uploadingImage
    
    // 上传后的图片 URL
    private val _uploadedImageUrl = MutableLiveData<String?>()
    val uploadedImageUrl: LiveData<String?> = _uploadedImageUrl
    
    // MOTD 和服务器消息收集
    private val serverMessageBuffer = mutableListOf<String>()
    private var isCollectingMotd = false
    private var serverMessageId: Long? = null
    
    fun connect(server: String, port: Int, nickname: String) {
        _connectionState.value = ConnectionState.CONNECTING
        _statusMessage.value = "正在连接到 $server:$port..."
        
        ircClient = IrcClient(
            onMessageReceived = { message -> handleIrcMessage(message) },
            onConnectionStateChanged = { connected -> 
                if (connected) {
                    _connectionState.postValue(ConnectionState.CONNECTED)
                    _statusMessage.postValue("已连接到 $server:$port")
                    addSystemMessage("已连接到 $server:$port")
                } else {
                    if (_connectionState.value != ConnectionState.DISCONNECTED) {
                        _connectionState.postValue(ConnectionState.ERROR)
                        _statusMessage.postValue("连接丢失")
                        addSystemMessage("连接丢失")
                    }
                }
            },
            onError = { error ->
                _connectionState.postValue(ConnectionState.ERROR)
                _statusMessage.postValue("错误: $error")
                addSystemMessage("错误: $error")
            }
        )
        
        viewModelScope.launch {
            try {
                ircClient?.connect(server, port, nickname)
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.ERROR
                _statusMessage.value = "连接失败: ${e.message}"
                addSystemMessage("连接失败: ${e.message}")
            }
        }
    }
    
    fun disconnect() {
        ircClient?.disconnect()
        _connectionState.value = ConnectionState.DISCONNECTED
        _statusMessage.value = "已断开连接"
        _currentChannel.value = null
        addSystemMessage("已断开连接")
    }
    
    fun joinChannel(channel: String) {
        val client = ircClient
        if (client == null || !client.isConnected()) {
            _statusMessage.value = "未连接到服务器"
            return
        }
        
        viewModelScope.launch {
            try {
                client.joinChannel(channel)
                val channelName = if (channel.startsWith("#")) channel else "#$channel"
                _currentChannel.value = channelName
                _statusMessage.value = "正在加入 $channelName..."
                addSystemMessage("正在加入 $channelName...")
            } catch (e: Exception) {
                _statusMessage.value = "加入频道失败: ${e.message}"
                addSystemMessage("加入频道失败: ${e.message}")
            }
        }
    }
    
    fun sendMessage(message: String) {
        val channel = _currentChannel.value
        if (channel == null) {
            _statusMessage.value = "请先加入一个频道"
            return
        }
        
        val client = ircClient
        if (client == null || !client.isConnected()) {
            _statusMessage.value = "未连接到服务器"
            return
        }
        
        viewModelScope.launch {
            try {
                client.sendMessage(channel, message)
                // 获取当前昵称，如果为空则使用"Me"
                val currentNick = client.nickname.takeIf { it.isNotBlank() } ?: "Me"
                addMessage(currentNick, message, isOwn = true)
            } catch (e: Exception) {
                _statusMessage.value = "发送消息失败: ${e.message}"
                addSystemMessage("发送消息失败: ${e.message}")
            }
        }
    }
    
    /**
     * 请求频道列表
     */
    fun requestChannelList() {
        val client = ircClient
        if (client == null || !client.isConnected()) {
            _statusMessage.value = "未连接到服务器"
            return
        }
        
        viewModelScope.launch {
            try {
                channelListBuffer.clear()
                client.requestChannelList()
                addSystemMessage("正在获取频道列表...")
            } catch (e: Exception) {
                _statusMessage.value = "获取频道列表失败: ${e.message}"
                addSystemMessage("获取频道列表失败: ${e.message}")
            }
        }
    }
    
    private fun handleIrcMessage(message: IrcMessage) {
        when {
            message.isPrivMsg() -> {
                val sender = message.getSenderNick()
                val text = message.trailing
                val currentNick = ircClient?.nickname ?: ""
                
                // 检查是否提及当前用户
                if (currentNick.isNotBlank() && 
                    notificationHelper.isMentioned(text, currentNick)) {
                    // 发送通知
                    val channel = _currentChannel.value ?: "未知频道"
                    notificationHelper.showMentionNotification(sender, text, channel)
                }
                
                addMessage(sender, text)
            }
            message.command == "JOIN" -> {
                val sender = message.getSenderNick()
                val channel = message.trailing.ifBlank { 
                    if (message.params.isNotEmpty()) message.params[0] else ""
                }
                addSystemMessage("[$+] $sender 加入了 $channel")
                if (sender == ircClient?.nickname) {
                    _statusMessage.postValue("已加入 $channel")
                }
            }
            message.command == "PART" -> {
                val sender = message.getSenderNick()
                val channel = if (message.params.isNotEmpty()) message.params[0] else ""
                addSystemMessage("[-] $sender 离开了 $channel")
            }
            message.command == "QUIT" -> {
                val sender = message.getSenderNick()
                addSystemMessage("[-] $sender 退出了 (${message.trailing})")
            }
            message.command == "NICK" -> {
                val oldNick = message.getSenderNick()
                val newNick = message.trailing.ifBlank { 
                    if (message.params.isNotEmpty()) message.params[0] else ""
                }
                addSystemMessage("[*] $oldNick 改名为 $newNick")
            }
            message.isNumericReply() -> {
                handleNumericReply(message)
            }
        }
    }
    
    private fun handleNumericReply(message: IrcMessage) {
        val code = message.command.toIntOrNull() ?: return
        val text = message.trailing
        
        when (code) {
            // RPL_WELCOME - 开始收集服务器消息
            1 -> {
                isCollectingMotd = true
                serverMessageBuffer.clear()
                serverMessageBuffer.add("欢迎: $text")
                _statusMessage.postValue("已登录")
            }
            // RPL_YOURHOST, RPL_CREATED, RPL_MYINFO, RPL_ISUPPORT
            2, 3, 4, 5 -> {
                if (isCollectingMotd && text.isNotBlank()) {
                    serverMessageBuffer.add(text)
                }
            }
            // RPL_MOTDSTART - MOTD 开始
            375 -> {
                isCollectingMotd = true
                if (text.isNotBlank()) {
                    serverMessageBuffer.add(text)
                }
            }
            // RPL_MOTD - MOTD 内容
            372 -> {
                if (isCollectingMotd && text.isNotBlank()) {
                    serverMessageBuffer.add(text)
                }
            }
            // RPL_ENDOFMOTD - MOTD 结束，显示折叠消息
            376 -> {
                if (isCollectingMotd && text.isNotBlank()) {
                    serverMessageBuffer.add(text)
                }
                finishServerMessages()
            }
            // RPL_LUSERCLIENT, RPL_LUSEROP, RPL_LUSERCHANNELS, etc.
            251, 252, 253, 254, 255 -> {
                if (isCollectingMotd && text.isNotBlank()) {
                    serverMessageBuffer.add(text)
                }
            }
            322 -> {
                // RPL_LIST - 频道列表项
                if (message.params.size >= 3) {
                    val channel = message.params[1]
                    val userCount = message.params[2].toIntOrNull() ?: 0
                    val topic = text
                    channelListBuffer.add(ChannelInfo(channel, userCount, topic))
                }
            }
            323 -> {
                // RPL_LISTEND - 频道列表结束
                _channelList.postValue(channelListBuffer.toList())
                addSystemMessage("[频道] 找到 ${channelListBuffer.size} 个频道")
            }
            332 -> {
                // RPL_TOPIC - 频道话题
                if (message.params.size >= 2) {
                    val channel = message.params[1]
                    addSystemMessage("📌 $channel 话题: $text")
                }
            }
            353 -> {
                // RPL_NAMREPLY - 用户列表（不显示，太长）
                // 可以在这里收集，但不显示在聊天区
            }
            366 -> {
                // RPL_ENDOFNAMES - 用户列表结束
                if (message.params.size >= 2) {
                    val channel = message.params[1]
                    _statusMessage.postValue("在 $channel 中")
                    addSystemMessage("✅ 已加入 $channel")
                }
            }
            else -> {
                // 其他数字响应码，如果在收集 MOTD 期间则添加
                if (isCollectingMotd && text.isNotBlank() && code < 400) {
                    serverMessageBuffer.add("[$code] $text")
                }
            }
        }
    }
    
    /**
     * 完成服务器消息收集，显示为折叠消息
     */
    private fun finishServerMessages() {
        isCollectingMotd = false
        
        if (serverMessageBuffer.isEmpty()) {
            return
        }
        
        val currentList = _messages.value ?: mutableListOf()
        val messageId = System.currentTimeMillis()
        serverMessageId = messageId
        
        currentList.add(
            ChatMessage(
                id = messageId,
                sender = "服务器",
                message = "📋 服务器欢迎消息 (点击展开 ${serverMessageBuffer.size} 条)",
                timestamp = dateFormat.format(Date()),
                isSystemMessage = true,
                isServerMessage = true,
                isExpandable = true,
                isExpanded = false,
                detailMessages = serverMessageBuffer.toList()
            )
        )
        _messages.postValue(currentList)
    }
    
    /**
     * 切换服务器消息的展开/折叠状态
     */
    fun toggleServerMessage(messageId: Long) {
        val currentList = _messages.value ?: return
        val index = currentList.indexOfFirst { it.id == messageId }
        if (index == -1) return
        
        val message = currentList[index]
        if (!message.isExpandable) return
        
        val updatedMessage = if (message.isExpanded) {
            // 折叠
            message.copy(
                message = "📋 服务器欢迎消息 (点击展开 ${message.detailMessages?.size ?: 0} 条)",
                isExpanded = false
            )
        } else {
            // 展开
            val details = message.detailMessages?.joinToString("\n") ?: ""
            message.copy(
                message = "📋 服务器欢迎消息 (点击折叠):\n\n$details",
                isExpanded = true
            )
        }
        
        currentList[index] = updatedMessage
        _messages.postValue(currentList)
    }
    
    private fun addMessage(sender: String, message: String, isSystem: Boolean = false, isOwn: Boolean = false) {
        val currentList = _messages.value ?: mutableListOf()
        currentList.add(
            ChatMessage(
                sender = sender,
                message = message,
                timestamp = dateFormat.format(Date()),
                isSystemMessage = isSystem,
                isOwnMessage = isOwn,
                imageUrl = extractImageUrl(message)  // 提取图片 URL
            )
        )
        _messages.postValue(currentList)
    }
    
    private fun addSystemMessage(message: String) {
        addMessage("系统", message, isSystem = true)
    }
    
    /**
     * 从 Markdown 格式的消息中提取图片 URL
     * 格式：![description](url)
     */
    private fun extractImageUrl(message: String): String? {
        return try {
            val pattern = "!\\[.*?\\]\\((https?://[^)]+)\\)".toRegex()
            val matchResult = pattern.find(message)
            matchResult?.groupValues?.get(1)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 清除所有通知
     */
    fun clearNotifications() {
        notificationHelper.cancelAllNotifications()
    }
    
    /**
     * 处理相机拍照后的图片
     *
     * @param imageUri 拍照后的图片 URI
     * @param uploadUrl 图床上传 URL（如 https://api.imgbb.com/1/upload）
     * @param apiKey 图床 API Key（如有需要）
     */
    fun handleCameraImage(
        imageUri: Uri,
        uploadUrl: String,
        apiKey: String? = null
    ) {
        val channel = _currentChannel.value
        if (channel == null) {
            _statusMessage.value = "请先加入一个频道"
            return
        }
        
        _uploadingImage.value = true
        
        viewModelScope.launch {
            try {
                // 第一步：保存图片到缓存
                val imageFile = imageUploadManager.saveImageFromUri(imageUri)
                if (imageFile == null) {
                    _statusMessage.postValue("保存图片失败")
                    _uploadingImage.postValue(false)
                    return@launch
                }
                
                // 第二步：上传到图床
                val imageUrl = imageUploadManager.uploadImageToHost(imageFile, uploadUrl, apiKey)
                
                if (imageUrl != null && imageUrl.isNotBlank()) {
                    _uploadedImageUrl.postValue(imageUrl)
                    _statusMessage.postValue("图片上传成功: $imageUrl")
                    addSystemMessage("📷 图片上传成功: $imageUrl")
                    // 自动在文本框中插入图片链接
                } else {
                    _statusMessage.postValue("上传图片到图床失败")
                    _uploadedImageUrl.postValue(null)
                    addSystemMessage("❌ 上传图片失败")
                }
            } catch (e: Exception) {
                _statusMessage.postValue("处理图片失败: ${e.message}")
                _uploadedImageUrl.postValue(null)
                addSystemMessage("❌ 错误: ${e.message}")
            } finally {
                _uploadingImage.postValue(false)
            }
        }
    }
    
    /**
     * 直接发送包含图片链接的消息
     *
     * @param message 文本内容
     * @param imageUrl 图片 URL（可选）
     */
    fun sendMessageWithImage(message: String, imageUrl: String? = null) {
        val channel = _currentChannel.value
        if (channel == null) {
            _statusMessage.value = "请先加入一个频道"
            return
        }
        
        val client = ircClient
        if (client == null || !client.isConnected()) {
            _statusMessage.value = "未连接到服务器"
            return
        }
        
        // 使用 Markdown 格式添加图片：![image](url)
        // 图片链接始终放在消息的最前面，便于 bot 解析
        val finalMessage = if (imageUrl != null && imageUrl.isNotBlank()) {
            val imageMarkdown = "![图片]($imageUrl)"
            if (message.isNotBlank()) {
                "$imageMarkdown $message"
            } else {
                imageMarkdown
            }
        } else {
            message
        }
        
        viewModelScope.launch {
            try {
                client.sendMessage(channel, finalMessage)
                val currentNick = client.nickname.takeIf { it.isNotBlank() } ?: "Me"
                addMessage(currentNick, finalMessage, isOwn = true)
                _uploadedImageUrl.postValue(null)
            } catch (e: Exception) {
                _statusMessage.postValue("发送消息失败: ${e.message}")
                addSystemMessage("发送消息失败: ${e.message}")
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        ircClient?.disconnect()
        notificationHelper.cancelAllNotifications()
        imageUploadManager.cleanCacheImages()
    }
}
