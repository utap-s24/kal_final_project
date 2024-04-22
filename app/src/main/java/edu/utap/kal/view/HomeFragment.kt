package edu.utap.kal.view

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.utap.kal.MainViewModel
import edu.utap.kal.R
import edu.utap.kal.databinding.FragmentHomeBinding

class HomeFragment :
    Fragment(R.layout.fragment_home) {
    private val viewModel: MainViewModel by activityViewModels()

    // https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.ViewHolder#getBindingAdapterPosition()
    // Getting the position of the selected item is unfortunately complicated
    // 3/2024, I think this is broken
    private fun getPos(holder: RecyclerView.ViewHolder) : Int {
        val pos = holder.bindingAdapterPosition
        // notifyDataSetChanged was called, so position is not known
        if( pos == RecyclerView.NO_POSITION) {
            return holder.absoluteAdapterPosition
        }
        return pos
    }
    private fun initTouchHelper(): ItemTouchHelper {
        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START)
            {
                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean {
                    return true
                }
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                                      direction: Int) {
                    val position = getPos(viewHolder)
                    Log.d(javaClass.simpleName, "Swipe delete $position")
                    viewModel.removeNoteAt(position)
                }
            }
        return ItemTouchHelper(simpleItemTouchCallback)
    }

    // No need for onCreateView because we passed R.layout to Fragment constructor
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentHomeBinding.bind(view)
        Log.d(javaClass.simpleName, "onViewCreated")
        // Create new note
        // Don't need action, because sending default argument
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_home_to_navigation_note_edit)
        }

        // Long press to edit.
        val adapter = NotesAdapter(viewModel) { position ->
            // https://developer.android.com/guide/navigation/navigation-pass-data#Safe-args
            val action =
                HomeFragmentDirections.actionNavigationHomeToNavigationNoteEdit(position, "Edit Note")
            findNavController().navigate(action)
        }

        val rv = binding.noteListRV
        val itemDecor = DividerItemDecoration(rv.context, LinearLayoutManager.VERTICAL)
        binding.noteListRV.addItemDecoration(itemDecor)
        binding.noteListRV.adapter = adapter
        // Swipe left to delete
        initTouchHelper().attachToRecyclerView(rv)

        viewModel.observeNotes().observe(viewLifecycleOwner) {
            Log.d(javaClass.simpleName, "noteList observe len ${it.size}")
            adapter.submitList(it)
        }
        viewModel.observeNotesEmpty().observe(viewLifecycleOwner) {
            if(it) {
                binding.emptyNotesView.visibility = View.VISIBLE
            } else {
                binding.emptyNotesView.visibility = View.GONE
            }
        }
    }
}