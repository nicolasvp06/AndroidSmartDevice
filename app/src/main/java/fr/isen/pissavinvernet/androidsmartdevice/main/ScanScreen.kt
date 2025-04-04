package fr.isen.pissavinvernet.androidsmartdevice.main

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.isen.pissavinvernet.androidsmartdevice.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val REQUEST_ENABLE_BT = 1

fun getSignalLevel(rssi: Int): Int {
    return when {
        rssi >= -60 -> 4
        rssi >= -70 -> 3
        rssi >= -80 -> 2
        rssi >= -90 -> 1
        else -> 0
    }
}

@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val isScanning = remember { mutableStateOf(false) }
    val devicesWithRssi = remember { mutableStateMapOf<BluetoothDevice, Int>() }
    val coroutineScope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }

    val showBluetoothWarning = remember { mutableStateOf(false) }
    val warningMessage = remember { mutableStateOf("") }
    val bluetoothEnabled = remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    LaunchedEffect(isScanning.value) {
        if (isScanning.value) {
            coroutineScope.launch {
                while (isScanning.value) {
                    scale.animateTo(1.5f)
                    delay(300)
                    scale.animateTo(1f)
                    delay(300)
                }
            }
        } else {
            scale.snapTo(1f)
        }
    }

    val scanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                result.device?.let { device ->
                    devicesWithRssi[device] = result.rssi
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text("Scan BLE", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))

        bluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        val bluetoothStatus = if (bluetoothEnabled.value) "Bluetooth activé" else "Bluetooth désactivé"
        Text(bluetoothStatus, fontSize = 16.sp, color = if (bluetoothEnabled.value) Color.Green else Color.Red)

        Spacer(modifier = Modifier.height(8.dp))
        Text(if (isScanning.value) "Scan en cours..." else "Scan inactif", fontSize = 14.sp)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_blutooth),
                contentDescription = "Bluetooth Logo",
                modifier = Modifier.size((64 * scale.value).dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {
                if (!bluetoothAdapter.isEnabled) {
                    warningMessage.value = "Veuillez activer le Bluetooth"
                    showBluetoothWarning.value = true
                    coroutineScope.launch {
                        delay(3000)
                        showBluetoothWarning.value = false
                    }
                    return@Button
                }

                if (isScanning.value) {
                    bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
                } else {
                    devicesWithRssi.clear()
                    bluetoothAdapter.bluetoothLeScanner?.startScan(scanCallback)
                    coroutineScope.launch {
                        delay(10000)
                        bluetoothAdapter.bluetoothLeScanner?.stopScan(scanCallback)
                        isScanning.value = false
                    }
                }
                isScanning.value = !isScanning.value
            }) {
                Text(if (isScanning.value) "Arrêter le scan" else "Démarrer le scan")
            }

            Button(onClick = {
                (context as? Activity)?.finish()
            }) {
                Text("Retour Accueil")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showBluetoothWarning.value) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFE0E0))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(warningMessage.value, color = Color.Red)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tri personnalisé
        val sortedDevices = devicesWithRssi.keys.sortedWith(
            compareBy(
                { !it.address.endsWith("6F:6E", true) }, // Priorité 1
                { !(it.name?.contains("Labo", true) == true) }, // Priorité 2
                { it.name.isNullOrEmpty() } // Priorité 3
            )
        )

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(sortedDevices) { device ->
                val isSpecial = device.address.endsWith("6F:6E", true)
                val backgroundColor = if (isSpecial) Color(0xFFDFFFE0) else MaterialTheme.colorScheme.surface
                val borderColor = if (isSpecial) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                val rssi = devicesWithRssi[device] ?: -100
                val signalLevel = getSignalLevel(rssi)

                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .clickable {
                            val intent = Intent(context, DeviceActivity::class.java)
                            intent.putExtra("device_address", device.address)
                            context.startActivity(intent)
                        }
                        .border(2.dp, borderColor, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name ?: "Appareil inconnu", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(device.address, fontSize = 14.sp, color = Color.Gray)
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            repeat(4) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(start = 2.dp)
                                        .size(width = 4.dp, height = (6 + index * 6).dp)
                                        .background(if (index < signalLevel) Color.Green else Color.LightGray)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
