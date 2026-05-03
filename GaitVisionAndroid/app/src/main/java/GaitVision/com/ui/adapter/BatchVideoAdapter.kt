package GaitVision.com.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import GaitVision.com.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// One row per video the user picked for the batch.
data class BatchVideoRow(
    val uri: Uri,
    val displayName: String,
    val status: BatchVideoStatus = BatchVideoStatus.QUEUED,
    val score: Int? = null,
    val errorMessage: String? = null,
    // null = still extracting or not found; shown as "Date unknown" while queued
    val recordedDateMillis: Long? = null,
)

enum class BatchVideoStatus { QUEUED, RUNNING, DONE, FAILED }

class BatchVideoAdapter :
    ListAdapter<BatchVideoRow, BatchVideoAdapter.ViewHolder>(DIFF) {

    // Activity sets this to open a per-row date picker; null = rows not tappable
    var onDateEditClick: ((uri: Uri, currentMillis: Long?) -> Unit)? = null

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BatchVideoRow>() {
            override fun areItemsTheSame(a: BatchVideoRow, b: BatchVideoRow) = a.uri == b.uri
            override fun areContentsTheSame(a: BatchVideoRow, b: BatchVideoRow) = a == b
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_batch_video, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(item: View) : RecyclerView.ViewHolder(item) {
        private val tvName: TextView = item.findViewById(R.id.tvVideoName)
        private val tvStatus: TextView = item.findViewById(R.id.tvVideoStatus)
        private val tvScore: TextView = item.findViewById(R.id.tvVideoScore)
        private val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun bind(row: BatchVideoRow) {
            val ctx = itemView.context
            tvName.text = row.displayName

            val isQueued = row.status == BatchVideoStatus.QUEUED

            // While queued, show the extracted (or overridden) recording date.
            // Append "· tap to edit" as a low-key affordance hint.
            // Once the batch starts, status text takes over.
            tvStatus.text = when (row.status) {
                BatchVideoStatus.QUEUED -> {
                    val dateStr = row.recordedDateMillis
                        ?.let { dateFmt.format(Date(it)) }
                        ?: "Date unknown"
                    "$dateStr · tap to edit"
                }
                BatchVideoStatus.RUNNING -> "Running…"
                BatchVideoStatus.DONE -> "Done"
                BatchVideoStatus.FAILED -> row.errorMessage ?: "Failed"
            }
            tvStatus.setTextColor(ContextCompat.getColor(ctx, when (row.status) {
                BatchVideoStatus.QUEUED -> if (row.recordedDateMillis != null)
                    R.color.chart_axis_text else R.color.score_none
                BatchVideoStatus.RUNNING -> R.color.icon_light_blue
                BatchVideoStatus.DONE -> R.color.score_good
                BatchVideoStatus.FAILED -> R.color.score_poor
            }))

            // Only QUEUED rows are editable
            itemView.setOnClickListener {
                if (isQueued) onDateEditClick?.invoke(row.uri, row.recordedDateMillis)
            }

            if (row.score != null) {
                tvScore.text = row.score.toString()
                val color = when {
                    row.score >= 80 -> R.color.score_good
                    row.score >= 60 -> R.color.score_warn
                    else -> R.color.score_poor
                }
                tvScore.setTextColor(ContextCompat.getColor(ctx, color))
            } else {
                tvScore.text = "—"
                tvScore.setTextColor(ContextCompat.getColor(ctx, R.color.score_none))
            }
        }
    }
}
