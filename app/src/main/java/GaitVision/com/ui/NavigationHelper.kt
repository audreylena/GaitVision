package GaitVision.com.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import GaitVision.com.R

object NavigationHelper {

    fun setupBottomNav(activity: Activity) {
        val navHome = activity.findViewById<View>(R.id.navHome)
        val navHelp = activity.findViewById<View>(R.id.navHelp)
        val navInfo = activity.findViewById<View>(R.id.navInfo)
        val navSettings = activity.findViewById<View>(R.id.navSettings)

        // Highlight current tab
        when (activity) {
            is DashboardActivity -> highlightTab(activity, R.id.ivNavHome, R.id.tvNavHome)
            is HelpActivity -> highlightTab(activity, R.id.ivNavHelp, R.id.tvNavHelp)
            is InfoActivity -> highlightTab(activity, R.id.ivNavInfo, R.id.tvNavInfo)
            is SettingsActivity -> highlightTab(activity, R.id.ivNavSettings, R.id.tvNavSettings)
        }

        // Set listeners
        navHome.setOnClickListener {
            if (activity !is DashboardActivity) {
                val intent = Intent(activity, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                activity.startActivity(intent)
                activity.overridePendingTransition(0, 0)
                activity.finish() // Optional: finish current activity to avoid stack buildup
            }
        }

        val navigateTo = { targetActivityClass: Class<out Activity> ->
            if (activity.javaClass != targetActivityClass) {
                activity.startActivity(Intent(activity, targetActivityClass))
                activity.overridePendingTransition(0, 0)
                if (activity !is DashboardActivity) activity.finish()
            }
        }

        navHelp.setOnClickListener { navigateTo(HelpActivity::class.java) }

        navInfo.setOnClickListener { navigateTo(InfoActivity::class.java) }

        navSettings.setOnClickListener { navigateTo(SettingsActivity::class.java) }
    }

    private fun highlightTab(activity: Activity, iconId: Int, textId: Int) {
        val icon = activity.findViewById<ImageView>(iconId)
        val text = activity.findViewById<TextView>(textId)
        
        val selectedColor = ContextCompat.getColor(activity, R.color.primary_blue)
        
        icon.setColorFilter(selectedColor)
        text.setTextColor(selectedColor)
    }
}
