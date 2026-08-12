package com.example.nivelir

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var primarySensor: Sensor? = null
    private var vibrator: Vibrator? = null

    private var pitch by mutableFloatStateOf(0f)
    private var roll by mutableFloatStateOf(0f)

    private var hasHapticsTriggered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        primarySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        } catch (_: Exception) { null }

        setContent {
            MaterialTheme {
                LaunchedEffect(pitch, roll) {
                    if (abs(pitch) < 0.3f && abs(roll) < 0.3f) {
                        if (!hasHapticsTriggered) {
                            try {
                                vibrator?.vibrate(
                                    VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                                )
                            } catch (_: SecurityException) {}
                            hasHapticsTriggered = true
                        }
                    } else {
                        hasHapticsTriggered = false
                    }
                }

                NivelirScreen(
                    pitch = pitch,
                    roll = roll,
                    onExit = {
                        finish()
                    }
                )
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        val alpha = 0.12f

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)

            val rawPitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val rawRoll = Math.toDegrees(orientation[2].toDouble()).toFloat()

            pitch += alpha * (rawPitch - pitch)
            roll += alpha * (rawRoll - roll)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val rawRoll = Math.toDegrees(atan2(event.values[0].toDouble(), event.values[2].toDouble())).toFloat()
            val rawPitch = Math.toDegrees(atan2(event.values[1].toDouble(), event.values[2].toDouble())).toFloat()

            pitch += alpha * (rawPitch - pitch)
            roll += alpha * (rawRoll - roll)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onResume() {
        super.onResume()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        primarySensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}

val CaseOuterBg = Color(0xFF12100E)
val CaseInnerBg = Color(0xFF1E1A17)
val BezelBorderColor = Color(0xFF382E26)
val FluidCenterColor = Color(0xFFFFB300)
val FluidEdgeColor = Color(0xFFB26A00)
val ReticleColor = Color(0xFF38230B)

val BubbleGreyCenter = Color(0xFF555555)
val BubbleGreyEdge = Color(0xFF222222)

@Composable
fun NivelirScreen(
    pitch: Float,
    roll: Float,
    onExit: () -> Unit
) {
    val textRotationAngle = remember(pitch, roll) {
        when {
            roll > 20f -> -90f
            roll < -20f -> 90f
            pitch < -25f -> 0f
            else -> 180f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CaseOuterBg)
            .systemBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "NIVELIR",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "by Ventsislav Negentsov",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(CaseInnerBg)
                .border(2.dp, BezelBorderColor, RoundedCornerShape(24.dp))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BullseyeLevelView(
                        pitch = pitch,
                        roll = roll,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    Column(
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${String.format("%.1f", abs(pitch))}°",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                softWrap = false,
                                modifier = Modifier
                                    .wrapContentSize(unbounded = true)
                                    .rotate(textRotationAngle)
                            )
                        }

                        VerticalTubeLevelView(
                            pitch = pitch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalTubeLevelView(
                        roll = roll,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.padding(start = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${String.format("%.1f", abs(roll))}°",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            softWrap = false,
                            modifier = Modifier
                                .wrapContentSize(unbounded = true)
                                .rotate(textRotationAngle)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B231D)),
            border = BorderStroke(1.5.dp, BezelBorderColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Exit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun BullseyeLevelView(pitch: Float, roll: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val minDim = size.minDimension
        if (minDim <= 0f) return@Canvas

        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = minDim / 2
        val fluidRadius = max(1f, outerRadius - 8.dp.toPx())

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2C241E), Color(0xFF15110E)),
                center = center,
                radius = outerRadius
            ),
            radius = outerRadius,
            center = center
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(FluidCenterColor, FluidEdgeColor),
                center = center,
                radius = fluidRadius
            ),
            radius = fluidRadius,
            center = center
        )

        drawLine(ReticleColor, Offset(center.x, center.y - fluidRadius), Offset(center.x, center.y + fluidRadius), 1.5f.dp.toPx())
        drawLine(ReticleColor, Offset(center.x - fluidRadius, center.y), Offset(center.x + fluidRadius, center.y), 1.5f.dp.toPx())
        drawCircle(ReticleColor, radius = fluidRadius * 0.3f, center = center, style = Stroke(width = 1.5f.dp.toPx()))

        val bubbleRadius = fluidRadius * 0.2f
        val maxOffset = max(0f, fluidRadius - bubbleRadius - 4.dp.toPx())

        val clampedRoll = (-roll / 30f).coerceIn(-1f, 1f)
        val clampedPitch = (pitch / 30f).coerceIn(-1f, 1f)

        val bubbleCenter = Offset(
            x = center.x + (clampedRoll * maxOffset),
            y = center.y + (clampedPitch * maxOffset)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BubbleGreyCenter, BubbleGreyEdge),
                center = bubbleCenter,
                radius = bubbleRadius
            ),
            radius = bubbleRadius,
            center = bubbleCenter
        )
    }
}

@Composable
fun VerticalTubeLevelView(pitch: Float, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CaseOuterBg)
            .border(1.5.dp, BezelBorderColor, RoundedCornerShape(10.dp))
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(FluidEdgeColor, FluidCenterColor, FluidEdgeColor)
            )
        )

        val mark1Y = h * 0.44f
        val mark2Y = h * 0.56f
        drawLine(ReticleColor, Offset(0f, mark1Y), Offset(w, mark1Y), strokeWidth = 2.dp.toPx())
        drawLine(ReticleColor, Offset(0f, mark2Y), Offset(w, mark2Y), strokeWidth = 2.dp.toPx())

        val bubbleHeight = 32.dp.toPx()
        val bubbleWidth = w * 0.8f
        val maxTravel = max(0f, (h / 2) - (bubbleHeight / 2) - 4.dp.toPx())
        val bubbleY = (h / 2) + ((pitch / 30f).coerceIn(-1f, 1f) * maxTravel) - (bubbleHeight / 2)
        val bubbleX = (w - bubbleWidth) / 2

        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(BubbleGreyCenter, BubbleGreyEdge),
                center = Offset(w / 2, bubbleY + (bubbleHeight / 2)),
                radius = max(1f, bubbleHeight)
            ),
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = CornerRadius(bubbleHeight / 2, bubbleHeight / 2)
        )
    }
}

@Composable
fun HorizontalTubeLevelView(roll: Float, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CaseOuterBg)
            .border(1.5.dp, BezelBorderColor, RoundedCornerShape(10.dp))
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(FluidEdgeColor, FluidCenterColor, FluidEdgeColor)
            )
        )

        val mark1X = w * 0.44f
        val mark2X = w * 0.56f
        drawLine(ReticleColor, Offset(mark1X, 0f), Offset(mark1X, h), strokeWidth = 2.dp.toPx())
        drawLine(ReticleColor, Offset(mark2X, 0f), Offset(mark2X, h), strokeWidth = 2.dp.toPx())

        val bubbleWidth = 32.dp.toPx()
        val bubbleHeight = h * 0.8f
        val maxTravel = max(0f, (w / 2) - (bubbleWidth / 2) - 4.dp.toPx())
        val bubbleX = (w / 2) - ((roll / 30f).coerceIn(-1f, 1f) * maxTravel) - (bubbleWidth / 2)
        val bubbleY = (h - bubbleHeight) / 2

        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(BubbleGreyCenter, BubbleGreyEdge),
                center = Offset(bubbleX + (bubbleWidth / 2), h / 2),
                radius = max(1f, bubbleWidth)
            ),
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = CornerRadius(bubbleWidth / 2, bubbleWidth / 2)
        )
    }
}