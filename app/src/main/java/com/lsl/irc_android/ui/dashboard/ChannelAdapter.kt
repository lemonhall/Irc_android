package com.lsl.irc_android.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lsl.irc_android.R

/**
 * 频道列表适配器
 */
class ChannelAdapter(
    private val onChannelClick: (IrcChannel) -> Unit
) : ListAdapter<IrcChannel, ChannelAdapter.ChannelViewHolder>(ChannelDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view, onChannelClick)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChannelViewHolder(
        itemView: View,
        private val onChannelClick: (IrcChannel) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val textChannelName: TextView = itemView.findViewById(R.id.text_channel_name)
        private val textUserCount: TextView = itemView.findViewById(R.id.text_user_count)
        private val textTopic: TextView = itemView.findViewById(R.id.text_topic)

        fun bind(channel: IrcChannel) {
            textChannelName.text = channel.name
            textUserCount.text = "${channel.userCount} 用户"
            textTopic.text = if (channel.topic.isNotBlank()) {
                channel.topic
            } else {
                "无话题"
            }
            
            itemView.setOnClickListener {
                onChannelClick(channel)
            }
        }
    }

    class ChannelDiffCallback : DiffUtil.ItemCallback<IrcChannel>() {
        override fun areItemsTheSame(oldItem: IrcChannel, newItem: IrcChannel): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: IrcChannel, newItem: IrcChannel): Boolean {
            return oldItem == newItem
        }
    }
}
