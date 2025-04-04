package com.demo.hiifi

import android.content.Context
import com.cloudinary.android.MediaManager

object CloudinaryManager {
    private var initialized = false

    fun init(context: Context) {
        if (!initialized) {
            val config = mapOf(
                "cloud_name" to "dxmectx5n",
                "api_key" to "337668532814689",
                "api_secret" to "OXmu1itck-kxGawtJdZS_cIURag"
            )
            MediaManager.init(context, config)
            initialized = true
        }
    }
}
