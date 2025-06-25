package com.example.treetrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TreeAdapterForHealth(
    private val trees: List<Pair<String, TreeData>>,
    private val onTreeClick: (String, TreeData) -> Unit
) : RecyclerView.Adapter<TreeAdapterForHealth.TreeViewHolder>() {

    inner class TreeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvTreeName)
        val desc: TextView = view.findViewById(R.id.tvTreeDesc)
        val age: TextView = view.findViewById(R.id.tvTreeAge)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val (key, tree) = trees[position]
                    onTreeClick(key, tree)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tree_card_health, parent, false) // ✅ changed
        return TreeViewHolder(view)
    }

    override fun getItemCount(): Int = trees.size

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        val (_, tree) = trees[position]
        holder.name.text = tree.name
        holder.desc.text = tree.description

        val ageDays = ((System.currentTimeMillis() - tree.datePlanted) / (1000 * 60 * 60 * 24)).toInt()
        holder.age.text = "Age: $ageDays days"
    }
}