package com.example.komunikav2.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.komunikav2.services.PermissionManager

object CompatibilityHelper {
    
    private const val TAG = "CompatibilityHelper"
    
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            brand = Build.BRAND,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            isEmulator = isEmulator()
        )
    }
    
    fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                "google_sdk" == Build.PRODUCT
    }
    
    fun getCompatibilityStatus(context: Context): CompatibilityStatus {
        val deviceInfo = getDeviceInfo()
        val permissionManager = PermissionManager(context)
        val permissionState = permissionManager.checkAllPermissions()
        
        val isSupported = deviceInfo.sdkInt >= 24
        val hasRequiredPermissions = permissionState.allRequiredGranted
        val isFullyCompatible = isSupported && hasRequiredPermissions
        
        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        
        when {
            deviceInfo.sdkInt < 24 -> {
                warnings.add("Android version ${deviceInfo.androidVersion} is not supported. Minimum required: Android 7.0")
            }
            deviceInfo.sdkInt < 26 -> {
                recommendations.add("Android 8.0+ recommended for optimal performance")
            }
            deviceInfo.sdkInt >= 33 -> {
                recommendations.add("Android 13+ detected - Nearby WiFi permission may be required")
            }
        }
        
        when (deviceInfo.brand.lowercase()) {
            "xiaomi", "redmi" -> {
                recommendations.add("MIUI detected - Check app permissions in Settings > Apps > Permissions")
            }
            "oppo", "oneplus", "realme" -> {
                recommendations.add("ColorOS detected - Ensure all permissions are granted in Settings")
            }
            "samsung" -> {
                recommendations.add("Samsung device detected - Check if Power Saving mode is disabled")
            }
            "huawei", "honor" -> {
                recommendations.add("EMUI detected - Check app permissions and battery optimization settings")
            }
        }
        
        if (!permissionState.bluetoothGranted) {
            warnings.add("Bluetooth permissions not granted")
        }
        
        if (!permissionState.locationGranted) {
            warnings.add("Location permissions not granted")
        }
        
        if (deviceInfo.sdkInt >= 33 && !permissionState.nearbyWifiGranted) {
            warnings.add("Nearby WiFi permission not granted (Android 13+)")
        }
        
        return CompatibilityStatus(
            deviceInfo = deviceInfo,
            isSupported = isSupported,
            hasRequiredPermissions = hasRequiredPermissions,
            isFullyCompatible = isFullyCompatible,
            warnings = warnings,
            recommendations = recommendations
        )
    }
    
    fun logCompatibilityInfo(context: Context) {
        val status = getCompatibilityStatus(context)
        
        Log.d(TAG, "=== Device Compatibility Report ===")
        Log.d(TAG, "Brand: ${status.deviceInfo.brand}")
        Log.d(TAG, "Model: ${status.deviceInfo.model}")
        Log.d(TAG, "Android: ${status.deviceInfo.androidVersion} (API ${status.deviceInfo.sdkInt})")
        Log.d(TAG, "Is Emulator: ${status.deviceInfo.isEmulator}")
        Log.d(TAG, "Supported: ${status.isSupported}")
        Log.d(TAG, "Has Permissions: ${status.hasRequiredPermissions}")
        Log.d(TAG, "Fully Compatible: ${status.isFullyCompatible}")
        
        if (status.warnings.isNotEmpty()) {
            Log.w(TAG, "Warnings:")
            status.warnings.forEach { Log.w(TAG, "  - $it") }
        }
        
        if (status.recommendations.isNotEmpty()) {
            Log.i(TAG, "Recommendations:")
            status.recommendations.forEach { Log.i(TAG, "  - $it") }
        }
        
        Log.d(TAG, "=== End Compatibility Report ===")
    }
    
    data class DeviceInfo(
        val brand: String,
        val manufacturer: String,
        val model: String,
        val androidVersion: String,
        val sdkInt: Int,
        val isEmulator: Boolean
    )
    
    data class CompatibilityStatus(
        val deviceInfo: DeviceInfo,
        val isSupported: Boolean,
        val hasRequiredPermissions: Boolean,
        val isFullyCompatible: Boolean,
        val warnings: List<String>,
        val recommendations: List<String>
    )
}
