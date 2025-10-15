package com.lsl.irc_android.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.lsl.irc_android.ui.home.HomeViewModel

class DashboardViewModel : ViewModel() {

    private val _channels = MutableLiveData<List<IrcChannel>>().apply {
        value = listOf(
            IrcChannel("#ai-collab-test", 2, "AI协作测试频道"),
            IrcChannel("#general", 0, "通用讨论"),
            IrcChannel("#random", 0, "随意聊天"),
            IrcChannel("#dev", 0, "开发讨论")
        )
    }
    val channels: LiveData<List<IrcChannel>> = _channels
    
    private val _statusMessage = MutableLiveData<String>().apply {
        value = "点击频道名称加入"
    }
    val statusMessage: LiveData<String> = _statusMessage
    
    private var homeViewModel: HomeViewModel? = null
    
    /**
     * 设置 HomeViewModel 引用
     */
    fun setHomeViewModel(viewModel: HomeViewModel) {
        homeViewModel = viewModel
        
        // 观察频道列表更新
        viewModel.channelList.observeForever { channelInfoList ->
            if (channelInfoList.isNotEmpty()) {
                val ircChannels = channelInfoList.map { info ->
                    IrcChannel(info.name, info.userCount, info.topic)
                }
                _channels.postValue(ircChannels)
                _statusMessage.postValue("找到 ${ircChannels.size} 个频道")
            }
        }
    }
    
    /**
     * 刷新频道列表（从服务器获取）
     */
    fun refreshChannels() {
        _statusMessage.value = "刷新中..."
        homeViewModel?.requestChannelList()
    }
    
    /**
     * 更新频道列表
     */
    fun updateChannels(newChannels: List<IrcChannel>) {
        _channels.value = newChannels
        _statusMessage.value = "找到 ${newChannels.size} 个频道"
    }
}