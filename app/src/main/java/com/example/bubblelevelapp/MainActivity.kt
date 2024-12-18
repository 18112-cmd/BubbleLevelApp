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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import kotlin.math.atan2
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var sensorListener: SensorEventListener

    private var angleX by mutableStateOf(0f)
    private var angleY by mutableStateOf(0f)
    private var isFlat by mutableStateOf(false)

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

                    // Calculate tilt angles for X and Y axes, and invert X for intuitive movement
                    angleX = -x.coerceIn(-10f, 10f) // Left-right tilt
                    angleY = y.coerceIn(-10f, 10f)  // Up-down tilt

                    Log.d("BubbleLevel", "AngleX: $angleX, AngleY: $angleY, Flat: $isFlat")
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        setContent {
            BubbleLevelAppTheme {
                BubbleLevelScreen(angleX, angleY, isFlat)
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
    fun BubbleLevelScreen(angleX: Float, angleY: Float, isFlat: Boolean) {
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
                BubbleLevel2D(angleX = angleX, angleY = angleY)
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
            val normalizedX = angleX / 10f // Normalize to [-1, 1]
            val maxOffset = (containerSize.toPx() - bubbleSize.toPx()) / 2
            (normalizedX * maxOffset).roundToInt()
        }

        val bubbleOffsetY = with(LocalDensity.current) {
            val normalizedY = angleY / 10f // Normalize to [-1, 1]
            val maxOffset = (containerSize.toPx() - bubbleSize.toPx()) / 2
            (-normalizedY * maxOffset).roundToInt() // Invert Y for upward tilt
        }

        Box(
            modifier = Modifier
                .size(containerSize)
                .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                .border(2.dp, MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            // Draw grid center lines
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

            // Bubble indicator
            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .offset { IntOffset(bubbleOffsetX, bubbleOffsetY) }
                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.onSecondary, CircleShape)
            )

            // Display current angles
            Text(
                text = "X: %.2f°, Y: %.2f°".format(angleX, angleY),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
