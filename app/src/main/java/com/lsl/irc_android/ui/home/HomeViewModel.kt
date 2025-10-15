package com.lsl.irc_android.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lsl.irc_android.irc.IrcClient
import com.lsl.irc_android.irc.IrcMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel : ViewModel() {

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
    
    private var ircClient: IrcClient? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
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
                addMessage(currentNick, message)
            } catch (e: Exception) {
                _statusMessage.value = "发送消息失败: ${e.message}"
                addSystemMessage("发送消息失败: ${e.message}")
            }
        }
    }
    
    private fun handleIrcMessage(message: IrcMessage) {
        when {
            message.isPrivMsg() -> {
                val sender = message.getSenderNick()
                val text = message.trailing
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
            1 -> {
                addSystemMessage("[+] 欢迎: $text")
                _statusMessage.postValue("已登录")
            }
            332 -> {
                if (message.params.size >= 2) {
                    val channel = message.params[1]
                    addSystemMessage("[话题] $channel 话题: $text")
                }
            }
            353 -> {
                if (message.params.size >= 3) {
                    val channel = message.params[2]
                    addSystemMessage("[用户] $channel 用户: $text")
                }
            }
            366 -> {
                if (message.params.size >= 2) {
                    val channel = message.params[1]
                    _statusMessage.postValue("在 $channel 中")
                }
            }
            else -> {
                if (text.isNotBlank()) {
                    addSystemMessage("[$code] $text")
                }
            }
        }
    }
    
    private fun addMessage(sender: String, message: String, isSystem: Boolean = false) {
        val currentList = _messages.value ?: mutableListOf()
        currentList.add(
            ChatMessage(
                sender = sender,
                message = message,
                timestamp = dateFormat.format(Date()),
                isSystemMessage = isSystem
            )
        )
        _messages.postValue(currentList)
    }
    
    private fun addSystemMessage(message: String) {
        addMessage("系统", message, true)
    }
    
    override fun onCleared() {
        super.onCleared()
        ircClient?.disconnect()
    }
}
