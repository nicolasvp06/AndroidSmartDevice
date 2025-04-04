package fr.isen.pissavinvernet.androidsmartdevice.main

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import fr.isen.pissavinvernet.androidsmartdevice.R
import fr.isen.pissavinvernet.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme
import java.util.*

class DeviceConnectedActivity : ComponentActivity() {
    private var gatt: BluetoothGatt? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private var button1Characteristic: BluetoothGattCharacteristic? = null
    private var button3Characteristic: BluetoothGattCharacteristic? = null
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var isConnected by mutableStateOf(false)
    private var ledStates = mutableStateListOf(false, false, false)
    private var btn1Count by mutableStateOf(0)
    private var btn3Count by mutableStateOf(0)
    private var isReceiving by mutableStateOf(false)

    private lateinit var deviceAddress: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceAddress = intent.getStringExtra("device_address") ?: return

        connectToDevice()

        setContent {
            AndroidSmartDeviceTheme {
                DeviceControlScreen()
            }
        }
    }

    @Composable
    fun DeviceControlScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Contrôle LEDs et Boutons", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(24.dp))

            ledStates.forEachIndexed { index, state ->
                val label = when (index) {
                    0 -> "LED 1 (Rouge)"
                    1 -> "LED 2 (Verte)"
                    2 -> "LED 3 (Bleue)"
                    else -> "LED ${index + 1}"
                }
                val statusText = if (state) "Allumée" else "Éteinte"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Image(
                        painter = painterResource(id = if (state) R.drawable.ic_led_on else R.drawable.ic_led_off),
                        contentDescription = label,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(8.dp)
                    )
                    Text("$label : $statusText", modifier = Modifier.weight(1f))
                    Button(onClick = { toggleLed(index) }) {
                        Text(if (state) "Éteindre" else "Allumer")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Recevoir notifications")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(checked = isReceiving, onCheckedChange = {
                    isReceiving = it
                    if (it) enableNotifications() else disableNotifications()
                })
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Bouton 1 : $btn1Count clics")
            Text("Bouton 3 : $btn3Count clics")

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = {
                disconnect()
                finish()
            }) {
                Text("Retour")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        gatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    isConnected = true
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    isConnected = false
                    disconnect()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                val services = gatt.services
                if (services.size > 3) {
                    val service3 = services[2]
                    val service4 = services[3]
                    ledCharacteristic = service3.characteristics.getOrNull(0)
                    button1Characteristic = service3.characteristics.getOrNull(1)
                    button3Characteristic = service4.characteristics.getOrNull(0)
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                if (characteristic == button3Characteristic) btn1Count++
                if (characteristic == button1Characteristic) btn3Count++
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun toggleLed(index: Int) {
        if (!isConnected) return
        ledStates[index] = !ledStates[index]

        val command = when (index) {
            0 -> if (ledStates[0]) byteArrayOf(0x01) else byteArrayOf(0x00)
            1 -> if (ledStates[1]) byteArrayOf(0x02) else byteArrayOf(0x00)
            2 -> if (ledStates[2]) byteArrayOf(0x03) else byteArrayOf(0x00)
            else -> byteArrayOf(0x00)
        }

        ledCharacteristic?.let {
            it.value = command
            gatt?.writeCharacteristic(it)
        }
    }

    private fun BluetoothGattCharacteristic.getCCCD(): BluetoothGattDescriptor? {
        return getDescriptor(CCCD_UUID)
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications() {
        val gatt = gatt ?: return
        listOf(button1Characteristic, button3Characteristic).forEach { charac ->
            charac?.let {
                gatt.setCharacteristicNotification(it, true)
                val desc = it.getCCCD()
                desc?.let { d ->
                    d.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(d)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun disableNotifications() {
        val gatt = gatt ?: return
        listOf(button1Characteristic, button3Characteristic).forEach { charac ->
            charac?.let {
                gatt.setCharacteristicNotification(it, false)
                val desc = it.getCCCD()
                desc?.let { d ->
                    d.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(d)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }
}
