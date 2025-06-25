package com.example.treetrack

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyTreesActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var treeAdapter: TreeAdapter
    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var emptyStateLayout: View
    private val treeList = mutableListOf<Pair<String, TreeData>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_trees)

        recyclerView = findViewById(R.id.rvTrees)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        recyclerView.layoutManager = LinearLayoutManager(this)

        auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        dbRef = FirebaseDatabase.getInstance("https://treetrack-931db-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("trees")
            .child(uid)

        treeAdapter = TreeAdapter(treeList, ::showUpdateDialog, ::deleteTree)
        recyclerView.adapter = treeAdapter

        loadTrees()
    }

    private fun loadTrees() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                treeList.clear()
                for (treeSnap in snapshot.children) {
                    val tree = treeSnap.getValue(TreeData::class.java)
                    if (tree != null) {
                        treeList.add(treeSnap.key!! to tree)
                    }
                }
                treeAdapter.notifyDataSetChanged()

                // Show or hide "no trees yet" background
                emptyStateLayout.visibility = if (treeList.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MyTreesActivity, "Failed to load trees", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteTree(key: String) {
        dbRef.child(key).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Tree deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete tree", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUpdateDialog(key: String, tree: TreeData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_tree, null)
        val inputName = dialogView.findViewById<EditText>(R.id.editTreeName)
        val inputDesc = dialogView.findViewById<EditText>(R.id.editTreeDesc)

        inputName.setText(tree.name)
        inputDesc.setText(tree.description)

        AlertDialog.Builder(this)
            .setTitle("Update Tree")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedTree = tree.copy(
                    name = inputName.text.toString(),
                    description = inputDesc.text.toString()
                )
                dbRef.child(key).setValue(updatedTree)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Tree updated", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to update tree", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}