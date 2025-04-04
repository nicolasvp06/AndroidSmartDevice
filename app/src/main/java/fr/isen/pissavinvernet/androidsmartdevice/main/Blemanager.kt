package fr.isen.pissavinvernet.androidsmartdevice.main

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat

data class BleDevice(
    val name: String?,
    val address: String?,
    val device: android.bluetooth.BluetoothDevice
)

class BleManager(private val context: Context) {
    val devices = mutableStateListOf<BleDevice>()

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    private val scanPeriod = 10000L // 10 secondes

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isScanning) return
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) return

        devices.clear()
        isScanning = true
        scanner?.startScan(scanCallback)

        handler.postDelayed({
            stopScan()
        }, scanPeriod)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) return

        isScanning = false
        scanner?.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device

            val name = if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                device.name
            } else {
                null
            }

            if (devices.none { it.address == device.address }) {
                val bleDevice = BleDevice(
                    name = name ?: "Appareil inconnu",
                    address = device.address,
                    device = device
                )
                devices.add(bleDevice)

                // Tri avec priorité :
                // 1. Appareils nommés "labo"
                // 2. Appareils avec nom connu
                // 3. Appareils avec adresse terminant par 6F:6E
                devices.sortWith(
                    compareByDescending<BleDevice> {
                        it.name?.lowercase()?.contains("labo") == true
                    }.thenByDescending {
                        it.name != null && it.name.lowercase() != "appareil inconnu"
                    }.thenByDescending {
                        it.address?.uppercase()?.endsWith("6F:6E") == true
                    }
                )
            }
        }
    }
}
