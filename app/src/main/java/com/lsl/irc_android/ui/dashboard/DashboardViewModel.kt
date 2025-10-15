package com.lsl.irc_android.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DashboardViewModel : ViewModel() {

    private val _channels = MutableLiveData<List<IrcChannel>>().apply {
        value = listOf(
            IrcChannel("#ai-collab-test", 2, "AI协作测试频道"),
            IrcChannel("#general", 0, ""),
            IrcChannel("#random", 0, ""),
            IrcChannel("#dev", 0, "开发讨论")
        )
    }
    val channels: LiveData<List<IrcChannel>> = _channels
    
    private val _statusMessage = MutableLiveData<String>().apply {
        value = "点击频道名称加入"
    }
    val statusMessage: LiveData<String> = _statusMessage
    
    /**
     * 刷新频道列表（从服务器获取）
     */
    fun refreshChannels() {
        // TODO: 实现从IRC服务器获取频道列表
        // 发送 LIST 命令到服务器
        _statusMessage.value = "刷新中..."
    }
    
    /**
     * 更新频道列表
     */
    fun updateChannels(newChannels: List<IrcChannel>) {
        _channels.value = newChannels
        _statusMessage.value = "找到 ${newChannels.size} 个频道"
    }
}