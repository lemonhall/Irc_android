package com.lsl.irc_android.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lsl.irc_android.R
import com.lsl.irc_android.ui.home.HomeViewModel

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var channelAdapter: ChannelAdapter
    
    private lateinit var textStatus: TextView
    private lateinit var btnRefresh: Button
    private lateinit var recyclerChannels: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        
        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        homeViewModel = ViewModelProvider(requireActivity())[HomeViewModel::class.java]
        
        // 连接 ViewModels
        dashboardViewModel.setHomeViewModel(homeViewModel)
        
        initViews(root)
        setupRecyclerView()
        setupObservers()
        setupButtons()
        
        return root
    }
    
    private fun initViews(root: View) {
        textStatus = root.findViewById(R.id.text_status)
        btnRefresh = root.findViewById(R.id.btn_refresh)
        recyclerChannels = root.findViewById(R.id.recycler_channels)
    }
    
    private fun setupRecyclerView() {
        channelAdapter = ChannelAdapter { channel ->
            onChannelClick(channel)
        }
        
        recyclerChannels.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = channelAdapter
        }
    }
    
    private fun setupObservers() {
        // 频道列表
        dashboardViewModel.channels.observe(viewLifecycleOwner) { channels ->
            channelAdapter.submitList(channels)
        }
        
        // 状态消息
        dashboardViewModel.statusMessage.observe(viewLifecycleOwner) { status ->
            textStatus.text = status
        }
    }
    
    private fun setupButtons() {
        btnRefresh.setOnClickListener {
            dashboardViewModel.refreshChannels()
        }
    }
    
    private fun onChannelClick(channel: IrcChannel) {
        // 加入频道
        homeViewModel.joinChannel(channel.name)
        Toast.makeText(context, "正在加入 ${channel.name}...", Toast.LENGTH_SHORT).show()
        
        // 可选：切换到聊天页面
        // activity?.findViewById<BottomNavigationView>(R.id.nav_view)?.selectedItemId = R.id.navigation_home
    }
}