package com.lsl.irc_android.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
    private lateinit var btnCamera: Button
    private lateinit var textCurrentChannel: TextView
    private lateinit var recyclerMessages: RecyclerView
    
    // 通知权限请求
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "通知权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "通知权限被拒绝，将无法收到提及通知", Toast.LENGTH_LONG).show()
        }
    }
    
    // 相机权限请求
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(context, "相机权限被拒绝，无法拍照", Toast.LENGTH_LONG).show()
        }
    }
    
    // 读写存储权限请求
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true ||
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        ) {
            Toast.makeText(context, "存储权限已授予", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 拍照返回结果处理
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri ->
                // 这里应该调用 ViewModel 上传图片
                // 需要在 Fragment 中配置图床 URL 和 API Key
                showImageUploadDialog(uri)
            }
        } else {
            Toast.makeText(context, "拍照失败或已取消", Toast.LENGTH_SHORT).show()
        }
    }
    
    private var photoUri: android.net.Uri? = null

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
        requestNotificationPermission()
        
        return root
    }
    
    /**
     * 请求通知权限 (Android 13+)
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已有权限
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // 显示说明
                    Toast.makeText(
                        context,
                        "需要通知权限来提醒你被提及的消息",
                        Toast.LENGTH_LONG
                    ).show()
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // 直接请求权限
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    /**
     * 初始化视图
     */
    private fun initViews(root: View) {
        editMessage = root.findViewById(R.id.edit_message)
        btnSend = root.findViewById(R.id.btn_send)
        btnCamera = root.findViewById(R.id.btn_camera)
        textCurrentChannel = root.findViewById(R.id.text_current_channel)
        recyclerMessages = root.findViewById(R.id.recycler_messages)
    }
    
    /**
     * 设置 RecyclerView
     */
    private fun setupRecyclerView() {
        messageAdapter = ChatMessageAdapter { messageId ->
            // 点击服务器消息时切换展开/折叠状态
            homeViewModel.toggleServerMessage(messageId)
        }
        recyclerMessages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = messageAdapter
        }
    }
    
    /**
     * 设置按钮事件
     */
    private fun setupButtons() {
        // 拍照按钮
        btnCamera.setOnClickListener {
            requestCameraPermissionAndTakePicture()
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
                    btnSend.isEnabled = false
                    btnCamera.isEnabled = false
                    textCurrentChannel.text = "当前频道: 未连接"
                }
                ConnectionState.CONNECTING -> {
                    btnSend.isEnabled = false
                    btnCamera.isEnabled = false
                    textCurrentChannel.text = "当前频道: 连接中..."
                }
                ConnectionState.CONNECTED -> {
                    btnSend.isEnabled = true
                    btnCamera.isEnabled = true
                }
                ConnectionState.ERROR -> {
                    btnSend.isEnabled = false
                    btnCamera.isEnabled = false
                    textCurrentChannel.text = "当前频道: 连接错误"
                }
                else -> {}
            }
        }
        
        // 消息列表
        homeViewModel.messages.observe(viewLifecycleOwner) { messages ->
            messageAdapter.submitList(messages.toList()) {
                // 在列表更新完成后滚动到底部
                if (messages.isNotEmpty()) {
                    recyclerMessages.smoothScrollToPosition(messages.size - 1)
                }
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
                btnCamera.isEnabled = homeViewModel.connectionState.value == ConnectionState.CONNECTED
            } else {
                textCurrentChannel.text = "📢 $status"
                btnSend.isEnabled = false
                btnCamera.isEnabled = false
            }
        }
        
        // 图片上传状态
        homeViewModel.uploadingImage.observe(viewLifecycleOwner) { isUploading ->
            btnSend.isEnabled = !isUploading && homeViewModel.connectionState.value == ConnectionState.CONNECTED
            btnCamera.isEnabled = !isUploading && homeViewModel.connectionState.value == ConnectionState.CONNECTED
        }
        
        // 上传后的图片 URL
        homeViewModel.uploadedImageUrl.observe(viewLifecycleOwner) { imageUrl ->
            if (imageUrl != null && imageUrl.isNotBlank()) {
                // 使用 Markdown 格式，链接始终添加到最前面
                val imageMarkdown = "![图片]($imageUrl) "
                val currentText = editMessage.text.toString()
                val newText = imageMarkdown + currentText
                editMessage.setText(newText)
                editMessage.setSelection(imageMarkdown.length)
            }
        }
    }
    
    /**
     * 请求相机权限并拍照
     */
    private fun requestCameraPermissionAndTakePicture() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(context, "需要相机权限才能拍照", Toast.LENGTH_LONG).show()
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    /**
     * 启动相机拍照
     */
    private fun launchCamera() {
        try {
            val photoFile = createImageFile()
            photoUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(context, "无法启动相机: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 创建图片文件
     */
    private fun createImageFile(): java.io.File {
        val storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return java.io.File.createTempFile("IMG_", ".jpg", storageDir)
    }
    
    /**
     * 显示图片上传对话框
     */
    private fun showImageUploadDialog(imageUri: android.net.Uri) {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("上传图片")
            .setMessage("是否要上传此图片到图床？\n\n请确保已在设置中配置图床地址和 API Key。")
            .setPositiveButton("上传") { _, _ ->
                // 从 IrcConfigManager 获取图床配置
                val configManager = com.lsl.irc_android.data.IrcConfigManager(requireContext())
                val imageHost = configManager.imageHost
                val apiKey = configManager.apiKey
                
                if (imageHost.isBlank()) {
                    Toast.makeText(
                        context,
                        "请先在设置中配置图床地址",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    homeViewModel.handleCameraImage(imageUri, imageHost, apiKey)
                }
            }
            .setNegativeButton("取消", null)
            .create()
        dialog.show()
    }
    
    override fun onResume() {
        super.onResume()
        // 当用户查看聊天页面时，清除所有通知
        homeViewModel.clearNotifications()
    }
}