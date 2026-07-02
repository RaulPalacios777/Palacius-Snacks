package com.palacius.palacius

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.palacius.palacius.data.OrdenRepository
import com.palacius.palacius.data.PalaciusDatabase
import com.palacius.palacius.ui.theme.PalaciusTheme

class MainActivity : ComponentActivity() {

    private val permisosBluetoothLauncher =
        registerForActivityResult(
            ActivityResultContracts
                .RequestMultiplePermissions()
        ) { resultados ->

            if (
                Build.VERSION.SDK_INT <
                Build.VERSION_CODES.S
            ) {
                return@registerForActivityResult
            }

            val permisoConnectConcedido =
                resultados[
                    Manifest.permission
                        .BLUETOOTH_CONNECT
                ] ?: tienePermiso(
                    Manifest.permission
                        .BLUETOOTH_CONNECT
                )

            val permisoScanConcedido =
                resultados[
                    Manifest.permission
                        .BLUETOOTH_SCAN
                ] ?: tienePermiso(
                    Manifest.permission
                        .BLUETOOTH_SCAN
                )

            if (
                !permisoConnectConcedido ||
                !permisoScanConcedido
            ) {
                Toast.makeText(
                    this,
                    "Debes permitir Dispositivos cercanos para imprimir tickets.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        solicitarPermisosBluetooth()

        val baseDeDatos =
            PalaciusDatabase
                .obtenerInstancia(this)

        val ordenDao =
            baseDeDatos.ordenDao()

        val ordenDetalleDao =
            baseDeDatos.ordenDetalleDao()

        val egresoDao =
            baseDeDatos.egresoDao()

        val menuDao =
            baseDeDatos.menuDao()

        /*
         * El repositorio coordina la transacción
         * entre OrdenDao y OrdenDetalleDao.
         */
        val ordenRepository =
            OrdenRepository(
                database = baseDeDatos,
                ordenDao = ordenDao,
                ordenDetalleDao =
                    ordenDetalleDao
            )

        setContent {
            PalaciusTheme {
                Surface(
                    modifier =
                        Modifier.fillMaxSize(),
                    color =
                        MaterialTheme
                            .colorScheme
                            .background
                ) {
                    MainDashboard(
                        ordenDao = ordenDao,
                        ordenRepository =
                            ordenRepository,
                        egresoDao = egresoDao,
                        menuDao = menuDao
                    )
                }
            }
        }
    }

    private fun solicitarPermisosBluetooth() {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.S
        ) {
            return
        }

        val permisosPendientes =
            mutableListOf<String>()

        if (
            !tienePermiso(
                Manifest.permission
                    .BLUETOOTH_CONNECT
            )
        ) {
            permisosPendientes.add(
                Manifest.permission
                    .BLUETOOTH_CONNECT
            )
        }

        if (
            !tienePermiso(
                Manifest.permission
                    .BLUETOOTH_SCAN
            )
        ) {
            permisosPendientes.add(
                Manifest.permission
                    .BLUETOOTH_SCAN
            )
        }

        if (permisosPendientes.isNotEmpty()) {
            permisosBluetoothLauncher.launch(
                permisosPendientes
                    .toTypedArray()
            )
        }
    }

    private fun tienePermiso(
        permiso: String
    ): Boolean {

        return ContextCompat
            .checkSelfPermission(
                this,
                permiso
            ) == PackageManager
            .PERMISSION_GRANTED
    }
}