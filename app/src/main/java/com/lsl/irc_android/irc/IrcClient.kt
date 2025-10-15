package com.lsl.irc_android.irc

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * IRC 客户端
 */
class IrcClient(
    private val onMessageReceived: (IrcMessage) -> Unit,
    private val onConnectionStateChanged: (Boolean) -> Unit,
    private val onError: (String) -> Unit
) {
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var receiveJob: Job? = null
    
    var server: String = ""
        private set
    var port: Int = 6667
        private set
    var nickname: String = ""
        private set
        
    @Volatile
    private var isConnected = false
    
    private val mutex = kotlinx.coroutines.sync.Mutex()
    
    /**
     * 连接到 IRC 服务器
     */
    suspend fun connect(server: String, port: Int, nickname: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (isConnected) {
                    disconnect()
                }
                
                this@IrcClient.server = server
                this@IrcClient.port = port
                this@IrcClient.nickname = nickname
                
                // 创建 Socket 连接，设置超时
                socket = Socket().apply {
                    soTimeout = 30000 // 30秒读取超时
                    connect(java.net.InetSocketAddress(server, port), 10000) // 10秒连接超时
                }
                
                writer = BufferedWriter(
                    OutputStreamWriter(socket!!.getOutputStream(), StandardCharsets.UTF_8)
                )
                reader = BufferedReader(
                    InputStreamReader(socket!!.getInputStream(), StandardCharsets.UTF_8)
                )
                
                isConnected = true
                withContext(Dispatchers.Main) {
                    onConnectionStateChanged(true)
                }
                
                // 发送登录信息
                sendRaw(IrcMessage.createNick(nickname))
                sendRaw(IrcMessage.createUser(nickname, nickname))
                
                // 启动接收线程
                startReceiving()
                
            } catch (e: Exception) {
                isConnected = false
                cleanupConnection()
                withContext(Dispatchers.Main) {
                    onConnectionStateChanged(false)
                    onError("连接失败: ${e.message}")
                }
                throw e
            }
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        isConnected = false
        
        try {
            if (writer != null) {
                runBlocking {
                    try {
                        sendRaw(IrcMessage.createQuit())
                    } catch (e: Exception) {
                        // 忽略发送QUIT时的错误
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略错误
        }
        
        cleanupConnection()
        onConnectionStateChanged(false)
    }
    
    /**
     * 清理连接资源
     */
    private fun cleanupConnection() {
        receiveJob?.cancel()
        
        try {
            writer?.close()
        } catch (e: Exception) { }
        
        try {
            reader?.close()
        } catch (e: Exception) { }
        
        try {
            socket?.close()
        } catch (e: Exception) { }
        
        writer = null
        reader = null
        socket = null
    }
    
    /**
     * 加入频道
     */
    suspend fun joinChannel(channel: String) {
        val channelName = if (channel.startsWith("#")) channel else "#$channel"
        sendRaw(IrcMessage.createJoin(channelName))
    }
    
    /**
     * 请求频道列表
     */
    suspend fun requestChannelList() {
        sendRaw("LIST\r\n")
    }
    
    /**
     * 发送消息到频道或用户
     */
    suspend fun sendMessage(target: String, message: String) {
        sendRaw(IrcMessage.createPrivMsg(target, message))
    }
    
    /**
     * 发送原始 IRC 命令
     */
    private suspend fun sendRaw(message: String) = withContext(Dispatchers.IO) {
        try {
            val currentWriter = writer
            if (!isConnected || currentWriter == null) {
                throw IllegalStateException("未连接到服务器")
            }
            
            currentWriter.write(message)
            currentWriter.flush()
            
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("发送消息失败: ${e.message}")
            }
            disconnect()
            throw e
        }
    }
    
    /**
     * 启动消息接收
     */
    private fun startReceiving() {
        receiveJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentReader = reader
                if (currentReader == null) {
                    onError("读取器未初始化")
                    return@launch
                }
                
                while (isConnected && !currentCoroutineContext().job.isCancelled) {
                    try {
                        val line = currentReader.readLine()
                        if (line == null) {
                            // 连接已关闭
                            break
                        }
                        
                        val message = IrcMessage.parse(line)
                        if (message != null) {
                            // 处理 PING
                            if (message.isPing()) {
                                val server = if (message.trailing.isNotBlank()) {
                                    message.trailing
                                } else if (message.params.isNotEmpty()) {
                                    message.params[0]
                                } else {
                                    ""
                                }
                                try {
                                    sendRaw(IrcMessage.createPong(server))
                                } catch (e: Exception) {
                                    // PONG发送失败，可能连接已断开
                                    break
                                }
                            }
                            
                            // 通知监听者
                            withContext(Dispatchers.Main) {
                                onMessageReceived(message)
                            }
                        }
                    } catch (e: java.net.SocketTimeoutException) {
                        // 读取超时，继续尝试
                        continue
                    } catch (e: Exception) {
                        if (isConnected) {
                            withContext(Dispatchers.Main) {
                                onError("接收消息错误: ${e.message}")
                            }
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                if (isConnected) {
                    withContext(Dispatchers.Main) {
                        onError("接收线程异常: ${e.message}")
                    }
                }
            } finally {
                if (isConnected) {
                    disconnect()
                }
            }
        }
    }
    
    /**
     * 检查是否已连接
     */
    fun isConnected(): Boolean = isConnected
}