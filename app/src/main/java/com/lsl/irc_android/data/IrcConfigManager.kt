package com.lsl.irc_android.data

import android.content.Context
import android.content.SharedPreferences

/**
 * IRC配置管理器
 */
class IrcConfigManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("irc_config", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_SERVER = "server"
        private const val KEY_PORT = "port"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_CHANNEL = "channel"
    }
    
    /**
     * 保存IRC配置
     */
    fun saveConfig(config: IrcConfig) {
        prefs.edit().apply {
            putString(KEY_SERVER, config.server)
            putInt(KEY_PORT, config.port)
            putString(KEY_NICKNAME, config.nickname)
            putString(KEY_CHANNEL, config.channel)
            apply()
        }
    }
    
    /**
     * 加载IRC配置
     */
    fun loadConfig(): IrcConfig {
        return IrcConfig(
            server = prefs.getString(KEY_SERVER, "irc.lemonhall.me") ?: "irc.lemonhall.me",
            port = prefs.getInt(KEY_PORT, 6667),
            nickname = prefs.getString(KEY_NICKNAME, "lemon_an") ?: "lemon_an",
            channel = prefs.getString(KEY_CHANNEL, "ai-collab-test") ?: "ai-collab-test"
        )
    }
}