package fr.isen.pissavinvernet.androidsmartdevice.main

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.isen.pissavinvernet.androidsmartdevice.R
import fr.isen.pissavinvernet.androidsmartdevice.ui.theme.AndroidSmartDeviceTheme
import kotlinx.coroutines.*

class DeviceActivity : ComponentActivity() {
    private var gatt: BluetoothGatt? = null
    private var connectionJob: Job? = null
    private var deviceAddress: String? = null

    private val connectionStatus = mutableStateOf("Appareil non connecté")
    private val isConnecting = mutableStateOf(false)

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        deviceAddress = intent.getStringExtra("device_address")
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)

        setContent {
            AndroidSmartDeviceTheme {
                val scale = remember { Animatable(1f) }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(isConnecting.value) {
                    if (isConnecting.value) {
                        coroutineScope.launch {
                            while (isConnecting.value) {
                                scale.animateTo(1.3f)
                                delay(300)
                                scale.animateTo(1f)
                                delay(300)
                            }
                        }
                    } else {
                        scale.snapTo(1f)
                    }
                }

                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Connexion à l'appareil",
                            style = MaterialTheme.typography.headlineMedium,
                            fontSize = 24.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Statut : ${connectionStatus.value}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 18.sp
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Image(
                            painter = painterResource(id = R.drawable.ic_blutooth),
                            contentDescription = "Bluetooth Icon",
                            modifier = Modifier.size((90 * scale.value).dp)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                isConnecting.value = true
                                connectionStatus.value = "Connexion en cours..."
                                connectToDevice(device)
                                startConnectionTimeout()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connexion Bluetooth")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                disconnectFromDevice()
                                connectionStatus.value = "Déconnecté"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Déconnexion Bluetooth")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { finish() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                        ) {
                            Text("Retour Accueil")
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        gatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                runOnUiThread {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        cancelConnectionTimeout()
                        isConnecting.value = false
                        connectionStatus.value = "Connecté à ${device.name ?: "appareil"}"
                        gatt?.discoverServices()

                        // ✅ Rediriger vers DeviceConnectedActivity
                        val intent = Intent(this@DeviceActivity, DeviceConnectedActivity::class.java)
                        intent.putExtra("device_address", device.address)
                        startActivity(intent)
                        finish()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        cancelConnectionTimeout()
                        isConnecting.value = false
                        connectionStatus.value = "Déconnecté"
                    }
                }
            }
        })
    }

    private fun startConnectionTimeout() {
        connectionJob = CoroutineScope(Dispatchers.Main).launch {
            delay(6000)
            if (isConnecting.value) {
                isConnecting.value = false
                connectionStatus.value = "Connexion échouée"
                disconnectFromDevice()
            }
        }
    }

    private fun cancelConnectionTimeout() {
        connectionJob?.cancel()
    }

    @SuppressLint("MissingPermission")
    private fun disconnectFromDevice() {
        gatt?.disconnect()
        gatt?.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectFromDevice()
        cancelConnectionTimeout()
    }
}
