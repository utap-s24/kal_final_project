package edu.utap.kal

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import edu.utap.kal.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    // See: https://developer.android.com/training/basics/intents/result
    // Only an activity can create a registerForActivityResult object, so
    // allocate it here, and pass to the AuthWrap object
    private val signInLauncher =
        registerForActivityResult(FirebaseAuthUIActivityResultContract()) {
            // AuthWrap will handle it
        }
    // TODO: expand all/collapse all in menu
    private fun initMenu() {
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_profile -> {
                        navController.navigate(R.id.settingsFragment)
                        true
                    }
                    R.id.menu_logout -> {
                        AuthWrap.logout()
                        true
                    }
                    else -> false
                }
            }
        })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the layout for the layout we created
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initMenu()

        // https://developer.android.com/guide/navigation/navigation-getting-started
        navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Firestore login, fetch initial
        AuthWrap.signInLauncher = signInLauncher
        AuthWrap.login()
        binding.indeterminateBar.visibility = View.GONE
//        viewModel.fetchInitialNotes() {
//            binding.indeterminateBar.visibility = View.GONE
//        }


        AuthWrap.observeCurrentUser().observe(this) { user ->
            if (user != null) {
                Log.d("XXX", "finally fetching the home fragment's notes")
                val currentUser = AuthWrap.getCurrentUser()
                viewModel.fetchInitialNotes(currentUser.uid) {  } // FIND ME
            }
        }
    }

    // navigateUp:
    // If we came here from within the app, pop the back stack
    // If we came here from another app, return to it.
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}
