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
    private lateinit var editMessage: TextInputEditText
    private lateinit var btnSend: Button
    private lateinit var textCurrentChannel: TextView
    private lateinit var recyclerMessages: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        
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
        editMessage = root.findViewById(R.id.edit_message)
        btnSend = root.findViewById(R.id.btn_send)
        textCurrentChannel = root.findViewById(R.id.text_current_channel)
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
                    btnSend.isEnabled = false
                    textCurrentChannel.text = "当前频道: 未连接"
                }
                ConnectionState.CONNECTING -> {
                    btnSend.isEnabled = false
                    textCurrentChannel.text = "当前频道: 连接中..."
                }
                ConnectionState.CONNECTED -> {
                    btnSend.isEnabled = true
                }
                ConnectionState.ERROR -> {
                    btnSend.isEnabled = false
                    textCurrentChannel.text = "当前频道: 连接错误"
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
        
        // 状态消息 - 显示在当前频道信息中
        homeViewModel.statusMessage.observe(viewLifecycleOwner) { status ->
            val channel = homeViewModel.currentChannel.value
            if (channel != null) {
                textCurrentChannel.text = "📢 $channel | $status"
            } else {
                textCurrentChannel.text = "📢 $status"
            }
        }
        
        // 当前频道
        homeViewModel.currentChannel.observe(viewLifecycleOwner) { channel ->
            val status = homeViewModel.statusMessage.value ?: "未连接"
            if (channel != null) {
                textCurrentChannel.text = "📢 $channel | $status"
                btnSend.isEnabled = homeViewModel.connectionState.value == ConnectionState.CONNECTED
            } else {
                textCurrentChannel.text = "📢 $status"
                btnSend.isEnabled = false
            }
        }
    }
}