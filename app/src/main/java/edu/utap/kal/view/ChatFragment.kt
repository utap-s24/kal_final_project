package edu.utap.kal.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import edu.utap.kal.AuthWrap
import edu.utap.kal.MainViewModel
import edu.utap.kal.Text
import edu.utap.kal.databinding.ChatroomBinding

class ChatFragment : Fragment() {
    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: ChatroomBinding? = null
    private val binding get() = _binding!!
    private val args: ChatFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ChatroomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // getting the user object
        val user = args.userCard

        binding.chattingWith.text = user.username
        val adapter = ChatAdapter(viewModel)
        val rv = binding.recyclerView
        val itemDecor = DividerItemDecoration(rv.context, LinearLayoutManager.VERTICAL)
        rv.addItemDecoration(itemDecor)
        rv.adapter = adapter

        // load and observe chats
        val currentUser = AuthWrap.getCurrentUser()
        viewModel.fetchInitialChat(currentUser.uid, user.UID)
        viewModel.observeChatHistory().observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }

        // sending a message
        binding.buttonSend.setOnClickListener {
            if (binding.editTextMessage.text.toString().isEmpty()) {
                // send toast that it cannot be empty
                Toast.makeText(requireContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                // trigger  view model function that adds the chat to the chatHistory live data
                viewModel.sendText(binding.editTextMessage.text.toString(), user.UID)
            }
        }

    }
}