package com.example.treetrack

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.osmdroid.util.GeoPoint
import java.util.*

object FirebaseTreeStorage {
    private val dbRef: DatabaseReference
        get() = FirebaseDatabase.getInstance().reference
            .child("trees")
            .child(FirebaseAuth.getInstance().currentUser?.uid ?: "unknown")

    private val trees = mutableListOf<TreeData>()

    fun addTree(tree: TreeData, callback: (() -> Unit)? = null) {
        val key = dbRef.push().key ?: return
        dbRef.child(key).setValue(tree).addOnCompleteListener {
            if (it.isSuccessful) {
                trees.add(tree)
                callback?.invoke()
            }
        }
    }

    fun getTrees(callback: (List<TreeData>) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trees.clear()
                for (treeSnap in snapshot.children) {
                    val tree = treeSnap.getValue(TreeData::class.java)
                    tree?.let { trees.add(it) }
                }
                callback(trees)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(emptyList())
            }
        })
    }

    fun clear() {
        trees.clear()
    }
}
