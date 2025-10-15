package com.lsl.irc_android.data

/**
 * IRC 配置数据类
 */
data class IrcConfig(
    val server: String = "irc.lemonhall.me",
    val port: Int = 6667,
    val nickname: String = "lemon_an",
    val channel: String = "ai-collab-test"
)