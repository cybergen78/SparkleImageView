package de.jarosz.sparkle

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.widget.RadioButton
import androidx.annotation.ColorInt
import de.jarosz.sparkle.wave.SinusoidWave
import de.jarosz.sparkle.wave.TriangleWave
import de.jarosz.sparkle.wave.WaveFunction
import kotlin.math.PI

@Suppress("MemberVisibilityCanBePrivate")
class FunctionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.buttonStyle
) : RadioButton(context, attrs, defStyleAttr) {
    private val paint = Paint()
    private val path = Path()

    /**
     * The wave function to draw. Default is [SinusoidWave].
     *
     * [getCurrentTextColor] and [strokeWidth] will be used for drawing the function wave.
     *
     * **Related XML Attributes:**
     *
     * [R.styleable.FunctionButton_waveType]
     * @see wavePeriods
     * @see strokeWidth
     * @see waveAmplitude
     * @see waveOffsetY
     * @see waveOffsetAngle
     */
    var waveFunction: WaveFunction = SinusoidWave()
        set(value) {
            field = value
            invalidate()
        }
    /**
     * How many periods of the wave to draw on the button. Default is 2.5.
     *
     * **Related XML Attributes:**
     *
     * [R.styleable.FunctionButton_wavePeriods]
     * @see waveFunction
     */
    var wavePeriods: Float = 2.5f
        set(value) {
            field = value
            invalidate()
        }
    /**
     * The amplitude of the wave. Default is 1.
     *
     * **Related XML Attributes:**
     *
     * [R.styleable.FunctionButton_waveAmplitude]
     * @see waveFunction
     */
    var waveAmplitude: Float = 1f
        set(value) {
            field = value
            invalidate()
        }
    /**
     * The y offset of the wave. Default is 0.5 (center vertically).
     *
     * **Related XML Attributes:**
     *
     * [R.styleable.FunctionButton_waveOffsetY]
     * @see waveFunction
     */
    var waveOffsetY: Float = 0.5f
        set(value) {
            field = value
            invalidate()
        }
    /**
     * The offset angle for the wave in radians. Default is 0.
     *
     * E.g. with an offset angle of [PI] a [SinusoidWave] would start downwards instead of upwards.
     *
     * **Related XML Attributes:**
     *
     * [R.styleable.FunctionButton_waveOffsetAngle]
     * @see waveFunction
     */
    var waveOffsetAngle: Float = 0f
        set(value) {
            field = value
            invalidate()
        }
    /**
     * The stroke width for the wave. Default is 2dp.
     *
     * **Related XML Attributes:**
     *
     * [R.styleable.FunctionButton_strokeWidth]
     * @see waveFunction
     */
    var strokeWidth: Float = 2 * context.resources.displayMetrics.density
        set(value) {
            field = value
            invalidate()
        }
    /**
     *  If true, an X axis will be drawn at level 0. Default is false.
     *
     *  [zeroAxisColor] and [zeroAxisStrokeWidth] will be used for drawing the function wave at [zeroAxisVerticalBias].
     *
     * **Related XML Attributes:**
     *
     * [R.styleable.FunctionButton_zeroAxisEnabled]
     * @see zeroAxisVerticalBias
     * @see zeroAxisColor
     * @see zeroAxisStrokeWidth
     */
    var zeroAxisEnabled: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    /**
     * The vertical bias of the zero axis. Default is 0.5.
     *
     * **Related XML Attributes:**
     *
     * [R.styleable.FunctionButton_zeroAxisVerticalBias]
     * @see zeroAxisEnabled
     */
    var zeroAxisVerticalBias: Float = 0.5f
        set(value) {
            field = value
            invalidate()
        }
    /**
     * The color for the zero axis. Default is [Color.GRAY].
     *
     * **Related XML Attributes:**
     *
     * [R.styleable.FunctionButton_zeroAxisColor]
     * @see zeroAxisEnabled
     */
    @ColorInt
    var zeroAxisColor: Int = Color.GRAY
        set(value) {
            field = value
            invalidate()
        }
    /**
     * The stroke width for the zero axis. Default is 2dp.
     *
     * **Related XML Attributes:**
     *
     * [R.styleable.FunctionButton_zeroAxisStrokeWidth]
     * @see zeroAxisEnabled
     */
    var zeroAxisStrokeWidth: Float = 2 * context.resources.displayMetrics.density
        set(value) {
            field = value
            invalidate()
        }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.FunctionButton, defStyleAttr, 0).apply {
            waveFunction = when (getInt(R.styleable.FunctionButton_waveType, 0)) {
                0 -> SinusoidWave()   // default
                1 -> TriangleWave()
                else -> waveFunction
            }

            wavePeriods = getFloat(R.styleable.FunctionButton_wavePeriods, wavePeriods)
            waveAmplitude = getFloat(R.styleable.FunctionButton_waveAmplitude, waveAmplitude)
            waveOffsetY = getFloat(R.styleable.FunctionButton_waveOffsetY, waveOffsetY)
            waveOffsetAngle = getFloat(R.styleable.FunctionButton_waveOffsetAngle, waveOffsetAngle)
            strokeWidth = getDimension(R.styleable.FunctionButton_strokeWidth, strokeWidth)
            zeroAxisEnabled = getBoolean(R.styleable.FunctionButton_zeroAxisEnabled, zeroAxisEnabled)
            zeroAxisVerticalBias = getFloat(R.styleable.FunctionButton_zeroAxisVerticalBias, zeroAxisVerticalBias)
            zeroAxisColor = getColor(R.styleable.FunctionButton_zeroAxisColor, zeroAxisColor)
            zeroAxisStrokeWidth = getDimension(R.styleable.FunctionButton_zeroAxisStrokeWidth, zeroAxisStrokeWidth)

            recycle()
        }

        paint.apply {
            // FUTURE: use FILL or FILL_AND_STROKE and draw a gradient from minimum lightness
            //  through the base color at the zero axis to maximum lightness.
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val drawingWidth = width - compoundPaddingLeft - compoundPaddingRight
        val drawingHeight = (height - compoundPaddingTop - compoundPaddingBottom).toFloat()
        val yOffset = drawingHeight * (1 - waveOffsetY) + compoundPaddingTop
        val drawingWidthFloat = drawingWidth.toFloat()
        val offsetX = compoundPaddingLeft.toFloat()

        if (zeroAxisEnabled) {
            paint.color = zeroAxisColor
            paint.strokeWidth = zeroAxisStrokeWidth
            val zeroAxisY = drawingHeight * (1 - zeroAxisVerticalBias) + compoundPaddingTop
            canvas?.drawLine(offsetX, zeroAxisY, offsetX + drawingWidth, zeroAxisY, paint)
        }

        paint.color = currentTextColor
        paint.strokeWidth = strokeWidth
        path.reset()
        path.moveTo(offsetX, y(0, drawingWidthFloat, drawingHeight, yOffset))
        (1 until drawingWidth).forEach {
            path.lineTo(
                it + offsetX,
                y(it, drawingWidthFloat, drawingHeight, yOffset)
            )
        }
        path.lineTo(
            drawingWidth + offsetX,
            y(drawingWidth, drawingWidthFloat, drawingHeight, yOffset)
        )
        canvas?.drawPath(path, paint)
    }

    private fun y(
        x: Int,
        drawingWidth: Float,
        drawingHeight: Float,
        yOffset: Float
    ) =
        -waveFunction.value(x / drawingWidth * 2 * PI.toFloat() * wavePeriods + waveOffsetAngle) * drawingHeight / 2 * waveAmplitude + yOffset
}