package de.jarosz.sparkle

import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.SparseIntArray
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import de.jarosz.sparkle.wave.WaveFunction
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private const val TAG = "SparkleDrawable"

class SparkleDrawable(function: WaveFunction) : Drawable() {
    private val paint = Paint()
    private lateinit var originalLightnessArray: FloatArray
    private lateinit var modifiedPixelArray: IntArray
    private lateinit var directionModifierArray: FloatArray
    private lateinit var randomHighlightsArray: SparseIntArray
    private var lastModOdd = false
    private val hslComponents = floatArrayOf(0f, 0f, 0f)
    private var originalLightness = 0f
    private var minLightness = 0f
    private var maxLightness = 1f
    private var lightnessDelta = 1f
    private val precision = 10000
    private val random = Random(System.nanoTime())
    private var bitmap: Bitmap? = null
        get() {
            if (field == null) {
                field = Bitmap.createBitmap(
                    bounds.width() / dotScale,
                    bounds.height() / dotScale,
                    Bitmap.Config.ARGB_8888
                )
            }

            return field
        }
    private var lastMod = 0f

    /**
     * Retro mode corresponds to the [Paint.isFilterBitmap] flag.
     * If the Bitmap is filtered, the pixel edges are sharp on higher [dotScale]s.
     */
    var retroMode
        get() = !paint.isFilterBitmap
        set(value) {
            paint.isFilterBitmap = !value
            invalidateSelf()
        }
    /**
     * The wave function for the lightness calculations.
     */
    var waveFunction: WaveFunction = function
        set(value) {
            field = value
            invalidateMod()
        }
    /**
     * The base color.
     */
    @ColorInt
    var color: Int = Color.GRAY
        set(value) {
            field = value
            paint.alpha = Color.alpha(value)
            ColorUtils.colorToHSL(value, hslComponents)
            originalLightness = hslComponents[2]
            updateLightnessRange()
            createLightnessArray()
            reset()
        }
    /**
     * The lightnessVarianceUp (0 < lightnessVarianceUp < 1, default = 0.15) means how much lighter (absolute) a dot can get at maximum lightness.
     * This results in a maximum lightness (< 1) for further calculations.
     *
     * @throws IllegalArgumentException if both [lightnessVarianceUp] and [lightnessVarianceDown] are 0.
     */
    var lightnessVarianceUp = 0.15f
        set(value) {
            field = value min 0f max 1f
            if (value == 0f && lightnessVarianceDown == 0f) {
                throw IllegalArgumentException(
                    "Lightness delta of 0.0 is not allowed. " +
                            "Choose other values for lightnessVarianceUp and lightnessVarianceDown."
                )
            }
            updateLightnessRange()
            createLightnessArray()
            reset()
        }
    /**
     * The lightnessVarianceDown (0 < lightnessVarianceDown < 1, default = 0.15) means how much darker (absolute) a dot can get at minimum lightness.
     * This results in a minimum lightness (> 0) for further calculations.
     *
     * @throws IllegalArgumentException if both [lightnessVarianceUp] and [lightnessVarianceDown] are 0.
     */
    var lightnessVarianceDown = 0.15f
        set(value) {
            field = value min 0f max 1f
            if (value == 0f && lightnessVarianceUp == 0f) {
                throw IllegalArgumentException(
                    "Lightness delta of 0.0 is not allowed. " +
                            "Choose other values for lightnessVarianceUp and lightnessVarianceDown."
                )
            }
            updateLightnessRange()
            createLightnessArray()
            reset()
        }
    /**
     * The scale of the individual dots (pixels).
     *
     * On high resolution displays the performance wouldn't be so great when processing each individual pixel.
     * Therefore the minimum dotScale is set to 2 (default). The recommended minimum dotScale is
     *
     *     resources.displayMetrics.density.roundToInt()
     */
    var dotScale = 2
        set(value) {
            field = value min 2 max 10000
            createLightnessArray()
            reset()
        }
    /**
     * The waveSpeedVariance (0 < waveSpeedVariance < 10, default = 1) is a factor by witch the wave speeds may differ between individual dots.
     * It's added from 0 to waveSpeedVariance times the default wave speed (= 1) randomly to the default wave speed.
     *
     * individualWaveSpeed = 1 + (random 0 to waveSpeedVariance)
     *
     * So a factor of 1 means that the fastest wave is twice as fast as the default wave speed (which is the slowest).
     * The waves may travel in positive or negative direction.
     * For the former example this results in individual random wave speeds from -2 to +2.
     *
     * The minimum value is 0 and the maximum value is 10.
     */
    var waveSpeedVariance = 1f
        set(value) {
            field = value min 0f max 10f
            createRandomDirectionModifierArray()
            invalidateMod()
        }
    /**
     * The randomly distributed highlights lightness will be calculated as follows.
     *
     * The delta from the calculated maximum lightness to 1 will be added to the maximum lightness
     * by the highlightsLightnessFactor (0 < highlightsLightnessFactor < 1, default: 0.9).
     *
     * So if the calculated maximum lightness is 0.5 (e.g. original color lightness is 0.4 and [lightnessVarianceUp] is 0.1)
     * and the highlightsLightnessFactor is 0.6, the highlight maximum lightness will be 0.8 (0.4 + 0.1 + ((1 - (0.4 + 0.1)) * 0.6) = 0.8).
     *
     * @see lightnessVarianceDown
     * @see lightnessVarianceUp
     */
    var highlightsLightnessFactor = 0.9f
        set(value) {
            field = value min 0f max 1f
            invalidateMod()
        }
    /**
     * The amount of pixels which will be used for random highlights (0 < highlightsAmount < 0.1 = 10%, default = 0.005 = 0.5%).
     *
     * So if the whole bitmap consists of 1000 dots (e.g. 500 x 500) and the highlightsAmount is 0.005,
     * the resulting randomly highlighted amount of dots is 5.
     *
     * @see dotScale
     */
    var highlightsAmount: Float = 0.005f
        set(value) {
            field = value min 0f max 0.1f
            createRandomHighlightsArray()
            invalidateMod()
        }
    /**
     * Depending on [highlightsReplacementSpeedFactor] this amount of the highlights will be replaced randomly at key frames (0 < highlightsVariability < 1, default = 0.2).
     *
     * @see highlightsReplacementSpeedFactor
     */
    var highlightsVariability: Float = 0.2f
        set(value) {
            field = value min 0f max 1f
        }
    /**
     * The factor by which the modulator (mod parameter of [modify]) is multiplied before calculating new highlight dots according to the [highlightsVariability].
     *
     * E.g. a factor of 1 will result in a recalculation of the highlighted dots every time the modulator reaches a new integer. A factor of 0.5 will result in half this speed.
     *
     * (0.1 < highlightsReplacementSpeedFactor < 10, default = 0.5)
     *
     * @see highlightsVariability
     * @see modify
     */
    var highlightsReplacementSpeedFactor: Float = 0.5f
        set(value) {
            field = value min 0.1f max 10f
            invalidateMod()
        }
    /**
     * The factor by which the modulator is multiplied for calculating the lightness of a highlighted dot.
     * This results in a wave speed which is [highlightsSensitivity] times the regular wave speed of a non highlighted dot (1).
     *
     * (0.1 < highlightsSensitivity < 10, default = 0.5)
     *
     * @see highlightsReplacementSpeedFactor
     * @see modify
     */
    var highlightsSensitivity: Float = 1.5f
        set(value) {
            field = value min 0.1f max 10f
            invalidateMod()
        }

