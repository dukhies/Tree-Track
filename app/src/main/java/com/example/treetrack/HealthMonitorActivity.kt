package com.example.treetrack

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class HealthMonitorActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var healthMonitorContainer: ScrollView
    private lateinit var tvHealthHeader: TextView
    private lateinit var tvHealthValue: TextView
    private lateinit var tvHealthStatus: TextView
    private lateinit var seekBarHealth: SeekBar
    private lateinit var ivHealthThumbnail: ImageView
    private lateinit var tvLastCheckDate: TextView
    private lateinit var tvUploadTime: TextView
    private lateinit var btnHealthCamera: Button
    private lateinit var btnHealthGallery: Button
    private lateinit var btnUpdateHealth: Button
    private lateinit var btnDeleteImage: Button

    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private val treeList = mutableListOf<Pair<String, TreeData>>()
    private lateinit var treeAdapter: TreeAdapterForHealth
    private var selectedTree: Pair<String, TreeData>? = null
    private var photoUri: Uri? = null
    private val dateFormat = SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            photoUri = it
            ivHealthThumbnail.setImageURI(it)
            updateUploadTime()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = photoUri
        if (success && uri != null) {
            ivHealthThumbnail.setImageURI(uri)
            updateUploadTime()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_monitor)

        recyclerView = findViewById(R.id.rvHealthTrees)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        healthMonitorContainer = findViewById(R.id.healthMonitorContainer)
        tvHealthHeader = findViewById(R.id.tvHealthHeader)
        tvHealthValue = findViewById(R.id.tvHealthValue)
        tvHealthStatus = findViewById(R.id.tvHealthStatus)
        seekBarHealth = findViewById(R.id.seekBarHealth)
        ivHealthThumbnail = findViewById(R.id.ivHealthThumbnail)
        tvLastCheckDate = findViewById(R.id.tvLastCheckDate)
        tvUploadTime = findViewById(R.id.tvUploadTime)
        btnHealthCamera = findViewById(R.id.btnHealthCamera)
        btnHealthGallery = findViewById(R.id.btnHealthGallery)
        btnUpdateHealth = findViewById(R.id.btnUpdateHealth)
        btnDeleteImage = findViewById(R.id.btnDeleteImage)

        recyclerView.layoutManager = LinearLayoutManager(this)
        treeAdapter = TreeAdapterForHealth(treeList) { key, tree ->
            selectedTree = key to tree
            showHealthMonitorForTree(key, tree)
        }
        recyclerView.adapter = treeAdapter

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return

        dbRef = FirebaseDatabase.getInstance("https://treetrack-931db-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("trees")
            .child(uid)

        loadTrees()
        setupHealthSlider()
        setupClickListeners()
    }

    private fun loadTrees() {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                treeList.clear()
                for (treeSnap in snapshot.children) {
                    val tree = treeSnap.getValue(TreeData::class.java)
                    tree?.let {
                        treeList.add(treeSnap.key!! to it)
                    }
                }

                if (treeList.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    healthMonitorContainer.visibility = View.GONE
                } else {
                    emptyStateLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    healthMonitorContainer.visibility = View.GONE
                    treeAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HealthMonitorActivity, "Failed to load trees", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showHealthMonitorForTree(key: String, tree: TreeData) {
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE
        healthMonitorContainer.visibility = View.VISIBLE

        tvHealthHeader.text = "Tree Health Monitor: ${tree.name}"
        seekBarHealth.progress = tree.health
        tvHealthValue.text = "${tree.health}%"
        updateHealthStatus(tree.health)
        updateLastCheckDate()
        tvUploadTime.text = "Upload Time: ${tree.uploadTime ?: "N/A"}"

        if (!tree.healthImageBase64.isNullOrEmpty()) {
            val imageBytes = Base64.decode(tree.healthImageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ivHealthThumbnail.setImageBitmap(bitmap)
        } else {
            ivHealthThumbnail.setImageResource(android.R.color.transparent)
        }
    }

    private fun setupHealthSlider() {
        seekBarHealth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvHealthValue.text = "$progress%"
                updateHealthStatus(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupClickListeners() {
        btnHealthGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnHealthCamera.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
            } else {
                openCamera()
            }
        }

        btnUpdateHealth.setOnClickListener {
            val (key, tree) = selectedTree ?: return@setOnClickListener
            val newHealth = seekBarHealth.progress
            tree.health = newHealth

            photoUri?.let { uri ->
                val base64Image = uriToBase64(uri)
                tree.healthImageBase64 = base64Image
                tree.uploadTime = dateFormat.format(Date())
                tvUploadTime.text = "Upload Time: ${tree.uploadTime}"
            }

            dbRef.child(key).setValue(tree)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Health updated", Toast.LENGTH_SHORT).show()
                    updateLastCheckDate()
                    photoUri = null
                }
                .addOnFailureListener {
                    Toast.makeText(this, "❌ Failed to update", Toast.LENGTH_SHORT).show()
                }
        }

        btnDeleteImage.setOnClickListener {
            val (key, tree) = selectedTree ?: return@setOnClickListener
            tree.healthImageBase64 = null
            tree.uploadTime = null

            dbRef.child(key).setValue(tree)
                .addOnSuccessListener {
                    Toast.makeText(this, "🗑️ Image deleted", Toast.LENGTH_SHORT).show()
                    ivHealthThumbnail.setImageResource(android.R.color.transparent)
                    tvUploadTime.text = "Upload Time: N/A"
                }
        }
    }

    private fun openCamera() {
        val photoFile = File.createTempFile("tree_photo", ".jpg", cacheDir)
        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            photoFile
        )
        photoUri = uri
        cameraLauncher.launch(uri)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateHealthStatus(healthValue: Int) {
        val status = when {
            healthValue >= 80 -> "Excellent Health"
            healthValue >= 60 -> "Good Health"
            healthValue >= 40 -> "Moderate Health"
            healthValue >= 20 -> "Poor Health"
            else -> "Critical Health"
        }

        val color = when {
            healthValue >= 80 -> android.graphics.Color.GREEN
            healthValue >= 60 -> android.graphics.Color.rgb(0, 150, 0)
            healthValue >= 40 -> android.graphics.Color.YELLOW
            healthValue >= 20 -> android.graphics.Color.rgb(255, 165, 0)
            else -> android.graphics.Color.RED
        }

        tvHealthStatus.text = status
        tvHealthStatus.setTextColor(color)
    }

    private fun updateLastCheckDate() {
        val currentDate = dateFormat.format(Date())
        tvLastCheckDate.text = "Last Check: $currentDate"
    }

    private fun updateUploadTime() {
        val currentTime = dateFormat.format(Date())
        tvUploadTime.text = "Upload Time: $currentTime"
    }

    override fun onBackPressed() {
        if (healthMonitorContainer.visibility == View.VISIBLE) {
            healthMonitorContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }
}