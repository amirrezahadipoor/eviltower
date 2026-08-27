package ir.hadipoor.eviltower

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableFloatStateOf
import ir.hadipoor.eviltower.monetization.AdManager
import ir.hadipoor.eviltower.monetization.BazaarBillingProvider
import ir.hadipoor.eviltower.monetization.BillingManager
import ir.hadipoor.eviltower.monetization.MockAdProvider
import ir.hadipoor.eviltower.ui.EvilTowerApp
import ir.hadipoor.eviltower.ui.GameViewModel

/**
 * Single-activity game host.
 *
 * * portrait-locked, edge-to-edge, screen kept on while playing;
 * * feeds the accelerometer into the tilt control scheme;
 * * wires the Cafe Bazaar billing provider and the (stubbed) ad provider.
 */
class MainActivity : ComponentActivity(), SensorEventListener {

    private val viewModel: GameViewModel by viewModels()

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private val tiltX = mutableFloatStateOf(0f)

    private lateinit var bazaarBilling: BazaarBillingProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // --- monetization: Cafe Bazaar billing + placeholder ads ---------------------------
        bazaarBilling = BazaarBillingProvider(this)
        BillingManager.provider = bazaarBilling
        bazaarBilling.connect { }
        AdManager.provider = MockAdProvider()

        setContent {
            EvilTowerApp(viewModel = viewModel, tiltX = tiltX)
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        viewModel.audio.resumeMusic()
    }

    override fun onPause() {
        sensorManager?.unregisterListener(this)
        viewModel.audio.pauseMusic()
        super.onPause()
    }

    override fun onDestroy() {
        runCatching { bazaarBilling.disconnect() }
        super.onDestroy()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val event = event ?: return
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        // portrait: values[0] is the left/right tilt (m/s^2). ±3 m/s² = full speed.
        tiltX.floatValue = (-event.values[0] / 3.2f).coerceIn(-1f, 1f)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
