package edu.utap.kal.view

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import edu.utap.kal.AuthWrap
import edu.utap.kal.MainViewModel
import edu.utap.kal.R
import edu.utap.kal.ViewModelDBHelper
import edu.utap.kal.databinding.SettingsBinding


class SettingsFragment  : Fragment(R.layout.settings) {
    private val viewModel: MainViewModel by activityViewModels()

    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Get rid of settings menu icon
                menu.clear()
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner)
    }

    // No need for onCreateView because we passed R.layout to Fragment constructor
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SettingsBinding.bind(view)
        // Send this request over the network as soon as we can
        //SSS
        super.onViewCreated(view, savedInstanceState)
        initMenu()

        binding.userRV.layoutManager = LinearLayoutManager(context)
        val adapter = UserAdapter(viewModel) {
            val navAction = SettingsFragmentDirections.actionSettingsToOneUserFragment(it)
            findNavController().navigate(navAction)
        }
        binding.userRV.adapter = adapter

        // Restore state for the fragment elements
        val currentUser = AuthWrap.getCurrentUser()
        val db = ViewModelDBHelper()
        db.fetchNameAndBio(currentUser.uid, binding.nameEdit, binding.bioEdit)
        viewModel.fetchInitialFollowing()
        viewModel.fetchInitialAllUsers()
        val initialFollowingList = viewModel.observeFollowing()
        Log.d("XXX", "initial Following list: ${initialFollowingList.value}")
        adapter.submitList(initialFollowingList.value)

        // on click listeners for the save buttons. need to send data to firestore to update vals
        binding.nameSaveBut.setOnClickListener {
            // name cannot be empty
            if (binding.nameEdit.text.isEmpty()) {
                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                // Update firestore with this new name
                db.updateName(currentUser.uid, binding.nameEdit.text.toString())
            }
        }
        binding.bioSaveBut.setOnClickListener {
            // update firestore with new bio
            db.updateBio(currentUser.uid, binding.bioEdit.text.toString())
        }

        // Handle search bar
        binding.searchButton.setOnClickListener {
            if (binding.searchEdit.text.isNotEmpty()) {
                viewModel.setSearchTerm(binding.searchEdit.text.toString())
            }
        }
        binding.searchEdit.addTextChangedListener {
            viewModel.setSearchTerm(it.toString())
        }

        // observe following list
        viewModel.observeFollowing().observe(viewLifecycleOwner) {
            Log.d("XXX", "a change was observed in the following list")
            adapter.submitList(it)
        }


    }
}