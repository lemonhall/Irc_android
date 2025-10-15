package com.lsl.irc_android.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.lsl.irc_android.R

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var messageAdapter: ChatMessageAdapter
    
    // UI 组件
    private lateinit var editServer: TextInputEditText
    private lateinit var editPort: TextInputEditText
    private lateinit var editNickname: TextInputEditText
    private lateinit var editChannel: TextInputEditText
    private lateinit var editMessage: TextInputEditText
    private lateinit var btnConnect: Button
    private lateinit var btnJoinChannel: Button
    private lateinit var btnSend: Button
    private lateinit var textStatus: TextView
    private lateinit var recyclerMessages: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        
        initViews(root)
        setupRecyclerView()
        setupButtons()
        setupObservers()
        
        return root
    }
    
    /**
     * 初始化视图
     */
    private fun initViews(root: View) {
        editServer = root.findViewById(R.id.edit_server)
        editPort = root.findViewById(R.id.edit_port)
        editNickname = root.findViewById(R.id.edit_nickname)
        editChannel = root.findViewById(R.id.edit_channel)
        editMessage = root.findViewById(R.id.edit_message)
        btnConnect = root.findViewById(R.id.btn_connect)
        btnJoinChannel = root.findViewById(R.id.btn_join_channel)
        btnSend = root.findViewById(R.id.btn_send)
        textStatus = root.findViewById(R.id.text_status)
        recyclerMessages = root.findViewById(R.id.recycler_messages)
    }
    
    /**
     * 设置 RecyclerView
     */
    private fun setupRecyclerView() {
        messageAdapter = ChatMessageAdapter()
        recyclerMessages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = messageAdapter
        }
    }
    
    /**
     * 设置按钮事件
     */
    private fun setupButtons() {
        // 连接按钮
        btnConnect.setOnClickListener {
            val server = editServer.text.toString()
            val port = editPort.text.toString().toIntOrNull() ?: 6667
            val nickname = editNickname.text.toString()
            
            if (server.isBlank() || nickname.isBlank()) {
                textStatus.text = "请填写服务器和昵称"
                return@setOnClickListener
            }
            
            when (homeViewModel.connectionState.value) {
                ConnectionState.DISCONNECTED, ConnectionState.ERROR -> {
                    homeViewModel.connect(server, port, nickname)
                }
                ConnectionState.CONNECTED -> {
                    homeViewModel.disconnect()
                }
                else -> {}
            }
        }
        
        // 加入频道按钮
        btnJoinChannel.setOnClickListener {
            val channel = editChannel.text.toString()
            if (channel.isNotBlank()) {
                homeViewModel.joinChannel(channel)
            }
        }
        
        // 发送按钮
        btnSend.setOnClickListener {
            sendMessage()
        }
        
        // 回车键发送
        editMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }
    }
    
    /**
     * 发送消息
     */
    private fun sendMessage() {
        val message = editMessage.text.toString()
        if (message.isNotBlank()) {
            homeViewModel.sendMessage(message)
            editMessage.text?.clear()
        }
    }
    
    /**
     * 设置观察者
     */
    private fun setupObservers() {
        // 连接状态
        homeViewModel.connectionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                ConnectionState.DISCONNECTED -> {
                    btnConnect.text = "连接"
                    btnConnect.isEnabled = true
                    btnJoinChannel.isEnabled = false
                    btnSend.isEnabled = false
                    
                    // 启用连接设置
                    editServer.isEnabled = true
                    editPort.isEnabled = true
                    editNickname.isEnabled = true
                }
                ConnectionState.CONNECTING -> {
                    btnConnect.text = "连接中..."
                    btnConnect.isEnabled = false
                    
                    // 禁用连接设置
                    editServer.isEnabled = false
                    editPort.isEnabled = false
                    editNickname.isEnabled = false
                }
                ConnectionState.CONNECTED -> {
                    btnConnect.text = "断开"
                    btnConnect.isEnabled = true
                    btnJoinChannel.isEnabled = true
                    btnSend.isEnabled = true
                }
                ConnectionState.ERROR -> {
                    btnConnect.text = "连接"
                    btnConnect.isEnabled = true
                    btnJoinChannel.isEnabled = false
                    btnSend.isEnabled = false
                    
                    // 启用连接设置
                    editServer.isEnabled = true
                    editPort.isEnabled = true
                    editNickname.isEnabled = true
                }
                else -> {}
            }
        }
        
        // 消息列表
        homeViewModel.messages.observe(viewLifecycleOwner) { messages ->
            messageAdapter.submitList(messages.toList())
            // 滚动到最新消息
            if (messages.isNotEmpty()) {
                recyclerMessages.scrollToPosition(messages.size - 1)
            }
        }
        
        // 状态消息
        homeViewModel.statusMessage.observe(viewLifecycleOwner) { status ->
            textStatus.text = status
        }
        
        // 当前频道
        homeViewModel.currentChannel.observe(viewLifecycleOwner) { channel ->
            if (channel != null) {
                editChannel.setText(channel)
            }
        }
    }
}