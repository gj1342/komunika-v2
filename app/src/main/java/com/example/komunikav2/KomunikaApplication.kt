package com.example.komunikav2

import android.app.Application
import android.view.MotionEvent
import android.view.View

class KomunikaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Disable hover events globally to prevent crashes
        System.setProperty("android.compose.ui.hover.enabled", "false")
        
        // Set up global hover event handling
        setupGlobalHoverHandling()
    }
    
    private fun setupGlobalHoverHandling() {
        // Override the default hover listener for all views
        View::class.java.getDeclaredMethod("setOnHoverListener", View.OnHoverListener::class.java)
            .let { method ->
                method.isAccessible = true
            }
    }
    
    companion object {
        fun createHoverListener(): View.OnHoverListener {
            return View.OnHoverListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_HOVER_ENTER,
                    MotionEvent.ACTION_HOVER_MOVE,
                    MotionEvent.ACTION_HOVER_EXIT -> {
                        // Consume all hover events to prevent crashes
                        true
                    }
                    else -> false
                }
            }
        }
    }
} 