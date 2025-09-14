package com.example.komunikav2.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var handLandmarkerResult: HandLandmarkerResult? = null

    private val circlePaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 10f
        style = Paint.Style.FILL
    }

    private val linePaint = Paint().apply {
        color = Color.RED
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    // Define connections between landmarks as pairs of indices
    private val connections = listOf(
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),  // Thumb
        Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),  // Index finger
        Pair(5, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),  // Middle finger
        Pair(9, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),  // Ring finger
        Pair(13, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20),  // Pinky finger
        Pair(0, 17) // Palm base to pinky
    )

    /**
     * Updates the overlay with new detection results and ensures the view is visible.
     */
    fun setResults(results: HandLandmarkerResult) {
        handLandmarkerResult = results
        visibility = View.VISIBLE
        invalidate()
    }

    /**
     * Clears the overlay by removing any stored detection results,
     * clearing the canvas, and setting the view visibility to GONE.
     */
    fun clear() {
        handLandmarkerResult = null
        visibility = View.GONE
        invalidate() // Request a redraw to clear the canvas
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // If no hand landmarks are available, clear the canvas completely.
        if (handLandmarkerResult == null || handLandmarkerResult?.landmarks()?.isEmpty() == true) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            return
        }

        // Draw landmarks and connections if available.
        handLandmarkerResult?.landmarks()?.forEach { handLandmarks ->
            val points = handLandmarks.map { landmark ->
                Pair(landmark.x() * width, landmark.y() * height)
            }

            // Draw connections between keypoints.
            for (connection in connections) {
                if (connection.first < points.size && connection.second < points.size) {
                    val start = points[connection.first]
                    val end = points[connection.second]
                    canvas.drawLine(start.first, start.second, end.first, end.second, linePaint)
                }
            }

            // Draw circles for each landmark.
            points.forEach { (x, y) ->
                canvas.drawCircle(x, y, 8f, circlePaint)
            }
        }
    }
} 