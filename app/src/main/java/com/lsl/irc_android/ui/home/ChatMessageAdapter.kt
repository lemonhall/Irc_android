package com.lsl.irc_android.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lsl.irc_android.R

/**
 * 聊天消息适配器
 */
class ChatMessageAdapter(
    private val onServerMessageClick: ((Long) -> Unit)? = null
) : ListAdapter<ChatMessage, ChatMessageAdapter.MessageViewHolder>(
    MessageDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view, onServerMessageClick)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(
        itemView: View,
        private val onServerMessageClick: ((Long) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val textSender: TextView = itemView.findViewById(R.id.text_sender)
        private val textMessage: TextView = itemView.findViewById(R.id.text_message)
        private val textTime: TextView = itemView.findViewById(R.id.text_time)

        fun bind(message: ChatMessage) {
            textSender.text = message.sender
            textMessage.text = message.message
            textTime.text = message.timestamp
            
            // 系统消息用不同颜色显示
            if (message.isSystemMessage) {
                textSender.alpha = 0.6f
                textMessage.alpha = 0.6f
            } else {
                textSender.alpha = 1.0f
                textMessage.alpha = 1.0f
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
    val detailMessages: List<String>? = null  // 详细消息列表
)