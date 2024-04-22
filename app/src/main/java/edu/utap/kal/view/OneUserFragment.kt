package edu.utap.kal.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import edu.utap.kal.MainViewModel
import edu.utap.kal.UserCard
import edu.utap.kal.databinding.FragmentOneUserBinding

class OneUserFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentOneUserBinding? = null
    private val binding get() = _binding!!
    private val args: OneUserFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOneUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // getting the user object
        val user = args.userCard

        // Assign attributes of the post
        binding.userName.text = user.username
        binding.userBio.text = user.bio
        val followingList = viewModel.observeFollowingListOnly().value ?: emptyList<UserCard>()
        Log.d("XXX", "Following list: ${followingList}")
        if (user in followingList) {
            binding.followBut.text = "UNFOLLOW"
        } else {
            binding.followBut.text = "FOLLOW"
        }

        val adapter = NotesAdapter(viewModel)
        val rv = binding.noteListRV
        val itemDecor = DividerItemDecoration(rv.context, LinearLayoutManager.VERTICAL)
        binding.noteListRV.addItemDecoration(itemDecor)
        binding.noteListRV.adapter = adapter

        // observe live data
        viewModel.fetchOtherUserNotes(user.UID)
        viewModel.observeOtherUserNotes().observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        binding.followBut.setOnClickListener {
            val followingList = viewModel.observeFollowingListOnly().value ?: emptyList<UserCard>()
            if (followingList.contains(user)) { // no longer wants to follow
                Log.d("XXX", "Clicked on unfollow")
                binding.followBut.text = "FOLLOW"
                viewModel.removeFollowing(user)
            } else { // wants to follow
                Log.d("XXX", "Clicked on follow")
                binding.followBut.text = "UNFOLLOW"
                viewModel.addFollowing(user.UID)
            }
        }

        // Chat feature
        binding.chatBut.setOnClickListener {
            val action = OneUserFragmentDirections.actionOneUserToChatFragment(user)
            findNavController().navigate(action)
        }
    }
}