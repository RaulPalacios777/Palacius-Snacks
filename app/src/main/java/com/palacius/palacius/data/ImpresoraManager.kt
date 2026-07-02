package com.palacius.palacius.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice

// Función rápida para listar dispositivos vinculados y ver si tu SUZWIP está ahí
fun obtenerImpresorasVinculadas(): List<BluetoothDevice> {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    return bluetoothAdapter.bondedDevices.filter {
        // Filtramos por dispositivos de audio/impresión o simplemente listamos todos
        it.bluetoothClass.majorDeviceClass == 1536 || it.name.contains("POS")
    }
}