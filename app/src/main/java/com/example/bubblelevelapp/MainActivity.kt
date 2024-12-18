package com.example.bubblelevelapp

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bubblelevelapp.ui.theme.BubbleLevelAppTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var sensorListener: SensorEventListener

    private var angleX by mutableStateOf(0f)
    private var angleY by mutableStateOf(0f)
    private var isFlat by mutableStateOf(false)

    // Maintain the last 500 sensor values
    private val angleHistoryX = mutableListOf<Float>()
    private val angleHistoryY = mutableListOf<Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: throw IllegalStateException("Accelerometer not available")

        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0] // Tilt left-right
                    val y = it.values[1] // Tilt up-down
                    val z = it.values[2] // Vertical axis

                    isFlat = z > 9.0 || z < -9.0

                    angleX = -x.coerceIn(-10f, 10f)
                    angleY = y.coerceIn(-10f, 10f)

                    if (angleHistoryX.size >= 500) angleHistoryX.removeAt(0)
                    if (angleHistoryY.size >= 500) angleHistoryY.removeAt(0)
                    angleHistoryX.add(angleX)
                    angleHistoryY.add(angleY)

                    Log.d("BubbleLevel", "AngleX: $angleX, AngleY: $angleY, Flat: $isFlat")
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        setContent {
            BubbleLevelAppTheme {
                BubbleLevelScreen(angleX, angleY, isFlat, angleHistoryX, angleHistoryY)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
    }

    @Composable
    fun BubbleLevelScreen(
        angleX: Float,
        angleY: Float,
        isFlat: Boolean,
        angleHistoryX: List<Float>,
        angleHistoryY: List<Float>
    ) {
        val configuration = LocalConfiguration.current
        val orientation = configuration.orientation

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isFlat) {
                Text(
                    text = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        "Flat in Landscape Mode"
                    } else {
                        "Flat in Portrait Mode"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EnhancedBubbleLevel2D(angleX, angleY)
                    MaxMinValues(angleHistoryX, angleHistoryY)
                }
            }
        }
    }

    @Composable
    fun EnhancedBubbleLevel2D(angleX: Float, angleY: Float) {
        val containerSize = 300.dp
        val bubbleSize = 30.dp
        val primaryColor = MaterialTheme.colorScheme.primary
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
        val onSurfaceError = MaterialTheme.colorScheme.error

        val bubbleOffsetX = with(LocalDensity.current) {
            val normalizedX = angleX / 10f
            val maxOffset = (containerSize.toPx() - bubbleSize.toPx()) / 2
            (normalizedX * maxOffset).roundToInt()
        }
        val bubbleOffsetY = with(LocalDensity.current) {
            val normalizedY = angleY / 10f
            val maxOffset = (containerSize.toPx() - bubbleSize.toPx()) / 2
            (-normalizedY * maxOffset).roundToInt()
        }

        Box(
            modifier = Modifier
                .size(containerSize)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                    )
                )
                .border(2.dp, primaryColor),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size / 2f

                // Draw center grid lines
                drawLine(onSurfaceColor, Offset(center.width, 0f), Offset(center.width, size.height), 4f)
                drawLine(onSurfaceColor, Offset(0f, center.height), Offset(size.width, center.height), 4f)

                // Draw North direction arrow
                drawLine(
                    color = onSurfaceError,
                    start = Offset(center.width, center.height),
                    end = Offset(center.width, 20f),
                    strokeWidth = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }

            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .offset { IntOffset(bubbleOffsetX, bubbleOffsetY) }
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.onSecondary, CircleShape)
            )

            // Axis Labels - Adjusted Position
            Text(
                text = "X: %.2f°".format(angleX),
                color = primaryColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 24.dp) // Moved further down
            )
            Text(
                text = "Y: %.2f°".format(angleY),
                color = primaryColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp, y = -10.dp) // Adjusted for better visibility
            )
        }
    }

    @Composable
    fun MaxMinValues(angleHistoryX: List<Float>, angleHistoryY: List<Float>) {
        val maxX = angleHistoryX.maxOrNull() ?: 0f
        val minX = angleHistoryX.minOrNull() ?: 0f
        val maxY = angleHistoryY.maxOrNull() ?: 0f
        val minY = angleHistoryY.minOrNull() ?: 0f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Max X: %.2f°, Min X: %.2f°".format(maxX, minX), fontSize = 16.sp)
            Text("Max Y: %.2f°, Min Y: %.2f°".format(maxY, minY), fontSize = 16.sp)
        }
    }
}
