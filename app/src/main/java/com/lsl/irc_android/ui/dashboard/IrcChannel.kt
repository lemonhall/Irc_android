package com.lsl.irc_android.ui.dashboard

/**
 * IRC频道数据类
 */
data class IrcChannel(
    val name: String,              // 频道名称
    val userCount: Int = 0,        // 用户数量
    val topic: String = ""         // 频道话题
)
