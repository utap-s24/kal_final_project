package edu.utap.kal.view

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.firebase.firestore.GeoPoint
import edu.utap.kal.MainViewModel
import edu.utap.kal.R
import edu.utap.kal.databinding.NoteEditBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    private lateinit var map: GoogleMap
    private lateinit var geocoder: Geocoder
    private var locationPermissionGranted = false
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

    private fun checkGooglePlayServices() {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode =
            googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 257)?.show()
            } else {
                Log.i(javaClass.simpleName,
                    "This device must install Google Play Services.")
            }
        }
    }

    private fun requestPermission() {
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    locationPermissionGranted = true
                } else -> {
                Toast.makeText(context,
                    "Unable to show location - permission required",
                    Toast.LENGTH_LONG).show()
            }
            }
        }
        locationPermissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    private fun processLocation(location: String): GeoPoint {
        Log.d("XXX", "ProcessLocation is called")
        val addresses = Geocoder(requireContext()).getFromLocationName(location, 1)
        var retAddress: GeoPoint
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            retAddress = GeoPoint(address.latitude, address.longitude)
        } else {
            Toast.makeText(requireContext(), "Was not a valid location.", Toast.LENGTH_LONG).show()
            retAddress = GeoPoint(0.0, 0.0)
        }
        return retAddress
    }

    // No need for onCreateView because we passed R.layout to Fragment constructor
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = NoteEditBinding.bind(view)
        initMenu()
        position = args.position

        // Google Maps work
        checkGooglePlayServices()
        requestPermission()

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
                Toast.makeText(requireContext(), "Enter note!", Toast.LENGTH_LONG).show()
            } else {
                val locationVal = binding.geocoderEditText.text.toString()
                var locationGeopoint = GeoPoint(0.0, 0.0)
                if (locationVal.isNotEmpty()) {
                    locationGeopoint = processLocation(locationVal)
                }

                if (position == -1) {
                    viewModel.createNote(inputText, pictureUUIDs, locationGeopoint)
                } else {
                    viewModel.updateNote(position, inputText, pictureUUIDs, locationGeopoint)
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