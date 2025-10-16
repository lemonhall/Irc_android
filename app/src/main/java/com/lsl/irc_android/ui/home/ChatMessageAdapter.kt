package com.lsl.irc_android.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lsl.irc_android.R
import com.lsl.irc_android.data.ImageLoader
import kotlinx.coroutines.launch
import java.util.regex.Pattern

/**
 * 聊天消息适配器
 */
class ChatMessageAdapter(
    private val onServerMessageClick: ((Long) -> Unit)? = null,
    private val lifecycleCoroutineScope: LifecycleCoroutineScope? = null
) : ListAdapter<ChatMessage, ChatMessageAdapter.MessageViewHolder>(
    MessageDiffCallback()
) {

    private lateinit var imageLoader: ImageLoader

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        imageLoader = ImageLoader(parent.context)
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view, onServerMessageClick, imageLoader, lifecycleCoroutineScope)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(
        itemView: View,
        private val onServerMessageClick: ((Long) -> Unit)?,
        private val imageLoader: ImageLoader,
        private val lifecycleCoroutineScope: LifecycleCoroutineScope?
    ) : RecyclerView.ViewHolder(itemView) {
        private val textSender: TextView = itemView.findViewById(R.id.text_sender)
        private val textMessage: TextView = itemView.findViewById(R.id.text_message)
        private val textTime: TextView = itemView.findViewById(R.id.text_time)
        private val imageMessage: ImageView = itemView.findViewById(R.id.image_message)

        fun bind(message: ChatMessage) {
            textSender.text = message.sender
            textMessage.text = message.message
            textTime.text = message.timestamp

            // 提取消息中的图片 URL（Markdown 格式：![xxx](url)）
            val imageUrl = extractImageUrl(message.message)

            // 显示图片
            if (!imageUrl.isNullOrBlank()) {
                imageMessage.visibility = View.VISIBLE
                lifecycleCoroutineScope?.launch {
                    imageLoader.loadImage(imageUrl, imageMessage, maxWidth = 1080)
                }
            } else {
                imageMessage.visibility = View.GONE
                imageMessage.setImageBitmap(null)
            }

            // 系统消息用不同颜色显示
            if (message.isSystemMessage) {
                textSender.alpha = 0.6f
                textMessage.alpha = 0.6f
                textSender.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            } else if (message.isOwnMessage) {
                // 自己的消息用醒目的颜色
                textSender.alpha = 1.0f
                textMessage.alpha = 1.0f
                textSender.setTextColor(itemView.context.getColor(android.R.color.holo_blue_dark))
                textSender.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                // 其他人的消息用默认颜色
                textSender.alpha = 1.0f
                textMessage.alpha = 1.0f
                textSender.setTextColor(itemView.context.getColor(android.R.color.black))
                textSender.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            // 服务器消息可点击展开/折叠
            if (message.isExpandable) {
                itemView.setOnClickListener {
                    onServerMessageClick?.invoke(message.id)
                }
                itemView.isClickable = true
                itemView.isFocusable = true
            } else {
                itemView.setOnClickListener(null)
                itemView.isClickable = false
                itemView.isFocusable = false
            }
        }

        /**
         * 从 Markdown 格式的消息中提取图片 URL
         * 格式：![description](url)
         */
        private fun extractImageUrl(message: String): String? {
            return try {
                val pattern = Pattern.compile("!\\[.*?\\]\\((https?://[^)]+)\\)")
                val matcher = pattern.matcher(message)
                if (matcher.find()) {
                    matcher.group(1)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}

/**
 * 聊天消息数据类
 */
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val sender: String,
    val message: String,
    val timestamp: String,
    val isSystemMessage: Boolean = false,
    val isServerMessage: Boolean = false,  // 服务器 MOTD/欢迎消息
    val isExpandable: Boolean = false,     // 是否可展开
    val isExpanded: Boolean = false,       // 是否已展开
    val detailMessages: List<String>? = null,  // 详细消息列表
    val isOwnMessage: Boolean = false,     // 是否是自己发送的消息
    val imageUrl: String? = null           // 消息中的图片 URL（Markdown 格式解析出来）
)