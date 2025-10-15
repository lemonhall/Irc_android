package com.lsl.irc_android.ui.home

/**
 * IRC 连接状态
 */
enum class ConnectionState {
    DISCONNECTED,  // 未连接
    CONNECTING,    // 连接中
    CONNECTED,     // 已连接
    ERROR          // 错误
}
