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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

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
        primarySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val alpha = 0.12f

        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val z = event.values[2].toDouble()

        val rawRoll = Math.toDegrees(atan2(x, sqrt(y * y + z * z))).toFloat()
        val rawPitch = Math.toDegrees(atan2(y, sqrt(x * x + z * z))).toFloat()

        pitch += alpha * (rawPitch - pitch)
        roll += alpha * (rawRoll - roll)
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

data class ThemePalette(
    val name: String,
    val caseOuterBg: Color,
    val caseInnerBg: Color,
    val bezelBorderColor: Color,
    val fluidCenterColor: Color,
    val fluidEdgeColor: Color,
    val reticleColor: Color
)

val AppThemes = listOf(
    ThemePalette(
        name = "Amber",
        caseOuterBg = Color(0xFF12100E),
        caseInnerBg = Color(0xFF1E1A17),
        bezelBorderColor = Color(0xFF382E26),
        fluidCenterColor = Color(0xFFFFB300),
        fluidEdgeColor = Color(0xFFB26A00),
        reticleColor = Color(0xFF38230B)
    ),
    ThemePalette(
        name = "Cyan",
        caseOuterBg = Color(0xFF0A1215),
        caseInnerBg = Color(0xFF122026),
        bezelBorderColor = Color(0xFF1D353F),
        fluidCenterColor = Color(0xFF00E5FF),
        fluidEdgeColor = Color(0xFF00838F),
        reticleColor = Color(0xFF00363D)
    ),
    ThemePalette(
        name = "Magenta",
        caseOuterBg = Color(0xFF150A12),
        caseInnerBg = Color(0xFF20121D),
        bezelBorderColor = Color(0xFF3F1D35),
        fluidCenterColor = Color(0xFFFF007F),
        fluidEdgeColor = Color(0xFF8F0047),
        reticleColor = Color(0xFF3D001F)
    ),
    ThemePalette(
        name = "Lime",
        caseOuterBg = Color(0xFF0E120A),
        caseInnerBg = Color(0xFF171E12),
        bezelBorderColor = Color(0xFF2E3826),
        fluidCenterColor = Color(0xFFCCFF00),
        fluidEdgeColor = Color(0xFF77B200),
        reticleColor = Color(0xFF23380B)
    ),
    ThemePalette(
        name = "Matrix",
        caseOuterBg = Color(0xFF0A150D),
        caseInnerBg = Color(0xFF122015),
        bezelBorderColor = Color(0xFF1D3F26),
        fluidCenterColor = Color(0xFF00FF66),
        fluidEdgeColor = Color(0xFF008F38),
        reticleColor = Color(0xFF003D17)
    ),
    ThemePalette(
        name = "Purple",
        caseOuterBg = Color(0xFF120A15),
        caseInnerBg = Color(0xFF1C1220),
        bezelBorderColor = Color(0xFF351D3F),
        fluidCenterColor = Color(0xFF9900FF),
        fluidEdgeColor = Color(0xFF55008F),
        reticleColor = Color(0xFF22003D)
    ),
    ThemePalette(
        name = "Crimson",
        caseOuterBg = Color(0xFF150A0A),
        caseInnerBg = Color(0xFF201212),
        bezelBorderColor = Color(0xFF3F1D1D),
        fluidCenterColor = Color(0xFFFF2A2A),
        fluidEdgeColor = Color(0xFF8F1212),
        reticleColor = Color(0xFF3D0B0B)
    )
)

