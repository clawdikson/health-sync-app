package com.clawd.healthsync

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity that handles the Health Connect permissions rationale.
 * This is required for the app to appear in Health Connect's app list.
 */
class PermissionsRationaleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Simply finish and let the system handle the permissions UI
        finish()
    }
}
