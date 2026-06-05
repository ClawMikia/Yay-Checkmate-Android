package com.yaycheckmate.utils

import android.content.Context
import android.view.View
import android.app.Activity
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.yaycheckmate.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("MMM dd yyyy", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFormattedDateTime(): String {
    val sdf = SimpleDateFormat("MMM dd yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Long.toFormattedDuration(): String {
    val minutes = TimeUnit.SECONDS.toMinutes(this)
    val seconds = this - TimeUnit.MINUTES.toSeconds(minutes)
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun Context.toast(message: String) {
    if (this is Activity) {
        val rootView = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
        val snackbarView = snackbar.view
        
        // Use the custom background with border
        snackbarView.background = ContextCompat.getDrawable(this, R.drawable.bg_snackbar)
        
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.apply {
            setTextColor(ContextCompat.getColor(this@toast, R.color.colorText))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 14f
        }
        
        // Add padding to ensure text doesn't stick to edges of the snackbar itself
        snackbarView.setPadding(32, 16, 32, 16)
        
        // Add margin to make it float and not stick to screen edges
        val params = snackbarView.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        params?.let {
            val margin = 48
            it.setMargins(margin, margin, margin, margin + 100) // Extra bottom margin for bottom nav
            snackbarView.layoutParams = it
        }
        
        snackbar.show()
    } else {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}



fun Int.toXpProgressText(currentXp: Int, nextLevelXp: Int): String {
    return "$currentXp / $nextLevelXp XP"
}
