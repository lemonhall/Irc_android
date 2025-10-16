package com.lsl.irc_android.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.lsl.irc_android.R
import com.lsl.irc_android.ui.home.HomeViewModel
import com.lsl.irc_android.ui.home.ConnectionState

class NotificationsFragment : Fragment() {

    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var homeViewModel: HomeViewModel
    
    // UI 组件
    private lateinit var editServer: TextInputEditText
    private lateinit var editPort: TextInputEditText
    private lateinit var editNickname: TextInputEditText
    private lateinit var editChannel: TextInputEditText
    private lateinit var editImageHost: TextInputEditText
    private lateinit var editApiKey: TextInputEditText
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var btnConnect: Button
    private lateinit var btnJoinChannel: Button
    private lateinit var textStatus: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_notifications, container, false)
        
        notificationsViewModel = ViewModelProvider(this)[NotificationsViewModel::class.java]
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        
        initViews(root)
        setupObservers()
        setupButtons()
        loadCurrentConfig()
        
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
        editImageHost = root.findViewById(R.id.edit_image_host)
        editApiKey = root.findViewById(R.id.edit_api_key)
        btnSave = root.findViewById(R.id.btn_save)
        btnReset = root.findViewById(R.id.btn_reset)
        btnConnect = root.findViewById(R.id.btn_connect)
        btnJoinChannel = root.findViewById(R.id.btn_join_channel)
        textStatus = root.findViewById(R.id.text_status)
    }
    
    /**
     * 设置按钮事件
     */
    private fun setupButtons() {
        // 保存设置
        btnSave.setOnClickListener {
            val server = editServer.text.toString()
            val port = editPort.text.toString()
            val nickname = editNickname.text.toString()
            val channel = editChannel.text.toString()
            val imageHost = editImageHost.text.toString()
            val apiKey = editApiKey.text.toString()
            
            if (server.isBlank()) {
                Toast.makeText(context, "请输入服务器地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (nickname.isBlank()) {
                Toast.makeText(context, "请输入昵称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            notificationsViewModel.saveConfig(server, port, nickname, channel)
            
            // 保存图床配置
            val configManager = com.lsl.irc_android.data.IrcConfigManager(requireContext())
            if (imageHost.isNotBlank()) {
                configManager.saveImageHost(imageHost)
            }
            if (apiKey.isNotBlank()) {
                configManager.saveApiKey(apiKey)
            }
            
            Toast.makeText(context, "设置已保存", Toast.LENGTH_SHORT).show()
        }
        
        // 重置设置
        btnReset.setOnClickListener {
            notificationsViewModel.resetToDefault()
        }
        
        // 连接按钮
        btnConnect.setOnClickListener {
            when (homeViewModel.connectionState.value) {
                ConnectionState.DISCONNECTED, ConnectionState.ERROR -> {
                    val config = notificationsViewModel.getCurrentConfig()
                    homeViewModel.connect(config.server, config.port, config.nickname)
                }
                ConnectionState.CONNECTED -> {
                    homeViewModel.disconnect()
                }
                else -> {}
            }
        }
        
        // 加入频道按钮
        btnJoinChannel.setOnClickListener {
            val config = notificationsViewModel.getCurrentConfig()
            if (config.channel.isNotBlank()) {
                homeViewModel.joinChannel(config.channel)
            }
        }
    }
    
    /**
     * 设置观察者
     */
    private fun setupObservers() {
        // 配置变化
        notificationsViewModel.config.observe(viewLifecycleOwner) { config ->
            editServer.setText(config.server)
            editPort.setText(config.port.toString())
            editNickname.setText(config.nickname)
            editChannel.setText(config.channel)
        }
        
        // 保存结果
        notificationsViewModel.saveResult.observe(viewLifecycleOwner) { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        
        // 连接状态
        homeViewModel.connectionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                ConnectionState.DISCONNECTED -> {
                    btnConnect.text = "连接到IRC服务器"
                    btnConnect.isEnabled = true
                    btnJoinChannel.isEnabled = false
                    textStatus.text = "未连接"
                }
                ConnectionState.CONNECTING -> {
                    btnConnect.text = "连接中..."
                    btnConnect.isEnabled = false
                    btnJoinChannel.isEnabled = false
                    textStatus.text = "连接中..."
                }
                ConnectionState.CONNECTED -> {
                    btnConnect.text = "断开连接"
                    btnConnect.isEnabled = true
                    btnJoinChannel.isEnabled = true
                    textStatus.text = "已连接"
                }
                ConnectionState.ERROR -> {
                    btnConnect.text = "连接到IRC服务器"
                    btnConnect.isEnabled = true
                    btnJoinChannel.isEnabled = false
                    textStatus.text = "连接错误"
                }
                else -> {}
            }
        }
        
        // IRC状态消息
        homeViewModel.statusMessage.observe(viewLifecycleOwner) { status ->
            textStatus.text = status
        }
    }
    
    /**
     * 加载当前配置
     */
    private fun loadCurrentConfig() {
        // 加载 IRC 配置
        notificationsViewModel.config.value?.let { config ->
            editServer.setText(config.server)
            editPort.setText(config.port.toString())
            editNickname.setText(config.nickname)
            editChannel.setText(config.channel)
        }
        
        // 加载图床配置
        val configManager = com.lsl.irc_android.data.IrcConfigManager(requireContext())
        editImageHost.setText(configManager.imageHost)
        editApiKey.setText(configManager.apiKey)
    }
}