    init {
        // Set the initial color in the init block to trigger recalculation of related variables
        // (needed if no other color is set and the default is used).
        color = Color.GRAY
        retroMode = true   // looks cooler 8)
    }

    private fun invalidateMod() = if (bounds.width() > 0 && bounds.height() > 0) modify(lastMod) else Unit

    private fun updateLightnessRange() {
        minLightness = max(0.001f, originalLightness - lightnessVarianceDown)
        maxLightness = min(0.999f, originalLightness + lightnessVarianceUp)
        lightnessDelta = maxLightness - minLightness
    }

    override fun draw(canvas: Canvas) {
        bitmap?.let {
            canvas.drawBitmap(it, null, bounds, paint)
        }
    }

    override fun onBoundsChange(bounds: Rect?) {
        super.onBoundsChange(bounds)

        createLightnessArray()
        reset()
    }

    private fun createLightnessArray() {
        if (bounds.width() == 0 || bounds.height() == 0) {
            return
        }

        val hslComponentsTmp = floatArrayOf(0f, 0f, 0f)
        ColorUtils.colorToHSL(color, hslComponentsTmp)

        originalLightnessArray =
            floatArrayOf().plus((0 until (bounds.width() / dotScale) * (bounds.height() / dotScale)).map {
                ((random.nextInt((lightnessDelta * precision).roundToInt()).toFloat()) / precision.toFloat()) + minLightness
            })
    }

