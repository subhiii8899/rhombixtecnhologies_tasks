package com.example.vivora

import android.app.Application
import com.cloudinary.android.MediaManager

class VivoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Cloudinary
        val config = mapOf(
            "cloud_name" to "droixs6gx",
            "api_key" to "136168773953969",
            "api_secret" to "TiZBLdNrHarYi7qhOtFE8BvrczE"
        )
        MediaManager.init(this, config)
    }
}
