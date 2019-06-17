# SparkleImageView
SparkleImageView is an Android library which renders a highly customizable noise pattern with a given color.
The [`SparkleImageView`](./src/main/java/sparkle/SparkleImageView.kt) uses the [`SparkleDrawable`](./src/main/java/sparkle/SparkleDrawable.kt)
and adds animation controls, e.g. responding to device motion or touches.

The [`WaveFunction`](./src/main/java/sparkle/wave)s used to modulate the `Bitmap` pixels
can be visualized easily with the [`FunctionButton`](./src/main/java/sparkle/FunctionButton.kt).

The sample application shows off every aspect of this library, so it's worth a look.

## Examples

### `SparkleImageView` with `SparkleDrawable`
The `mode` is set to `Mode.TOUCH_DISTANCE` and the dotScale is set to _2dp_.
Any other parameters are left at their default values.

![SparkleImageView with SparkleDrawable](./art/SparkleImageView_with_SparkleDrawable.webm)

### `FunctionButton`
![FunctionButton](./art/FunctionButton.webm)

## Installation
The sparkleimageview package has the following dependencies:
* Android KTX
* Appcompat
```groovy
implementation 'de.jarosz.sparkle:sparkleimageview:<latest-version>'
```

## Usage
SparkleDrawable, SparkleImageView and FunctionButton can be used in exchange for their superclasses.
SparkleImageView and FunctionButton are also usable in XML as well as programmatically.

### XML

#### `SparkleImageView`
```xml
<de.jarosz.sparkle.SparkleImageView
        android:id="@+id/imageView"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="@color/sparkleColor"/>
```
> Obviously, only `ColorDrawable` is supported as background drawable. 

Other supported XML attributes are:

* `sensitivity: [float|reference]`
* `waveFunction: [sinusoid|triangle]`
* `mode: [device_motion|touch_distance|touch_circle]`

#### `FunctionButton`
```xml
<de.jarosz.sparkle.FunctionButton
        android:layout_width="match_parent"
        android:layout_height="100dp"
        android:id="@+id/triangleButton"
        android:paddingStart="5dp"
        android:paddingEnd="5dp"
        android:paddingTop="7dp"
        android:paddingBottom="7dp"
        app:waveType="triangle"
        app:zeroAxisEnabled="true"/>
```
Use a `ToggleButton` style to customize the [`FunctionButton`](./src/main/java/sparkle/FunctionButton.kt) although it's a `RadioButton`, if you want a `Checkable` behavior.
The padding is used to keep the graph in the [`FunctionButton`](./src/main/java/sparkle/FunctionButton.kt)s drawable bounds.

Other supported XML attributes are:

* `wavePeriods: [float|reference]`
* `strokeWidth: [dimension|reference]`
* `waveAmplitude: [float|reference]`
* `waveOffsetY: [float|reference]`
* `waveOffsetAngle: [float|reference]`
* `zeroAxisVerticalBias: [float|reference]`
* `zeroAxisColor: [color|reference]`
* `zeroAxisStrokeWidth: [dimension|reference]`
### Code

#### `SparkleDrawable`
```kotlin
val sparkleDrawable = SparkleDrawable(TriangleWave()).apply {
    dotScale = resources.displayMetrics.density.roundToInt()
    color = Color.BLUE
}
```

###### Performance
All `Bitmap` lightness modulation is done in software.
Each pixels/dots lightness value is calculated separately according to the [`WaveFunction`](./src/main/java/sparkle/wave),
a random start value and other randomizing parameters.
Therefore the dot scale is limited to at least _2_ and it's a good idea to set it to at least
`resources.displayMetrics.density.roundToInt()` for acceptable performance.
This way you get dots of _1dp_ in size, rounded to `Int`.

#### `SparkleImageView`
```kotlin
val sparkleImageView = SparkleImageView(this).apply {
    setBackgroundColor(Color.BLUE)
    // change parameters as desired
    mode = Mode.TOUCH_DISTANCE
    // fine tuning through sparkleDrawable
    sparkleDrawable.apply {
        lightnessVarianceUp = 0.2f
        retroMode = false
        highlightsAmount = 0.01f
    }
}
```
The `dotScale` is set to `resources.displayMetrics.density.roundToInt()` by default, if you don't instantiate [`SparkleDrawable`](./src/main/java/sparkle/SparkleDrawable.kt) yourself.
Furthermore, most of the "fine tuning parameters" are scoped to [`SparkleDrawable`](./src/main/java/sparkle/SparkleDrawable.kt).

#### WaveFunction
An interface defining just to methods for converting an angle to a value and vice versa.

For the use with [`SparkleDrawable`](./src/main/java/sparkle/SparkleDrawable.kt) and [`FunctionButton`](./src/main/java/sparkle/FunctionButton.kt)
custom [`WaveFunction`](./src/main/java/sparkle/wave)s must return values between _-1_ and _1_ for any given angle.

For all [`WaveFunction`](./src/main/java/sparkle/wave)s `value(angle)` must be equal to `value(angle(value(angle)))`.