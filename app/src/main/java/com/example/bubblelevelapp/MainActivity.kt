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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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

        // Initialize the sensor manager and accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: throw IllegalStateException("Accelerometer not available")

        // Sensor listener for calculating tilt angles
        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0] // Tilt left-right
                    val y = it.values[1] // Tilt up-down
                    val z = it.values[2] // Vertical axis

                    // Detect if device is flat
                    isFlat = z > 9.0 || z < -9.0

                    // Calculate tilt angles for X and Y axes
                    angleX = -x.coerceIn(-10f, 10f) // Invert X for intuitive movement
                    angleY = y.coerceIn(-10f, 10f)

                    // Maintain last 500 sensor values
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
                    BubbleLevel2D(angleX, angleY)
                    MaxMinValues(angleHistoryX, angleHistoryY)
                }
            }
        }
    }

    @Composable
    fun BubbleLevel2D(angleX: Float, angleY: Float) {
        // Define dimensions
        val containerSize = 300.dp
        val bubbleSize = 30.dp
        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        // Calculate bubble offset based on angle
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
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size / 2f
                drawLine(
                    color = onSurfaceColor,
                    start = Offset(center.width, 0f),
                    end = Offset(center.width, size.height),
                    strokeWidth = 4f
                )
                drawLine(
                    color = onSurfaceColor,
                    start = Offset(0f, center.height),
                    end = Offset(size.width, center.height),
                    strokeWidth = 4f
                )
            }

            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .offset { IntOffset(bubbleOffsetX, bubbleOffsetY) }
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.onSecondary, CircleShape)
            )

            Text(
                text = "X: %.2f°, Y: %.2f°".format(angleX, angleY),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    @Composable
    fun MaxMinValues(angleHistoryX: List<Float>, angleHistoryY: List<Float>) {
        val maxX = angleHistoryX.maxOrNull() ?: 0f
        val minX = angleHistoryX.minOrNull() ?: 0f
        val maxY = angleHistoryY.maxOrNull() ?: 0f
        val minY = angleHistoryY.minOrNull() ?: 0f

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Max X: %.2f°, Min X: %.2f°".format(maxX, minX), style = MaterialTheme.typography.bodyLarge)
            Text("Max Y: %.2f°, Min Y: %.2f°".format(maxY, minY), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
