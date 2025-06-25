package com.example.treetrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class TreeAdapter(
    private val trees: List<Pair<String, TreeData>>,
    private val onUpdate: (String, TreeData) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<TreeAdapter.TreeViewHolder>() {

    inner class TreeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvTreeName)
        val desc: TextView = view.findViewById(R.id.tvTreeDesc)
        val age: TextView = view.findViewById(R.id.tvTreeAge)
        val btnUpdate: ImageButton = view.findViewById(R.id.btnUpdate)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tree_card, parent, false)
        return TreeViewHolder(view)
    }

    override fun getItemCount(): Int = trees.size

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        val (key, tree) = trees[position]
        holder.name.text = tree.name
        holder.desc.text = tree.description

        val ageDays = ((System.currentTimeMillis() - tree.datePlanted) / (1000 * 60 * 60 * 24)).toInt()
        holder.age.text = "Age: $ageDays days"

        holder.btnUpdate.setOnClickListener {
            onUpdate(key, tree)
        }

        holder.btnDelete.setOnClickListener {
            val treeName = tree.name
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Delete Tree")
                .setMessage("Delete \"$treeName\"?")
                .setPositiveButton("Yes") { _, _ ->
                    onDelete(key)  // ✅ Now correctly references passed delete callback
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}