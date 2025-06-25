package com.example.treetrack

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvEmail: TextView
    private lateinit var tvUid: TextView
    private lateinit var switchTheme: Switch
    private lateinit var tvAppInfo: TextView
    private lateinit var btnLogout: Button

    private lateinit var preferences: SharedPreferences
    private val PREFS_NAME = "app_prefs"
    private val THEME_KEY = "dark_mode"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize views
        tvEmail = findViewById(R.id.tvEmail)
        tvUid = findViewById(R.id.tvUid)
        switchTheme = findViewById(R.id.switchTheme)
        tvAppInfo = findViewById(R.id.tvAppInfo)
        btnLogout = findViewById(R.id.btnLogoutSettings)

        // Account Info
        val user = FirebaseAuth.getInstance().currentUser
        tvEmail.text = user?.email ?: "No email"
        tvUid.text = "UID: ${user?.uid ?: "N/A"}"

        // App Info
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        tvAppInfo.text = "TreeTrack v$versionName (Android ${Build.VERSION.RELEASE})"

        // Theme toggle
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isDarkMode = preferences.getBoolean(THEME_KEY, false)
        switchTheme.isChecked = isDarkMode
        applyTheme(isDarkMode)

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean(THEME_KEY, isChecked).apply()
            applyTheme(isChecked)
        }

        // Logout
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            TreeStorage.clearTrees()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun applyTheme(darkMode: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_NO
        )
    }
}
