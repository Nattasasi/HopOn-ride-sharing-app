package com.tritech.hopon.ui.rideDiscovery.core

import android.os.Build
import com.tritech.hopon.BuildConfig

object ApiBaseUrlResolver {

    fun resolve(): String {
        return if (isProbablyEmulator()) {
            BuildConfig.API_BASE_URL_EMULATOR
        } else {
            BuildConfig.API_BASE_URL_DEVICE
        }
    }

    private fun isProbablyEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val product = Build.PRODUCT.orEmpty()

        return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            model.contains("google_sdk") ||
            model.contains("Emulator") ||
            model.contains("Android SDK built for x86") ||
            manufacturer.contains("Genymotion") ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product == "google_sdk"
    }
}