    /**
     * Recalculates the modified Bitmap, recreates random variables and highlights and redraws this drawable.
     */
    fun reset() {
        bitmap = null
        createModifiedPixelArray()
        createRandomDirectionModifierArray()
        createRandomHighlightsArray()
        // first draw
        if (bounds.width() > 0 && bounds.height() > 0) {
            invalidateMod()
        }
    }

    private fun createModifiedPixelArray() {
        if (bounds.width() == 0 || bounds.height() == 0) {
            return
        }

        val hslComponentsTmp = floatArrayOf(0f, 0f, 0f)
        ColorUtils.colorToHSL(color, hslComponentsTmp)

        // make a deep copy
        modifiedPixelArray = intArrayOf().plus((0 until originalLightnessArray.size).map {
            hslComponentsTmp[2] = originalLightnessArray[it]
            ColorUtils.HSLToColor(hslComponentsTmp)
        })
    }

    private fun createRandomDirectionModifierArray() {
        if (bounds.width() == 0 || bounds.height() == 0) {
            return
        }

        // create random direction modifier map
        var minRandom = 0.0f
        var maxRandom = 0.0f

        directionModifierArray = floatArrayOf().plus((0 until originalLightnessArray.size).map {
            // POTENTIAL: we could split the array into two arrays (direction array and modifier array).
            // This would allow to calculate the direction with a dynamic waveSpeedVariance
            // without recalculating the directionModifierArray (or the two new ones).
            // But this would definitely affect memory usage by one more bitmap(size)
            // and could also affect the performance, because the calculation in modify() would be more heavy.
            // Think about it...
            val direction =
                (if (random.nextBoolean()) 1f else -1f) + ((random.nextFloat() - 0.5f) * 2 * waveSpeedVariance)
            minRandom = min(direction, minRandom)
            maxRandom = max(direction, maxRandom)
            direction
        })

        invalidateSelf()
    }

    private fun createRandomHighlightsArray() {
        if (bounds.width() == 0 || bounds.height() == 0) {
            return
        }

        val pixelCount = directionModifierArray.size
        val highlightsCount = (pixelCount * highlightsAmount).toInt()
        randomHighlightsArray = SparseIntArray(highlightsCount)
        (0 until highlightsCount).forEach { _ ->
            randomHighlightsArray.put(random.nextInt(pixelCount), 1)
        }
    }

