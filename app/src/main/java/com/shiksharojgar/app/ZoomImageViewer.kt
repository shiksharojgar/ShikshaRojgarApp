
package com.shiksharojgar.app

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import android.view.ScaleGestureDetector

class ZoomableImageView(context: Context) : AppCompatImageView(context) {

    private val imageMatrixValues = Matrix()
    private val savedMatrix = Matrix()
    private val startPoint = PointF()
    private var dragging = false
    private var fitScale = 1f

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                imageMatrixValues.postScale(
                    detector.scaleFactor,
                    detector.scaleFactor,
                    detector.focusX,
                    detector.focusY
                )
                imageMatrix = imageMatrixValues
                return true
            }
        }
    )

    init {
        scaleType = ScaleType.MATRIX
        setBackgroundColor(Color.BLACK)
        imageMatrix = imageMatrixValues
        setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(imageMatrixValues)
                    startPoint.set(event.x, event.y)
                    dragging = true
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    savedMatrix.set(imageMatrixValues)
                    dragging = false
                }

                MotionEvent.ACTION_MOVE -> {
                    if (dragging && !scaleDetector.isInProgress &&
                        event.pointerCount == 1
                    ) {
                        imageMatrixValues.set(savedMatrix)
                        imageMatrixValues.postTranslate(
                            event.x - startPoint.x,
                            event.y - startPoint.y
                        )
                        imageMatrix = imageMatrixValues
                    }
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    savedMatrix.set(imageMatrixValues)
                    if (event.pointerCount > 2) {
                        startPoint.set(event.getX(0), event.getY(0))
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false
                    performClick()
                }
            }
            true
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun setImageBitmap(bitmap: Bitmap?) {
        super.setImageBitmap(bitmap)
        post {
            if (bitmap != null && width > 0 && height > 0) {
                val scaleX = width.toFloat() / bitmap.width
                val scaleY = height.toFloat() / bitmap.height
                fitScale = minOf(scaleX, scaleY)
                imageMatrixValues.reset()
                imageMatrixValues.postScale(fitScale, fitScale)
                imageMatrixValues.postTranslate(
                    (width - bitmap.width * fitScale) / 2f,
                    (height - bitmap.height * fitScale) / 2f
                )
                imageMatrix = imageMatrixValues
            }
        }
    }
}

object ZoomImageViewer {
    fun show(context: Context, bitmap: Bitmap) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val image = ZoomableImageView(context).apply {
            setImageBitmap(bitmap)
            contentDescription = "ज़ूम करने योग्य फोटो"
        }

        val close = android.widget.TextView(context).apply {
            text = "✕"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(18, 12, 18, 12)
            setOnClickListener { dialog.dismiss() }
        }

        val container = FrameLayout(context).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                image,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            addView(
                close,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.END
                )
            )
        }

        dialog.setContentView(container)
        dialog.setOnKeyListener { _, keyCode, event ->
    if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
        if (event.action == android.view.KeyEvent.ACTION_UP &&
            !event.isCanceled
        ) {
            dialog.dismiss()
        }
        true
    } else {
        false
    }
}
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
        }
    }
}
