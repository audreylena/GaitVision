package GaitVision.com.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import GaitVision.com.R
import GaitVision.com.data.Patient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentPatientsAdapter(
    private val onPatientClick: (Patient) -> Unit
) : ListAdapter<Patient, RecentPatientsAdapter.ViewHolder>(PatientDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_patient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvPatientName)
        private val tvDetails: TextView = itemView.findViewById(R.id.tvPatientDetails)

        fun bind(patient: Patient) {
            tvName.text = patient.fullName
            
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            val dateStr = dateFormat.format(Date(patient.lastModifiedAt))
            
            // Format: "ID: 123 • Age: 45 • Last modified: Oct 24"
            tvDetails.text = "ID: ${patient.participantId} • Age: ${patient.age ?: "-"} • Last modified: $dateStr"

            itemView.setOnClickListener {
                onPatientClick(patient)
            }
        }
    }

    class PatientDiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean {
            return oldItem.participantId == newItem.participantId
        }

        override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean {
            return oldItem == newItem
        }
    }
}
