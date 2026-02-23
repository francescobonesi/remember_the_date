package com.frabon.rememberthedate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import com.frabon.rememberthedate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    companion object {
        const val PREFS_NAME = "app_prefs"
        const val KEY_HAS_REQUESTED_NOTIFICATION_PERMISSION =
            "has_requested_notification_permission"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, getString(R.string.permission_granted_toast), Toast.LENGTH_SHORT)
                .show()
        } else {
            showPermissionDeniedPermanentlyDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navController.graph)

        setupActionBarWithNavController(navController, appBarConfiguration)

        askNotificationPermissionOnce()
    }

    private fun askNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val hasRequestedBefore =
                prefs.getBoolean(KEY_HAS_REQUESTED_NOTIFICATION_PERMISSION, false)

            if (!hasRequestedBefore) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                prefs.edit { putBoolean(KEY_HAS_REQUESTED_NOTIFICATION_PERMISSION, true) }
            }
        }
    }

    private fun showPermissionDeniedPermanentlyDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_denied_dialog_title))
            .setMessage(getString(R.string.permission_denied_dialog_message))
            .setNegativeButton(getString(R.string.permission_dialog_ok), null)
            .setPositiveButton(getString(R.string.permission_dialog_go_to_settings)) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}