@Composable
fun NivelirScreen(
    pitch: Float,
    roll: Float,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nivelir_prefs", Context.MODE_PRIVATE) }
    var currentThemeIndex by remember {
        mutableIntStateOf(prefs.getInt("theme_index", 0).coerceIn(0, AppThemes.size - 1))
    }
    val theme = AppThemes[currentThemeIndex]

    val textRotationAngle = remember(pitch, roll) {
        when {
            roll > 20f -> 90f
            roll < -20f -> -90f
            pitch < -25f -> 180f
            else -> 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.caseOuterBg)
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
                .background(theme.caseInnerBg)
                .border(2.dp, theme.bezelBorderColor, RoundedCornerShape(24.dp))
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
                        theme = theme,
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
                            theme = theme,
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
                        theme = theme,
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    currentThemeIndex = (currentThemeIndex + 1) % AppThemes.size
                    prefs.edit().putInt("theme_index", currentThemeIndex).apply()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = theme.caseInnerBg),
                border = BorderStroke(1.5.dp, theme.fluidCenterColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Theme: ${theme.name}",
                    color = theme.fluidCenterColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = onExit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = theme.caseInnerBg),
                border = BorderStroke(1.5.dp, theme.bezelBorderColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Exit",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun BullseyeLevelView(pitch: Float, roll: Float, theme: ThemePalette, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val minDim = size.minDimension
        if (minDim <= 0f) return@Canvas

        val center = Offset(size.width / 2, size.height / 2)
        val outerRadius = minDim / 2
        val fluidRadius = max(1f, outerRadius - 8.dp.toPx())

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(theme.bezelBorderColor, theme.caseOuterBg),
                center = center,
                radius = outerRadius
            ),
            radius = outerRadius,
            center = center
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(theme.fluidCenterColor, theme.fluidEdgeColor),
                center = center,
                radius = fluidRadius
            ),
            radius = fluidRadius,
            center = center
        )

        drawLine(theme.reticleColor, Offset(center.x, center.y - fluidRadius), Offset(center.x, center.y + fluidRadius), 1.5f.dp.toPx())
        drawLine(theme.reticleColor, Offset(center.x - fluidRadius, center.y), Offset(center.x + fluidRadius, center.y), 1.5f.dp.toPx())
        drawCircle(theme.reticleColor, radius = fluidRadius * 0.3f, center = center, style = Stroke(width = 1.5f.dp.toPx()))

        val bubbleRadius = fluidRadius * 0.16f
        val maxOffset = max(0f, fluidRadius - bubbleRadius - 4.dp.toPx())

        val clampedRoll = (roll / 30f).coerceIn(-1f, 1f)
        val clampedPitch = (-pitch / 30f).coerceIn(-1f, 1f)

        val bubbleCenter = Offset(
            x = center.x + (clampedRoll * maxOffset),
            y = center.y + (clampedPitch * maxOffset)
        )

        // Realistic glass bubble with offset glossy highlight and rim
        val highlightOffset = Offset(bubbleCenter.x - bubbleRadius * 0.3f, bubbleCenter.y - bubbleRadius * 0.3f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.85f), theme.fluidCenterColor.copy(alpha = 0.3f), theme.fluidEdgeColor.copy(alpha = 0.85f)),
                center = highlightOffset,
                radius = bubbleRadius * 1.2f
            ),
            radius = bubbleRadius,
            center = bubbleCenter
        )
        drawCircle(
            color = theme.reticleColor.copy(alpha = 0.6f),
            radius = bubbleRadius,
            center = bubbleCenter,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun VerticalTubeLevelView(pitch: Float, theme: ThemePalette, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(theme.caseOuterBg)
            .border(1.5.dp, theme.bezelBorderColor, RoundedCornerShape(10.dp))
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(theme.fluidEdgeColor, theme.fluidCenterColor, theme.fluidEdgeColor)
            )
        )

        val mark1Y = h * 0.44f
        val mark2Y = h * 0.56f
        drawLine(theme.reticleColor, Offset(0f, mark1Y), Offset(w, mark1Y), strokeWidth = 2.dp.toPx())
        drawLine(theme.reticleColor, Offset(0f, mark2Y), Offset(w, mark2Y), strokeWidth = 2.dp.toPx())

        val bubbleHeight = 26.dp.toPx()
        val bubbleWidth = w * 0.72f
        val maxTravel = max(0f, (h / 2) - (bubbleHeight / 2) - 4.dp.toPx())
        val bubbleY = (h / 2) - ((pitch / 30f).coerceIn(-1f, 1f) * maxTravel) - (bubbleHeight / 2)
        val bubbleX = (w - bubbleWidth) / 2

        val bubbleHighlightCenter = Offset(w / 2 - bubbleWidth * 0.15f, bubbleY + bubbleHeight * 0.3f)
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.85f), theme.fluidCenterColor.copy(alpha = 0.3f), theme.fluidEdgeColor.copy(alpha = 0.85f)),
                center = bubbleHighlightCenter,
                radius = max(1f, bubbleHeight) * 1.1f
            ),
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = CornerRadius(bubbleHeight / 2, bubbleHeight / 2)
        )
        drawRoundRect(
            color = theme.reticleColor.copy(alpha = 0.6f),
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = CornerRadius(bubbleHeight / 2, bubbleHeight / 2),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun HorizontalTubeLevelView(roll: Float, theme: ThemePalette, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(theme.caseOuterBg)
            .border(1.5.dp, theme.bezelBorderColor, RoundedCornerShape(10.dp))
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(theme.fluidEdgeColor, theme.fluidCenterColor, theme.fluidEdgeColor)
            )
        )

        val mark1X = w * 0.44f
        val mark2X = w * 0.56f
        drawLine(theme.reticleColor, Offset(mark1X, 0f), Offset(mark1X, h), strokeWidth = 2.dp.toPx())
        drawLine(theme.reticleColor, Offset(mark2X, 0f), Offset(mark2X, h), strokeWidth = 2.dp.toPx())

        val bubbleWidth = 26.dp.toPx()
        val bubbleHeight = h * 0.72f
        val maxTravel = max(0f, (w / 2) - (bubbleWidth / 2) - 4.dp.toPx())
        val bubbleX = (w / 2) + ((roll / 30f).coerceIn(-1f, 1f) * maxTravel) - (bubbleWidth / 2)
        val bubbleY = (h - bubbleHeight) / 2

        val bubbleHighlightCenter = Offset(bubbleX + bubbleWidth * 0.3f, h / 2 - bubbleHeight * 0.15f)
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.85f), theme.fluidCenterColor.copy(alpha = 0.3f), theme.fluidEdgeColor.copy(alpha = 0.85f)),
                center = bubbleHighlightCenter,
                radius = max(1f, bubbleWidth) * 1.1f
            ),
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = CornerRadius(bubbleWidth / 2, bubbleWidth / 2)
        )
        drawRoundRect(
            color = theme.reticleColor.copy(alpha = 0.6f),
            topLeft = Offset(bubbleX, bubbleY),
            size = Size(bubbleWidth, bubbleHeight),
            cornerRadius = CornerRadius(bubbleWidth / 2, bubbleWidth / 2),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}