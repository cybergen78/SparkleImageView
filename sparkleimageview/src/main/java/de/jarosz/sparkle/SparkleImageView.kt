package de.jarosz.sparkle

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.annotation.CallSuper
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import de.jarosz.sparkle.wave.SinusoidWave
import de.jarosz.sparkle.wave.TriangleWave
import de.jarosz.sparkle.wave.WaveFunction
import kotlin.math.*

private const val TAG = "SparkleImageView"

@Suppress("MemberVisibilityCanBePrivate")
class SparkleImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), SensorEventListener {
    private var sensorThread: HandlerThread? = null
    private var lastSensorValue: Float = 0f
    private var currentSensorValue: Float = 0f
    private lateinit var internalSparkleDrawable: SparkleDrawable
    private var firstX = 0f
    private var firstY = 0f

    /**
     * Convenience property to avoid casting to [SparkleDrawable].
     *
     * @see SparkleDrawable
     */
    val sparkleDrawable: SparkleDrawable
        get() {
            if (!this::internalSparkleDrawable.isInitialized) {
                internalSparkleDrawable = SparkleDrawable(waveFunction).apply {
                    dotScale = resources.displayMetrics.density.roundToInt()
                }
            }

            if (drawable == null) {
                setImageDrawable(internalSparkleDrawable)
            }

            return internalSparkleDrawable
        }
    /**
     * The factor by which the sensor or touch value is multiplied before applying it to the [SparkleDrawable]. (default = 6)
     *
     * @see mode
     */
    var sensitivity = 6f
        set(value) {
            field = value
            if (width > 0 && height > 0) {
                sparkleDrawable.modify(currentSensorValue * value)
            }
        }
    /**
     * Convenience property delegating to [SparkleDrawable.waveFunction]
     *
     * @see SparkleDrawable.waveFunction
     */
    var waveFunction: WaveFunction = TriangleWave()
        set(value) {
            // We need an own field here, because waveFunction is accessed before sparkleDrawable is initialized.
            field = value
            sparkleDrawable.waveFunction = value
        }
    /**
     * The way the modulation of the [sparkleDrawable] is calculated.
     *
     * @see Mode
     */
    var mode: Mode = Mode.MOTION
        set(value) {
            field = value
            when (value) {
                Mode.MOTION -> switchToMotionMode()
                Mode.TOUCH_ANGLE, Mode.TOUCH_DISTANCE -> switchToTouchMode()
            }
        }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.SparkleImageView, defStyleAttr, 0).apply {
            waveFunction = when (getInt(R.styleable.SparkleImageView_waveFunction, 1)) {
                0 -> SinusoidWave()
                1 -> TriangleWave()   // default
                else -> waveFunction
            }

            sensitivity = getFloat(R.styleable.SparkleImageView_sensitivity, sensitivity)
            mode = Mode.values()[getInteger(R.styleable.SparkleImageView_mode, Mode.MOTION.ordinal)]

            recycle()
        }
    }

    /**
     * Sets the color of the [sparkleDrawable].
     *
     * @param color the color to set.
     *
     * @see SparkleDrawable.color
     */
    override fun setBackgroundColor(color: Int) {
        sparkleDrawable.color = color
    }

    /**
     * Returns the color of the [sparkleDrawable].
     *
     * @return the color of the [sparkleDrawable]
     *
     * @see SparkleDrawable.color
     */
    fun getBackgroundColor() = sparkleDrawable.color

    /**
     * Sets the given drawable as the background.
     *
     * As this class works with [SparkleDrawable], only [ColorDrawable]s are supported,
     * where the color of the drawable is set as the color of the [sparkleDrawable].
     *
     * @param background the [ColorDrawable] to take the color from
     *
     * @see sparkleDrawable
     * @see SparkleDrawable.color
     * @see waveFunction
     */
    override fun setBackground(background: Drawable?) {
        if (background !is ColorDrawable) {
            Log.w(TAG, "Only ColorDrawable is supported as background.")
            return
        }

        // waveFunction may not be initialized yet, if the background color is set through XML,
        // because this function is called in the constructor of the super class
        // and the access to sparkleDrawable accesses waveFunction.
        // lateinit is impossible with a custom setter.
        @Suppress("SENSELESS_COMPARISON")
        if (waveFunction == null) {
            waveFunction = TriangleWave()
        }

        sparkleDrawable.color = background.color
    }

    private fun switchToTouchMode() {
        stop()
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                firstX = event.x
                firstY = event.y
                return@setOnTouchListener true
            }

            val sensorValue = touchSensorValue(event)

            if (event.action == MotionEvent.ACTION_MOVE) {
                sparkleDrawable.modify((sensorValue) * sensitivity)
            } else if (event.action == MotionEvent.ACTION_UP) {
                currentSensorValue = sensorValue
            }

            true
        }
    }

    private fun touchSensorValue(event: MotionEvent): Float {
        val deltaX = event.x - firstX
        val deltaY = event.y - firstY
        val delta = sqrt((deltaX * deltaX) + (deltaY * deltaY))

        return when (mode) {
            Mode.TOUCH_ANGLE -> (if (deltaY > 0) {
                acos(deltaX / delta)
            } else {
                2 * PI.toFloat() - acos(deltaX / delta)
            }) % (2 * PI.toFloat()) + currentSensorValue
            Mode.TOUCH_DISTANCE -> (delta / 500f) % (2 * PI.toFloat()) + currentSensorValue
            Mode.MOTION -> 0f
        }
    }

    private fun switchToMotionMode() {
        setOnTouchListener(null)
        onVisibilityChanged(this, visibility)
    }

    /**
     * Calls super and starts or stops the motion detection in [Mode.MOTION] mode according to the new visibility.
     *
     * @see Drawable
     */
    @CallSuper
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (mode == Mode.MOTION) {
            when (visibility) {
                View.VISIBLE -> start()
                View.INVISIBLE, View.GONE -> stop()
            }
        }
    }

    /**
     * Calls super and stops motion detection and all animations.
     *
     * @see Drawable
     */
    @CallSuper
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    /**
     * Starts detecting device motion and the corresponding animation.
     *
     * This is automatically called if the view gets visible.
     */
    fun start() {
        startMotionDetection()
        animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                if (abs(lastSensorValue - currentSensorValue) > 0.001f) {
                    sparkleDrawable.modify(currentSensorValue * sensitivity)
                    lastSensorValue = currentSensorValue
                }
            }
        }.apply {
            repeatCount = Animation.INFINITE
        }
    }

    /**
     * Stops detecting device motion and also the corresponding animation.
     *
     * This is automatically called if the view gets invisible or removed from the view hierarchy.
     */
    fun stop() {
        clearAnimation()
        stopMotionDetection()
    }

    private fun startMotionDetection() {
        sensorThread = HandlerThread("Sensor event loop").also {
            it.start()
            ContextCompat.getSystemService(context, SensorManager::class.java)?.also { sensorManager ->
                sensorManager.registerListener(
                    this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                    SensorManager.SENSOR_DELAY_FASTEST, Handler(it.looper)
                )
            }
        }
    }

    private fun stopMotionDetection() {
        ContextCompat.getSystemService(context, SensorManager::class.java)?.unregisterListener(this)
        sensorThread = sensorThread?.let {
            it.quit()
            it.interrupt()
            null
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            // TODO: maybe there's room for improvement
            currentSensorValue = (it.values[0] * 2 + it.values[1] + it.values[2]) // / 3 * 2 * PI.toFloat()
//            Log.d(TAG, "values: [${"%.3f".format(it.values[0])}/${"%.3f".format(it.values[1])}/${"%.3f".format(it.values[2])}] -> delta: $currentSensorValue")
        }
    }

    enum class Mode {
        /**
         * The animation responds to the device motion.
         */
        MOTION,
        /**
         * The animation responds to touches (swipes) by distance. Helpful for testing or playing.
         */
        TOUCH_DISTANCE,
        /**
         * The animation responds to touches by the angle to the horizontal axis. Helpful for testing or playing.
         */
        TOUCH_ANGLE
        // TODO: add Mode.AUTO which animates automatically, indefinitely
    }
}