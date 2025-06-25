package com.example.treetrack

import android.os.Environment
import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var searchView: SearchView
    private lateinit var recyclerView: RecyclerView
    private lateinit var dbRef: DatabaseReference
    private lateinit var adapter: TreeReportAdapter

    private val allTrees = mutableListOf<TreeData>()
    private val filteredTrees = mutableListOf<TreeData>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        searchView = findViewById(R.id.searchViewReports)
        recyclerView = findViewById(R.id.recyclerViewReports)

        adapter = TreeReportAdapter(filteredTrees) { tree -> showReportDialog(tree) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            dbRef = FirebaseDatabase.getInstance("https://treetrack-931db-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("trees")
                .child(userId)
            loadTrees()
        } else {
            Toast.makeText(this, "Please log in to view reports", Toast.LENGTH_SHORT).show()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(query: String?): Boolean {
                val q = query?.trim()?.lowercase() ?: ""
                filteredTrees.clear()
                filteredTrees.addAll(allTrees.filter { it.name.lowercase().contains(q) })
                adapter.updateList(filteredTrees)
                return true
            }
        })
    }

    private fun loadTrees() {
        val emptyState = findViewById<TextView>(R.id.tvEmptyState)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTrees.clear()

                for (child in snapshot.children) {
                    val tree = child.getValue(TreeData::class.java)
                    if (tree != null) {
                        // Ensure each tree has its Firebase ID (useful for filename or updates)
                        if (tree.id.isEmpty()) tree.id = child.key ?: ""
                        allTrees.add(tree)
                        println("✅ Loaded: ${tree.name}")
                    }
                }

                filteredTrees.clear()
                filteredTrees.addAll(allTrees)

                if (filteredTrees.isEmpty()) {
                    emptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    Toast.makeText(this@ReportsActivity, "No trees loaded", Toast.LENGTH_SHORT).show()
                } else {
                    emptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }

                adapter.updateList(filteredTrees.toMutableList()) // 🛠 fixed: use a clone
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ReportsActivity, "❌ Failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun showReportDialog(tree: TreeData) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_tree_report, null)
        val imageView = view.findViewById<ImageView>(R.id.ivDialogImage)
        val textView = view.findViewById<TextView>(R.id.tvDialogDetails)
        val btnPdf = view.findViewById<Button>(R.id.btnPrintTreePdf)

        // Show image if available
        if (!tree.healthImageBase64.isNullOrEmpty()) {
            val bytes = Base64.decode(tree.healthImageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            imageView.setImageBitmap(bitmap)
            imageView.visibility = ImageView.VISIBLE
        } else {
            imageView.visibility = ImageView.GONE
        }

        val age = ((System.currentTimeMillis() - tree.datePlanted) / (1000 * 60 * 60 * 24)).toInt()
        val status = when {
            tree.health >= 80 -> "Excellent"
            tree.health >= 60 -> "Good"
            tree.health >= 40 -> "Moderate"
            tree.health >= 20 -> "Poor"
            else -> "Critical"
        }

        textView.text = """
            🌳 ${tree.name}
            Description: ${tree.description}
            Planted: ${dateFormat.format(Date(tree.datePlanted))}
            Age: $age days
            Health: $status (${tree.health}%)
            Last Checked: ${tree.uploadTime ?: "N/A"}
        """.trimIndent()

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(true)
            .create()

        btnPdf.setOnClickListener {
            generatePDF(tree)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun generatePDF(tree: TreeData) {
        try {
            val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!folder.exists()) folder.mkdirs()

            val fileName = "TreeReport_${tree.id}_${System.currentTimeMillis()}.pdf"
            val pdfFile = File(folder, fileName)
            val writer = PdfWriter(FileOutputStream(pdfFile))
            val pdfDoc = PdfDocument(writer)
            val document = Document(pdfDoc)

            document.add(Paragraph("🌳 Tree Report").setBold())
            document.add(Paragraph("Name: ${tree.name}"))
            document.add(Paragraph("Description: ${tree.description}"))
            document.add(Paragraph("Planted: ${dateFormat.format(Date(tree.datePlanted))}"))
            document.add(Paragraph("Health: ${tree.health}%"))
            document.add(Paragraph("Last Checked: ${tree.uploadTime ?: "N/A"}"))

            if (!tree.healthImageBase64.isNullOrEmpty()) {
                val imgBytes = Base64.decode(tree.healthImageBase64, Base64.DEFAULT)
                val image = Image(ImageDataFactory.create(imgBytes)).scaleToFit(300f, 300f)
                document.add(image)
            }

            document.close()
            Toast.makeText(this, "✅ PDF saved to: ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}