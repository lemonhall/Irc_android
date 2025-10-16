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
        private const val KEY_IMAGE_HOST = "image_host"
        private const val KEY_API_KEY = "api_key"
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
    
    /**
     * 保存图床地址
     */
    fun saveImageHost(url: String) {
        prefs.edit().putString(KEY_IMAGE_HOST, url).apply()
    }
    
    /**
     * 获取图床地址
     */
    val imageHost: String
        get() = prefs.getString(KEY_IMAGE_HOST, "") ?: ""
    
    /**
     * 保存图床 API Key
     */
    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }
    
    /**
     * 获取图床 API Key
     */
    val apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
}