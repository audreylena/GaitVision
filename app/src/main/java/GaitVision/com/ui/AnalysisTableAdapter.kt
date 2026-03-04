package GaitVision.com.ui

import android.graphics.Color
import androidx.core.content.ContextCompat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import GaitVision.com.data.AnalysisResult

/**
 * RecyclerView adapter for the analysis history data table.
 *
 * Rows are built **programmatically** to match whatever [columns] was built
 * via reflection in [AnalysisHistoryActivity].  Adding new fields to
 * [AnalysisResult] automatically adds columns — this adapter never needs to
 * be modified for that.
 */
class AnalysisTableAdapter(
    private val items: List<AnalysisResult>,
    private val columns: List<AnalysisHistoryActivity.ColumnDef>,
    private val onRowClick: (AnalysisResult) -> Unit
) : RecyclerView.Adapter<AnalysisTableAdapter.RowViewHolder>() {

    inner class RowViewHolder(val row: LinearLayout) : RecyclerView.ViewHolder(row)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val density = parent.context.resources.displayMetrics.density
        val row = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, (10 * density).toInt(), 0, (10 * density).toInt())
            isClickable = true
            isFocusable = true
            setBackgroundResource(android.R.drawable.list_selector_background)
        }

        // Create one TextView per column (plus "View Results" action cell)
        columns.forEach { col ->
            val tv = TextView(parent.context).apply {
                textSize = 13f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                minWidth = (col.minWidthDp * density).toInt()
                setPadding(
                    (12 * density).toInt(), 0,
                    (12 * density).toInt(), 0
                )
                tag = col.label   // identify cell by column label
            }
            row.addView(tv)
        }

        // Action cell
        val actionTv = TextView(parent.context).apply {
            text = "View →"
            textSize = 12f
            setTextColor(ContextCompat.getColor(parent.context, R.color.table_header_text))
            gravity = Gravity.CENTER
            minWidth = (96 * density).toInt()
            setPadding(
                (12 * density).toInt(), 0,
                (12 * density).toInt(), 0
            )
            tag = "action"
        }
        row.addView(actionTv)

        return RowViewHolder(row)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val result = items[position]
        val density = holder.row.context.resources.displayMetrics.density

        // Alternate row backgrounds for readability
        holder.row.setBackgroundColor(
            if (position % 2 == 0)
                ContextCompat.getColor(holder.row.context, R.color.table_row_even)
            else
                ContextCompat.getColor(holder.row.context, R.color.table_row_odd)
        )

        // Bind each data cell
        for (i in 0 until holder.row.childCount - 1) {  // last child is action
            val tv = holder.row.getChildAt(i) as? TextView ?: continue
            val col = columns.getOrNull(i) ?: continue
            val text = col.getValue(result)
            tv.text = text

            // Colour-code the Score column
            if (col.label == "Score") {
                val score = result.overallScore
                val ctx = tv.context
                tv.setTextColor(when {
                    score == null -> ContextCompat.getColor(ctx, R.color.score_none)
                    score >= 80   -> ContextCompat.getColor(ctx, R.color.score_good)
                    score >= 60   -> ContextCompat.getColor(ctx, R.color.score_warn)
                    else          -> ContextCompat.getColor(ctx, R.color.score_poor)
                })
            } else {
                tv.setTextColor(Color.WHITE)
            }
        }

        // Row click
        holder.row.setOnClickListener { onRowClick(result) }
    }

    override fun getItemCount() = items.size
}
