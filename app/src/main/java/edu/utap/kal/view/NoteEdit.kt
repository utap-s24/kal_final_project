package edu.utap.kal.view

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import edu.utap.kal.MainViewModel
import edu.utap.kal.R
import edu.utap.kal.databinding.NoteEditBinding

class NoteEdit :
    Fragment(R.layout.note_edit) {
    private lateinit var imageAdapter: ImageAdapter
    // TODO: This should really be part of view model to allow
    // navigation away from in progress note editing
    // A little bit of a pain in the butt that this is not a MutableList
    // but the ListAdapter needs two lists with distinct references
    // to work properly.  So either hassle with List or make it
    // a MutableList and then have to call notify* on adapter.
    private lateinit var pictureUUIDs: List<String>
    private var position = -1
    private val viewModel: MainViewModel by activityViewModels()
    private val args: NoteEditArgs by navArgs()
    // It is a real bummer that we must initialize a registerForActivityResult
    // here or in onViewCreated.  You CAN'T initialize it in an onClickListener
    // where it could capture state like the file name.
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            viewModel.pictureSuccess {
                val newList = pictureUUIDs.toMutableList()
                newList.add(it)
                Log.d(
                    javaClass.simpleName,
                    "photo added ${it} len ${newList.size}"
                )
                pictureUUIDs = newList
                imageAdapter.submitList(pictureUUIDs)
            }
        } else {
            viewModel.pictureFailure()
        }
    }

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
        super.onViewCreated(view, savedInstanceState)
        val binding = NoteEditBinding.bind(view)
        initMenu()
        position = args.position
        pictureUUIDs = if(position == -1) {
            listOf()
        } else {
            val note = viewModel.getNote(position)
            binding.inputET.text.insert(0, note.text)
            note.pictureUUIDs
        }
        // Put cursor in edit text
        binding.inputET.requestFocus()

        binding.photosRV.layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        imageAdapter = ImageAdapter(viewModel) { pictureUUIDPosition ->
            Log.d(javaClass.simpleName, "pictureUUIDs del $pictureUUIDPosition")
            val shorterList = pictureUUIDs.toMutableList()
            shorterList.removeAt(pictureUUIDPosition)
            pictureUUIDs = shorterList
            imageAdapter.submitList(pictureUUIDs)
        }
        binding.photosRV.adapter = imageAdapter
        imageAdapter.submitList(pictureUUIDs)

        binding.saveButton.setOnClickListener {
            val inputText = binding.inputET.text.toString()
            if (inputText.isEmpty()) {
                Toast.makeText(activity,
                        "Enter note!",
                        Toast.LENGTH_LONG).show()
            } else {
                if(position == -1) {
                    Log.d(javaClass.simpleName, "create note len ${pictureUUIDs.size} pos $position")
                    viewModel.createNote(inputText, pictureUUIDs)
                } else {
                    Log.d(javaClass.simpleName, "update list len ${pictureUUIDs.size} pos $position")
                    viewModel.updateNote(position, inputText, pictureUUIDs)
                }
                findNavController().popBackStack()
            }
        }
        binding.cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.cameraButton.setOnClickListener{
            TakePictureWrapper.takePicture(requireContext(),
                viewModel,
                cameraLauncher)
        }
    }
}