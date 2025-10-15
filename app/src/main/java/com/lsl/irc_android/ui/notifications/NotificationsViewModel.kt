package com.lsl.irc_android.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.lsl.irc_android.data.IrcConfig
import com.lsl.irc_android.data.IrcConfigManager

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val configManager = IrcConfigManager(application)
    
    private val _config = MutableLiveData<IrcConfig>().apply {
        value = configManager.loadConfig()
    }
    val config: LiveData<IrcConfig> = _config
    
    private val _saveResult = MutableLiveData<String>()
    val saveResult: LiveData<String> = _saveResult
    
    /**
     * 保存配置
     */
    fun saveConfig(server: String, port: String, nickname: String, channel: String) {
        try {
            val portInt = port.toIntOrNull() ?: 6667
            val newConfig = IrcConfig(
                server = server.trim(),
                port = portInt,
                nickname = nickname.trim(),
                channel = channel.trim()
            )
            
            configManager.saveConfig(newConfig)
            _config.value = newConfig
            _saveResult.value = "设置已保存"
        } catch (e: Exception) {
            _saveResult.value = "保存失败: ${e.message}"
        }
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefault() {
        val defaultConfig = IrcConfig()
        configManager.saveConfig(defaultConfig)
        _config.value = defaultConfig
        _saveResult.value = "已重置为默认设置"
    }
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): IrcConfig {
        return _config.value ?: IrcConfig()
    }
}