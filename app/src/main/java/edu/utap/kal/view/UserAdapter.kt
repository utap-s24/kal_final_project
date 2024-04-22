package edu.utap.kal.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.utap.kal.MainViewModel
import edu.utap.kal.R
import edu.utap.kal.User
import edu.utap.kal.UserCard
import edu.utap.kal.databinding.UserListRowBinding

class UserAdapter(private val viewModel: MainViewModel,
                  private val navigateToOneUser: (UserCard)->Unit) :
    ListAdapter<UserCard, UserAdapter.VH>(DiffUser()) {
    // This class allows the adapter to compute what has changed
    class DiffUser : DiffUtil.ItemCallback<UserCard>() {
        override fun areItemsTheSame(oldItem: UserCard, newItem: UserCard): Boolean {
            return oldItem.username == newItem.username
        }
        override fun areContentsTheSame(oldItem: UserCard, newItem: UserCard): Boolean {
            return oldItem.bio == newItem.bio
        }
    }

    inner class VH(private val userListRowBinding: UserListRowBinding) :
        RecyclerView.ViewHolder(userListRowBinding.root) {

        fun bind(user : UserCard) {
            userListRowBinding.title.text = user.username
            userListRowBinding.bio.text = user.bio

            // set onClickListeners
            userListRowBinding.title.setOnClickListener {
                navigateToOneUser(user)
            }
            userListRowBinding.bio.setOnClickListener {
                navigateToOneUser(user)
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val userListRowBinding = UserListRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(userListRowBinding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}