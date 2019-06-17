package de.jarosz.sparkle.sample

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.madrapps.pikolo.listeners.SimpleColorSelectionListener
import de.jarosz.sparkle.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty

class MainActivity : AppCompatActivity() {
    private var colorMenuItem: MenuItem? = null
    private var settingsMenuItem: MenuItem? = null
    private var applyingMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        initializeControls()
        colorPicker.visibility = View.INVISIBLE
        colorPicker.alpha = 0f
        scrollView.visibility = View.INVISIBLE
        scrollView.alpha = 0f
    }

    private fun initializeControls() {
        colorPicker.apply {
            setColor(imageView.getBackgroundColor())
            setColorSelectionListener(object : SimpleColorSelectionListener() {
                override fun onColorSelected(color: Int) {
                    val hsl = floatArrayOf(0f, 0f, 0f)
                    ColorUtils.colorToHSL(color, hsl)
                    if (hsl[2] > 0f && hsl[2] < 1f) {
                        updateButtonZeroAxis(color)
                        updateLightnessVarianceSeekBarMaxValues(color)
                    }
                }

                override fun onColorSelectionEnd(color: Int) {
                    val hsl = floatArrayOf(0f, 0f, 0f)
                    ColorUtils.colorToHSL(color, hsl)
                    hsl[2] = hsl[2] min 0.01f max 0.99f
                    val newColor = ColorUtils.HSLToColor(hsl)
                    if (imageView.getBackgroundColor() != newColor) {
                        imageView.setBackgroundColor(color)
                        updateButtonZeroAxis(color)
                        updateLightnessVarianceSeekBarMaxValues(color)
                    }
                }
            })
        }
        updateLightnessVarianceSeekBarMaxValues(imageView.getBackgroundColor())

        lightnessVarianceDownSeekBar.apply {
            progress = (imageView.sparkleDrawable.lightnessVarianceDown * 100).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val lightnessVarianceDown = (progress min 1 max 99) / 100f
                    lightnessVarianceDownTextView.text = (lightnessVarianceDown * 100).format("%.0f %%")
                    updateButtonAmplitudes(
                        imageView.getBackgroundColor(),
                        lightnessVarianceUpSeekBar.progress / 100f,
                        lightnessVarianceDown
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (progress < 1) {
                        progress = 1
                    }

                    val lightnessVarianceDown = (progress min 1 max 99) / 100f
                    if (imageView.sparkleDrawable.lightnessVarianceDown != lightnessVarianceDown) {
                        imageView.sparkleDrawable.lightnessVarianceDown = lightnessVarianceDown
                        lightnessVarianceDownTextView.text = (lightnessVarianceDown * 100).format("%.0f %%")
                        updateButtonAmplitudes(
                            imageView.getBackgroundColor(),
                            lightnessVarianceUpSeekBar.progress / 100f,
                            lightnessVarianceDown
                        )
                    }
                }
            })
        }

        lightnessVarianceUpSeekBar.apply {
            progress = (imageView.sparkleDrawable.lightnessVarianceUp * 100).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val lightnessVarianceUp = (progress min 1 max 99) / 100f
                    lightnessVarianceUpTextView.text = (lightnessVarianceUp * 100).format("%.0f %%")
                    updateButtonAmplitudes(
                        imageView.getBackgroundColor(),
                        lightnessVarianceUp,
                        lightnessVarianceDownSeekBar.progress / 100f
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (progress < 1) {
                        progress = 1
                    }

                    val lightnessVarianceUp = (progress min 1 max 99) / 100f
                    if (imageView.sparkleDrawable.lightnessVarianceUp != lightnessVarianceUp) {
                        imageView.sparkleDrawable.lightnessVarianceUp = lightnessVarianceUp
                        lightnessVarianceUpTextView.text = (lightnessVarianceUp * 100).format("%.0f %%")
                        updateButtonAmplitudes(
                            imageView.getBackgroundColor(),
                            lightnessVarianceUp,
                            lightnessVarianceDownSeekBar.progress / 100f
                        )
                    }
                }
            })
        }

        waveSpeedVarianceSeekBar.apply {
            progress = (imageView.sparkleDrawable.waveSpeedVariance * 10).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    imageView.sparkleDrawable.waveSpeedVariance = progress / 10f
                    waveSpeedVarianceTextView.text = imageView.sparkleDrawable.waveSpeedVariance.format("%.1f")
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }

        val densityInt = resources.displayMetrics.density.roundToInt()
        scaleSeekBar.apply {
            progress = imageView.sparkleDrawable.dotScale / densityInt
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (progress > 0 && imageView.sparkleDrawable.dotScale != densityInt * progress) {
                        imageView.sparkleDrawable.dotScale = densityInt * progress
                        @SuppressLint("SetTextI18n")
                        scaleTextView.text = "%dx".format(imageView.sparkleDrawable.dotScale / densityInt)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (progress == 0) {
                        progress = 1
                    }
                }
            })
        }

        triangleButton.apply {
            isChecked = imageView.waveFunction.javaClass == waveFunction.javaClass
            setOnCheckedChangeListener { _, _ ->
                applyWaveFunction(this)
            }
        }
        sinusoidButton.apply {
            isChecked = imageView.waveFunction.javaClass == waveFunction.javaClass
            setOnCheckedChangeListener { _, _ ->
                applyWaveFunction(this)
            }
        }

        sensitivitySeekBar.apply {
            progress = (imageView.sensitivity * 10).roundToInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (progress > 0 && imageView.sensitivity != progress / 10f) {
                        imageView.sensitivity = progress / 10f
                        sensitivityTextView.text = imageView.sensitivity.format("%.1f")
                        updateButtonPeriods(imageView.sensitivity)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (progress == 0) {
                        progress = 1
                    }
                }
            })
        }

        modeDeviceMotion.apply {
            isChecked = imageView.mode == SparkleImageView.Mode.MOTION
            setOnCheckedChangeListener { button, _ ->
                applyMode(button, SparkleImageView.Mode.MOTION)
            }
        }
        modeTouchDistance.apply {
            isChecked = imageView.mode == SparkleImageView.Mode.TOUCH_DISTANCE
            setOnCheckedChangeListener { button, _ ->
                applyMode(button, SparkleImageView.Mode.TOUCH_DISTANCE)
            }
        }
        modeTouchAngle.apply {
            isChecked = imageView.mode == SparkleImageView.Mode.TOUCH_ANGLE
            setOnCheckedChangeListener { button, _ ->
                applyMode(button, SparkleImageView.Mode.TOUCH_ANGLE)
            }
        }

        initializeHighlightsSeekBar(
            highlightsLightnessSeekBar, imageView.sparkleDrawable::highlightsLightnessFactor, 0,
            100f, highlightsLightnessTextView, 1f, "%.0f %%"
        )
        initializeHighlightsSeekBar(
            highlightsAmountSeekBar, imageView.sparkleDrawable::highlightsAmount, 0,
            1000f, highlightsAmountTextView, 0.1f, "%.1f %%"
        )
        initializeHighlightsSeekBar(
            highlightsVariabilitySeekBar, imageView.sparkleDrawable::highlightsVariability, 0,
            100f, highlightsVariabilityTextView, 1f, "%.0f %%"
        )
        initializeHighlightsSeekBar(
            highlightsReplacementSpeedFactorSeekBar, imageView.sparkleDrawable::highlightsReplacementSpeedFactor, 1,
            10f, highlightsReplacementSpeedFactorTextView, 0.1f, "%.1f"
        )
        initializeHighlightsSeekBar(
            highlightsSensitivitySeekBar, imageView.sparkleDrawable::highlightsSensitivity, 1,
            10f, highlightsSensitivityTextView, 0.1f, "%.1f"
        )

        retroModeSwitch.apply {
            isChecked = imageView.sparkleDrawable.retroMode
            setOnCheckedChangeListener { _, isChecked ->
                imageView.sparkleDrawable.retroMode = isChecked
            }
        }

        updateValues()
        updateButtonZeroAxis(imageView.getBackgroundColor())
        updateButtonAmplitudes(
            imageView.getBackgroundColor(),
            imageView.sparkleDrawable.lightnessVarianceUp,
            imageView.sparkleDrawable.lightnessVarianceDown
        )
    }

    private fun initializeHighlightsSeekBar(
        seekBar: SeekBar,
        sparkleDrawableProperty: KMutableProperty<Float>,
        minProgress: Int,
        factor: Float,
        textView: TextView,
        textValueFactor: Float,
        format: String
    ) = seekBar.apply {
        progress = (sparkleDrawableProperty.call() * factor).roundToInt()
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress >= minProgress && sparkleDrawableProperty.call() != progress / factor) {
                    sparkleDrawableProperty.setter.call(progress / factor)
                    textView.text = (sparkleDrawableProperty.call() * factor * textValueFactor).format(format)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) = seekBar?.run {
                if (progress < minProgress) {
                    progress = minProgress
                }
                Unit
            } ?: Unit
        })
    }

    private fun updateLightnessVarianceSeekBarMaxValues(@ColorInt color: Int) {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.colorToHSL(color, hsl)
        lightnessVarianceDownSeekBar.max = (hsl[2] * 100).roundToInt()
        lightnessVarianceUpSeekBar.max = 100 - (hsl[2] * 100).roundToInt()
        updateButtonAmplitudes(
            color,
            lightnessVarianceUpSeekBar.progress / 100f,
            lightnessVarianceDownSeekBar.progress / 100f
        )
    }

    private fun applyMode(modeButton: CompoundButton, mode: SparkleImageView.Mode) {
        if (applyingMode) {
            return
        }

        if (modeButton.isChecked) {
            applyingMode = true
            listOf(modeDeviceMotion, modeTouchDistance, modeTouchAngle).forEach {
                if (it != modeButton) {
                    it.isChecked = false
                }
            }
            imageView.mode = mode
            applyingMode = false
        } else if (!modeButton.isChecked) {
            modeButton.isChecked = true
        }
    }

    private fun applyWaveFunction(functionButton: FunctionButton) {
        if (functionButton.isChecked) {
            imageView.waveFunction = functionButton.waveFunction
            listOf(triangleButton, sinusoidButton).forEach {
                if (it != functionButton) {
                    it.isChecked = false
                }
            }
            imageView.waveFunction = functionButton.waveFunction
        }
    }

    private fun updateButtonZeroAxis(@ColorInt color: Int) {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.colorToHSL(color, hsl)
        triangleButton.zeroAxisVerticalBias = hsl[2]
        sinusoidButton.zeroAxisVerticalBias = hsl[2]
    }

    private fun updateButtonAmplitudes(@ColorInt color: Int, lightnessVarianceUp: Float, lightnessVarianceDown: Float) {
        val hsl = floatArrayOf(0f, 0f, 0f)
        ColorUtils.colorToHSL(color, hsl)
        val amplitude = lightnessVarianceUp + lightnessVarianceDown
        triangleButton.waveAmplitude = amplitude
        sinusoidButton.waveAmplitude = amplitude
        triangleButton.waveOffsetY = hsl[2] - lightnessVarianceDown + amplitude / 2
        sinusoidButton.waveOffsetY = hsl[2] - lightnessVarianceDown + amplitude / 2
    }

    private fun updateValues() {
        val densityInt = resources.displayMetrics.density.roundToInt()
        @SuppressLint("SetTextI18n")
        scaleTextView.text = "%dx".format(imageView.sparkleDrawable.dotScale / densityInt)
        lightnessVarianceDownTextView.text = (imageView.sparkleDrawable.lightnessVarianceDown * 100).format("%.0f %%")
        lightnessVarianceUpTextView.text = (imageView.sparkleDrawable.lightnessVarianceUp * 100).format("%.0f %%")
        waveSpeedVarianceTextView.text = imageView.sparkleDrawable.waveSpeedVariance.format("%.1f")
        sensitivityTextView.text = imageView.sensitivity.format("%.1f")
        highlightsLightnessTextView.text = (imageView.sparkleDrawable.highlightsLightnessFactor * 100).format("%.0f %%")
        highlightsAmountTextView.text = (imageView.sparkleDrawable.highlightsAmount * 100).format("%.1f %%")
        highlightsVariabilityTextView.text = (imageView.sparkleDrawable.highlightsVariability * 100).format("%.0f %%")
        highlightsReplacementSpeedFactorTextView.text =
            (imageView.sparkleDrawable.highlightsReplacementSpeedFactor).format("%.1f")
        highlightsSensitivityTextView.text = (imageView.sparkleDrawable.highlightsSensitivity).format("%.1f")
        updateButtonPeriods(imageView.sensitivity)
    }

    private fun updateButtonPeriods(sensitivity: Float) {
        triangleButton.wavePeriods = sensitivity
        sinusoidButton.wavePeriods = sensitivity
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        colorMenuItem = menu?.findItem(R.id.color)?.apply {
            setOnMenuItemClickListener {
                toggleMenuItem(it, settingsMenuItem)
                false
            }
        }
        settingsMenuItem = menu?.findItem(R.id.settings)?.apply {
            setOnMenuItemClickListener {
                toggleMenuItem(it, colorMenuItem)
                false
            }
        }
        return true
    }

    private fun toggleMenuItem(menuItem: MenuItem, otherMenuItem: MenuItem?) {
        menuItem.isChecked = !menuItem.isChecked
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            menuItem.iconTintList =
                ColorStateList.valueOf(Color.argb(if (menuItem.isChecked) 127 else 255, 255, 255, 255))
        }
        otherMenuItem?.let {
            it.isChecked = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.reset -> imageView.sparkleDrawable.reset()
            R.id.color -> {
                toggleColorPickerVisibility(item.isChecked)
                toggleSettingsVisibility(false)
                settingsMenuItem?.isChecked = false
            }
            R.id.settings -> {
                toggleSettingsVisibility(item.isChecked)
                toggleColorPickerVisibility(false)
                colorMenuItem?.isChecked = false
            }
            else -> return false
        }

        return true
    }

    private fun toggleColorPickerVisibility(visible: Boolean) {
        colorPicker.animate().setDuration(300).alpha(if (visible) 1f else 0f)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    if (!visible) {
                        colorPicker.visibility = View.INVISIBLE
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                    if (visible) {
                        colorPicker.visibility = View.VISIBLE
                    }
                }
            })
    }

    private fun toggleSettingsVisibility(visible: Boolean) {
        scrollView.animate().setDuration(300).alpha(if (visible) 1f else 0f)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    if (!visible) {
                        scrollView.visibility = View.INVISIBLE
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                    if (visible) {
                        scrollView.visibility = View.VISIBLE
                    }
                }
            })
    }
}
