package edu.utap.kal.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ImageButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.utap.kal.MainViewModel
import edu.utap.kal.R
import edu.utap.kal.Text
import edu.utap.kal.databinding.TextRowBinding

class ChatAdapter(private val viewModel: MainViewModel) :
    ListAdapter<Text, ChatAdapter.VH>(DiffUser()) {
    // This class allows the adapter to compute what has changed
    class DiffUser : DiffUtil.ItemCallback<Text>() {
        override fun areItemsTheSame(oldItem: Text, newItem: Text): Boolean {
            return (oldItem.username == newItem.username && oldItem.timeStamp == newItem.timeStamp)
        }
        override fun areContentsTheSame(oldItem: Text, newItem: Text): Boolean {
            return oldItem.message == newItem.message
        }
    }

    inner class VH(private val textBinding: TextRowBinding) :
        RecyclerView.ViewHolder(textBinding.root) {

        fun bind(text : Text) {
            textBinding.senderName.text = text.username
            textBinding.textMessage.text = text.message
            textBinding.timestamp.text = text.timeStamp.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val textBinding = TextRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(textBinding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}