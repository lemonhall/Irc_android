package com.lsl.irc_android.irc

/**
 * IRC 消息数据类
 */
data class IrcMessage(
    val prefix: String = "",      // 消息前缀（通常是发送者信息）
    val command: String,           // IRC 命令
    val params: List<String> = emptyList(),  // 参数列表
    val trailing: String = ""      // 结尾消息
) {
    companion object {
        /**
         * 解析 IRC 消息
         * 格式: [:prefix] <command> [params] [:trailing]
         */
        fun parse(raw: String): IrcMessage? {
            if (raw.isBlank()) return null
            
            var message = raw.trim()
            var prefix = ""
            var trailing = ""
            
            // 提取前缀
            if (message.startsWith(":")) {
                val spaceIndex = message.indexOf(' ')
                if (spaceIndex > 0) {
                    prefix = message.substring(1, spaceIndex)
                    message = message.substring(spaceIndex + 1).trim()
                }
            }
            
            // 提取尾部消息
            val colonIndex = message.indexOf(" :")
            if (colonIndex >= 0) {
                trailing = message.substring(colonIndex + 2)
                message = message.substring(0, colonIndex)
            }
            
            // 分割命令和参数
            val parts = message.split(" ").filter { it.isNotBlank() }
            if (parts.isEmpty()) return null
            
            val command = parts[0]
            val params = parts.drop(1)
            
            return IrcMessage(prefix, command, params, trailing)
        }
        
        /**
         * 创建 PRIVMSG 消息
         */
        fun createPrivMsg(target: String, message: String): String {
            return "PRIVMSG $target :$message\r\n"
        }
        
        /**
         * 创建 JOIN 消息
         */
        fun createJoin(channel: String): String {
            return "JOIN $channel\r\n"
        }
        
        /**
         * 创建 NICK 消息
         */
        fun createNick(nickname: String): String {
            return "NICK $nickname\r\n"
        }
        
        /**
         * 创建 USER 消息
         */
        fun createUser(username: String, realname: String): String {
            return "USER $username 0 * :$realname\r\n"
        }
        
        /**
         * 创建 PONG 消息
         */
        fun createPong(server: String): String {
            return "PONG :$server\r\n"
        }
        
        /**
         * 创建 QUIT 消息
         */
        fun createQuit(message: String = "Leaving"): String {
            return "QUIT :$message\r\n"
        }
    }
    
    /**
     * 获取发送者昵称
     */
    fun getSenderNick(): String {
        if (prefix.isBlank()) return ""
        val exclamationIndex = prefix.indexOf('!')
        return if (exclamationIndex > 0) {
            prefix.substring(0, exclamationIndex)
        } else {
            prefix
        }
    }
    
    /**
     * 判断是否是 PRIVMSG
     */
    fun isPrivMsg(): Boolean = command.equals("PRIVMSG", ignoreCase = true)
    
    /**
     * 判断是否是 PING
     */
    fun isPing(): Boolean = command.equals("PING", ignoreCase = true)
    
    /**
     * 判断是否是数字回复
     */
    fun isNumericReply(): Boolean = command.all { it.isDigit() }
    
    override fun toString(): String {
        val sb = StringBuilder()
        if (prefix.isNotBlank()) {
            sb.append(":").append(prefix).append(" ")
        }
        sb.append(command)
        if (params.isNotEmpty()) {
            sb.append(" ").append(params.joinToString(" "))
        }
        if (trailing.isNotBlank()) {
            sb.append(" :").append(trailing)
        }
        return sb.toString()
    }
}