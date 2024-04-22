package edu.utap.kal.view

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import edu.utap.kal.MainViewModel
import edu.utap.kal.R
import edu.utap.kal.databinding.ImageRollFragmentBinding

class ImageRoll : Fragment(R.layout.image_roll_fragment) {
    private val viewModel: MainViewModel by activityViewModels()

    // Get rid of image roll menu icon
    private fun initMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // No image roll menu in image roll fragment
                menu.clear()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }, viewLifecycleOwner)
    }
    // No need for onCreateView because we passed R.layout to Fragment constructor
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = ImageRollFragmentBinding.bind(view)
        // Send this request over the network as soon as we can
        //SSS
        viewModel.refreshAllImages()
        super.onViewCreated(view, savedInstanceState)
        initMenu()

        binding.imageViewRV.layoutManager =
                StaggeredGridLayoutManager(2,
                        StaggeredGridLayoutManager.VERTICAL)
        val adapter = ImageAdapter(viewModel)
        binding.imageViewRV.adapter = adapter

        // Whenever allImages refreshes, we will update
        viewModel.observeAllImages().observe(viewLifecycleOwner) {
            Log.d(javaClass.simpleName, "allImages observe len ${it.size}")
            adapter.submitList(it)
        }
        //EEE // XXX Write the ImageRoll!
    }
}