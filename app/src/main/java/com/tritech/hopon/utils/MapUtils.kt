package com.tritech.hopon.utils

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.tritech.hopon.R
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.roundToInt


object MapUtils {

    private const val TAG = "MapUtils"

    fun getCarBitmap(context: Context): Bitmap {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_car)
        return Bitmap.createScaledBitmap(bitmap, 50, 100, false)
    }

    fun getDestinationBitmap(): Bitmap {
        val height = 20
        val width = 20
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val radius = (minOf(width, height) / 2f)
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)
        return bitmap
    }

    fun getLocationIconBitmap(
        context: Context,
        drawableResId: Int,
        colorResId: Int,
        sizePx: Int = 72
    ): Bitmap {
        val color = ContextCompat.getColor(context, colorResId)

        val width = sizePx
        val height = sizePx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val drawable = ContextCompat.getDrawable(context, drawableResId)
            ?: return getDestinationBitmap()

        // Colorize the vector directly so the pin shape stays intact.
        val padding = (sizePx * 0.0833f).roundToInt().coerceAtLeast(2)
        drawable.mutate().colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        drawable.setBounds(padding, padding, width - padding, height - padding)
        drawable.draw(canvas)

        return bitmap
    }

    fun getRotation(start: LatLng, end: LatLng): Float {
        val latDifference: Double = abs(start.latitude - end.latitude)
        val lngDifference: Double = abs(start.longitude - end.longitude)
        var rotation = -1F
        when {
            start.latitude < end.latitude && start.longitude < end.longitude -> {
                rotation = Math.toDegrees(atan(lngDifference / latDifference)).toFloat()
            }
            start.latitude >= end.latitude && start.longitude < end.longitude -> {
                rotation = (90 - Math.toDegrees(atan(lngDifference / latDifference)) + 90).toFloat()
            }
            start.latitude >= end.latitude && start.longitude >= end.longitude -> {
                rotation = (Math.toDegrees(atan(lngDifference / latDifference)) + 180).toFloat()
            }
            start.latitude < end.latitude && start.longitude >= end.longitude -> {
                rotation =
                    (90 - Math.toDegrees(atan(lngDifference / latDifference)) + 270).toFloat()
            }
        }
        Log.d(TAG, "getRotation: $rotation")
        return rotation
    }
}