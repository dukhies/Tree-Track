    package com.example.treetrack

    import android.annotation.SuppressLint
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.Button
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView
    import java.text.SimpleDateFormat
    import java.util.*

    class TreeReportAdapter(
        private var trees: MutableList<TreeData>,
        private val onItemClicked: (TreeData) -> Unit
    ) : RecyclerView.Adapter<TreeReportAdapter.VH>() {

        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        inner class VH(item: View) : RecyclerView.ViewHolder(item) {
            val name = item.findViewById<TextView>(R.id.tvReportName)
            val summary = item.findViewById<TextView>(R.id.tvReportSummary)
            val button = item.findViewById<Button>(R.id.btnPrintTreePdf)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tree_report, parent, false)
            return VH(view)
        }

        override fun getItemCount(): Int = trees.size

        override fun onBindViewHolder(holder: VH, pos: Int) {

            val tree = trees[pos]
            val age = ((System.currentTimeMillis() - tree.datePlanted) / (1000 * 60 * 60 * 24)).toInt()
            val healthStatus = when {
                tree.health >= 80 -> "Excellent"
                tree.health >= 60 -> "Good"
                tree.health >= 40 -> "Moderate"
                tree.health >= 20 -> "Poor"
                else -> "Critical"
            }

            holder.name.text = tree.name
            holder.summary.text = "Planted: ${dateFormat.format(Date(tree.datePlanted))} | Health: $healthStatus (${tree.health}%)"
            holder.button.setOnClickListener { onItemClicked(tree) }
        }

        fun updateList(newList: List<TreeData>) {
            trees = newList.toMutableList()
            notifyDataSetChanged()
        }
    }
