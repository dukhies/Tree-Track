package com.example.treetrack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private lateinit var btnTrackTrees: Button
    private lateinit var btnMyTrees: Button
    private lateinit var btnHealthMonitor: Button
    private lateinit var btnReports: Button
    private lateinit var btnSettings: Button

    private val LOCATION_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize buttons
        btnTrackTrees = findViewById(R.id.btnTrackTrees)
        btnMyTrees = findViewById(R.id.btnMyTrees)
        btnHealthMonitor = findViewById(R.id.btnHealthMonitor)
        btnReports = findViewById(R.id.btnReports)
        btnSettings = findViewById(R.id.btnSettings)

        btnTrackTrees.setOnClickListener {
            val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(this, locationPermission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(locationPermission), LOCATION_PERMISSION_REQUEST)
            } else {
                startActivity(Intent(this, TrackTreesActivity::class.java))
            }
        }

        btnMyTrees.setOnClickListener {
            startActivity(Intent(this, MyTreesActivity::class.java))
        }

        btnHealthMonitor.setOnClickListener {
            startActivity(Intent(this, HealthMonitorActivity::class.java))
        }

        btnReports.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showSettingsDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val imgProfile = view.findViewById<ImageView>(R.id.imgProfile)
        val etUsername = view.findViewById<EditText>(R.id.etUsername)
        val btnSave = view.findViewById<Button>(R.id.btnSaveUsername)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // Center the text inside the EditText
        etUsername.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        etUsername.gravity = android.view.Gravity.CENTER

        // Load saved username
        val sharedPrefs = getSharedPreferences("TreeTrackPrefs", MODE_PRIVATE)
        val savedName = sharedPrefs.getString("username", "")
        etUsername.setText(savedName)

        btnSave.setOnClickListener {
            val newName = etUsername.text.toString().trim()
            sharedPrefs.edit().putString("username", newName).apply()
            Toast.makeText(this, "Username saved!", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    TreeStorage.clearTrees()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, TrackTreesActivity::class.java))
        } else {
            Toast.makeText(this, "Location permission is required to track trees.", Toast.LENGTH_SHORT).show()
        }
    }
}