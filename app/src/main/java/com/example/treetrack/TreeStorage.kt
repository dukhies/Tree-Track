package com.example.treetrack

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object TreeStorage {
    private val dbRef = FirebaseDatabase.getInstance("https://treetrack-931db-default-rtdb.asia-southeast1.firebasedatabase.app/")
        .reference
    private var userTrees: MutableList<TreeData> = mutableListOf()

    fun getTrees(): List<TreeData> = userTrees

    fun loadTrees(onComplete: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.e("TreeStorage", "❌ No user logged in. Cannot load trees.")
            onComplete()
            return
        }

        Log.d("TreeStorage", "📥 Loading trees for UID: $uid")

        dbRef.child("trees").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userTrees.clear()
                    for (treeSnap in snapshot.children) {
                        treeSnap.getValue(TreeData::class.java)?.let {
                            it.id = treeSnap.key ?: ""
                            userTrees.add(it)
                        }
                    }
                    Log.d("TreeStorage", "✅ Loaded ${userTrees.size} trees")
                    onComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("TreeStorage", "❌ Failed to load trees: ${error.message}")
                    onComplete()
                }
            })
    }

    fun addTree(context: Context, tree: TreeData, onComplete: (() -> Unit)? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.e("TreeStorage", "❌ No user logged in. Cannot add tree.")
            Toast.makeText(context, "❌ You are not logged in", Toast.LENGTH_SHORT).show()
            onComplete?.invoke()
            return
        }

        Log.d("TreeStorage", "📤 Adding tree for UID: $uid")

        val key = dbRef.child("trees").child(uid).push().key
        if (key == null) {
            Log.e("TreeStorage", "❌ Failed to generate Firebase key.")
            Toast.makeText(context, "❌ Failed to generate tree ID", Toast.LENGTH_SHORT).show()
            onComplete?.invoke()
            return
        }

        tree.id = key
        tree.datePlanted = System.currentTimeMillis()

        dbRef.child("trees").child(uid).child(key).setValue(tree)
            .addOnSuccessListener {
                Log.d("TreeStorage", "✅ Tree saved to Firebase: ${tree.name}")
                userTrees.add(tree)
                onComplete?.invoke()
            }
            .addOnFailureListener {
                Log.e("TreeStorage", "❌ Failed to add tree: ${it.message}")
                Toast.makeText(context, "❌ Firebase error: ${it.message}", Toast.LENGTH_LONG).show()
                onComplete?.invoke()
            }
    }

    fun updateTree(tree: TreeData, onComplete: (() -> Unit)? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val treeId = tree.id
        if (treeId.isEmpty()) return

        dbRef.child("trees").child(uid).child(treeId).setValue(tree)
            .addOnSuccessListener {
                val index = userTrees.indexOfFirst { it.id == treeId }
                if (index != -1) {
                    userTrees[index] = tree
                }
                onComplete?.invoke()
            }
            .addOnFailureListener {
                Log.e("TreeStorage", "❌ Failed to update tree: ${it.message}")
                onComplete?.invoke()
            }
    }

    fun deleteTree(id: String, onComplete: (() -> Unit)? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        dbRef.child("trees").child(uid).child(id).removeValue()
            .addOnSuccessListener {
                userTrees.removeAll { it.id == id }
                onComplete?.invoke()
            }
            .addOnFailureListener {
                Log.e("TreeStorage", "❌ Failed to delete tree: ${it.message}")
                onComplete?.invoke()
            }
    }

    fun clearTrees() {
        Log.d("TreeStorage", "🧹 Local tree list cleared")
        userTrees.clear()
    }
}