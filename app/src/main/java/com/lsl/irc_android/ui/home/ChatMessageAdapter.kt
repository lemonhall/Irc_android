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
class ChatMessageAdapter : ListAdapter<ChatMessage, ChatMessageAdapter.MessageViewHolder>(
    MessageDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
    val isSystemMessage: Boolean = false
)