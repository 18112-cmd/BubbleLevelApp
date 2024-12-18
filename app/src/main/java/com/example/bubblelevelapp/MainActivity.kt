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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.example.bubblelevelapp.ui.theme.BubbleLevelAppTheme

class MainActivity : ComponentActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var sensorListener: SensorEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SensorManager and Sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: throw IllegalStateException("Accelerometer not available")

        // Initialize Sensor Listener
        sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val z = it.values[2]
                    Log.d("BubbleLevel", "Z-axis value: $z")
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }


        // Define content with Jetpack Compose
        setContent {
            BubbleLevelAppTheme {
                BubbleLevelScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Register Sensor Listener
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()

        // Unregister Sensor Listener
        sensorManager.unregisterListener(sensorListener)
    }

    // Bubble Level Composable Screen
    @Composable
    fun BubbleLevelScreen() {
        val configuration = LocalConfiguration.current
        var isFlat by remember { mutableStateOf(false) }
        val orientation = configuration.orientation

        // Set up the sensor listener
        LaunchedEffect(Unit) {
            sensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        val z = it.values[2]
                        isFlat = z > 9.0 || z < -9.0 // Detect if device is flat
                        Log.d("BubbleLevel", "Flat: $isFlat, Z: $z")
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
        }

        // Display content based on device state
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isFlat) {
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Text("Flat in Landscape Mode", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Flat in Portrait Mode", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Text("Not Flat in Landscape Mode", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Not Flat in Portrait Mode", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