    private fun updateRandomHighlightsArrayIfNeeded(mod: Float) {
        val modOdd = (mod * highlightsReplacementSpeedFactor).toInt() % 2 == 1
        if (modOdd != lastModOdd && highlightsAmount > 0) {
            // remove some highlights
            val modifyCount = (randomHighlightsArray.size() * highlightsVariability).roundToInt() min 1
            (0 until modifyCount).forEach { _ ->
                randomHighlightsArray.removeAt(random.nextInt(randomHighlightsArray.size()))
            }

            // add the same amount of highlights
            val pixelCount = directionModifierArray.size
            (0 until modifyCount).forEach { _ ->
                var randomPixel = random.nextInt(pixelCount)
                while (randomHighlightsArray.indexOfKey(randomPixel) > 0) {
                    randomPixel = random.nextInt(pixelCount)
                }
                randomHighlightsArray.put(randomPixel, 1)
            }
            lastModOdd = modOdd
        }
    }

    /**
     * Sets the current modifier and redraws this drawable.
     *
     * @param mod the modifier to set
     *
     * @see minLightness
     * @see maxLightness
     * @see highlightsLightnessFactor
     * @see highlightsSensitivity
     * @see dotScale
     */
    fun modify(mod: Float) {
        try {
            updateRandomHighlightsArrayIfNeeded(mod)

            // Use the original lightness, so the modifications are revertible (except the random components).
            originalLightnessArray.forEachIndexed { index, lightness ->
                val isHighlighted = randomHighlightsArray[index] == 1
                val lightnessDeltaWithHighlights = if (isHighlighted) {
                    maxLightness + ((1 - maxLightness) * highlightsLightnessFactor) - minLightness
                } else {
                    lightnessDelta
                }
                val sensitivity = if (isHighlighted) highlightsSensitivity else 1f
                // Modify highlighted dots randomly to get highlights even for mod == 0.
                val highlightedLightness = if (isHighlighted) {
                    (lightness * abs(directionModifierArray[index])) % lightnessDeltaWithHighlights + minLightness
                } else lightness
                hslComponents[2] = getLightness(
                    highlightedLightness,
                    mod * sensitivity,
                    directionModifierArray[index],
                    minLightness,
                    lightnessDeltaWithHighlights
                )
                modifiedPixelArray[index] = ColorUtils.HSLToColor(hslComponents)
            }

            bitmap?.setPixels(
                modifiedPixelArray, 0, bounds.width() / dotScale, 0, 0,
                bounds.width() / dotScale, bounds.height() / dotScale
            )
            invalidateSelf()
        } catch (e: Exception) {
            Log.e(
                TAG, "An error occurred while modulating the SparkleDrawable.", e
            )
        }
        lastMod = mod
    }

    private fun getLightness(
        originalLightness: Float,
        mod: Float,
        directionModifier: Float,
        minLightness: Float,
        lightnessDelta: Float
    ): Float {
        val randomDirection = (directionModifier / abs(directionModifier)).roundToInt()
        // min and max are special workarounds for a kotlin bug:
        // e.g.: 0.4 - (0.2 / 2 + 0.4) / 0.2 * 2 = -0.99999994
        // or:   0.6 - (0.2 / 2 + 0.4) / 0.2 * 2 = 1.0000002
        val normalizedOriginalLightness =
            convertToNormalizedLightness(originalLightness, minLightness, lightnessDelta) *
                    randomDirection min -1f max 1f

        return convertToShiftedLightness(
            waveFunction.value(
                abs(directionModifier) * mod + waveFunction.angle(
                    normalizedOriginalLightness
                )
            ) * randomDirection, minLightness, lightnessDelta
        )
    }

    private fun convertToShiftedLightness(
        normalizedLightness: Float,
        minLightness: Float,
        lightnessDelta: Float
    ): Float = (normalizedLightness * lightnessDelta / 2) + (lightnessDelta / 2 + minLightness)

    private fun convertToNormalizedLightness(
        shiftedLightness: Float,
        minLightness: Float,
        lightnessDelta: Float
    ): Float = (shiftedLightness - (lightnessDelta / 2 + minLightness)) / lightnessDelta * 2

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun getOpacity() = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }
}