package com.palacius.palacius.data

import android.bluetooth.BluetoothAdapter

fun imprimirDireccionesBluetooth() {
    val adapter = BluetoothAdapter.getDefaultAdapter()
    val devices = adapter.bondedDevices
    for (device in devices) {
        // Esto imprimirá en la consola de Android Studio (Logcat) los nombres y direcciones
        println("Dispositivo: ${device.name} - MAC: ${device.address}")
    }
}