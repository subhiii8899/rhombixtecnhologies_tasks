package com.example.vivora

import android.app.Application
import com.cloudinary.android.MediaManager

class VivoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Cloudinary with keys from BuildConfig
        val config = mapOf(
            "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
            "api_key" to BuildConfig.CLOUDINARY_API_KEY,
            "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
        )
        MediaManager.init(this, config)
    }
}
