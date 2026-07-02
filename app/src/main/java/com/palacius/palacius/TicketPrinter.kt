package com.palacius.palacius

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.palacius.palacius.data.OrdenEntity
import java.util.Locale

sealed class ResultadoImpresion {

    object Exito : ResultadoImpresion()

    data class Error(
        val mensaje: String,
        val causa: Throwable? = null
    ) : ResultadoImpresion()
}

class TicketPrinter(
    context: Context,
    macAddress: String
) {

    private val appContext = context.applicationContext

    /*
     * Se limpia la dirección para evitar errores por
     * espacios o letras minúsculas.
     */
    private val macImpresora = macAddress
        .trim()
        .uppercase(Locale.ROOT)

    companion object {
        private const val TAG = "PALACIUS_PRINTER"

        /*
         * Para impresora térmica de 58 mm.
         */
        private const val ANCHO_CARACTERES = 32
        private const val DPI_IMPRESORA = 203
        private const val ANCHO_IMPRESION_MM = 48f
    }

    /**
     * Intenta imprimir una orden utilizando exactamente
     * la dirección MAC configurada en Palacius POS.
     */
    fun imprimirTicketOrden(
        orden: OrdenEntity
    ): ResultadoImpresion {

        var printer: EscPosPrinter? = null

        try {
            Log.d(
                TAG,
                "Iniciando impresión. Orden=${orden.id}, MAC=$macImpresora"
            )

            if (macImpresora.isBlank()) {
                return error(
                    "No existe una dirección MAC configurada."
                )
            }

            if (!BluetoothAdapter.checkBluetoothAddress(macImpresora)) {
                return error(
                    "La dirección MAC no tiene un formato válido: $macImpresora"
                )
            }

            /*
 * Android 12 o superior necesita los permisos
 * BLUETOOTH_CONNECT y BLUETOOTH_SCAN.
 *
 * La librería DantSu ejecuta cancelDiscovery()
 * antes de abrir la conexión con la impresora.
 */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                val permisoConnectConcedido =
                    ContextCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED

                if (!permisoConnectConcedido) {
                    return error(
                        "La aplicación no tiene permiso BLUETOOTH_CONNECT."
                    )
                }

                val permisoScanConcedido =
                    ContextCompat.checkSelfPermission(
                        appContext,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED

                if (!permisoScanConcedido) {
                    return error(
                        "La aplicación no tiene permiso BLUETOOTH_SCAN."
                    )
                }
            }

            val bluetoothManager =
                appContext.getSystemService(
                    Context.BLUETOOTH_SERVICE
                ) as? BluetoothManager
                    ?: return error(
                        "No se pudo acceder al servicio Bluetooth."
                    )

            val bluetoothAdapter =
                bluetoothManager.adapter
                    ?: return error(
                        "Este dispositivo no tiene adaptador Bluetooth."
                    )

            if (!bluetoothAdapter.isEnabled) {
                return error(
                    "El Bluetooth está apagado."
                )
            }

            /*
             * Aquí sí utilizamos exactamente la MAC configurada.
             */
            val dispositivo =
                bluetoothAdapter.getRemoteDevice(macImpresora)

            if (dispositivo.bondState != BluetoothDevice.BOND_BONDED) {
                return error(
                    "La impresora no está vinculada con el dispositivo."
                )
            }

            Log.d(
                TAG,
                "Dispositivo encontrado: ${dispositivo.name ?: "Sin nombre"}"
            )

            val conexion = BluetoothConnection(dispositivo)

            printer = EscPosPrinter(
                conexion,
                DPI_IMPRESORA,
                ANCHO_IMPRESION_MM,
                ANCHO_CARACTERES
            )

            val ticket = construirTicket(orden)

            Log.d(TAG, "Contenido que se enviará:\n$ticket")

            printer.printFormattedText(ticket)

            Log.d(
                TAG,
                "Impresión terminada correctamente."
            )

            return ResultadoImpresion.Exito

        } catch (securityException: SecurityException) {
            return error(
                mensaje = "Android bloqueó el acceso Bluetooth por falta de permisos.",
                causa = securityException
            )

        } catch (exception: Exception) {
            return error(
                mensaje = exception.message
                    ?: "Ocurrió un error desconocido al imprimir.",
                causa = exception
            )

        } finally {
            try {
                printer?.disconnectPrinter()

                Log.d(
                    TAG,
                    "Conexión con la impresora cerrada."
                )
            } catch (exception: Exception) {
                Log.e(
                    TAG,
                    "No se pudo cerrar la conexión.",
                    exception
                )
            }
        }
    }

    /**
     * Construye el ticket con el formato temporal actual.
     *
     * Más adelante será reemplazado para utilizar
     * OrdenArticuloEntity y OrdenArticuloExtraEntity.
     */
    private fun construirTicket(
        orden: OrdenEntity
    ): String {

        val identificador =
            limpiarTexto(orden.identificador)
                .ifBlank { "Venta" }

        val metodoPago =
            limpiarTexto(orden.metodoPago)
                .ifBlank { "No especificado" }

        val productos =
            orden.resumenProductos
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString("\n") { linea ->
                    imprimirProducto(linea)
                }

        val total =
            String.format(
                Locale.US,
                "\$%.2f",
                orden.total
            )

        return buildString {
            appendLine("[L]--------------------------------")
            appendLine("[C]<b>PALACIUS SNACKS</b>")
            appendLine("[C]Zempoala, Hidalgo")
            appendLine("[L]--------------------------------")

            appendLine("[L]Orden:")

            dividirLinea(
                identificador,
                ANCHO_CARACTERES
            ).forEach { linea ->
                appendLine("[L]$linea")
            }

            appendLine("[L]--------------------------------")

            if (productos.isBlank()) {
                appendLine("[L]Sin productos registrados")
            } else {
                appendLine(productos)
            }

            appendLine("[L]--------------------------------")
            appendLine("[L]<b>TOTAL:</b>[R]<b>$total</b>")
            appendLine("[L]Pago:[R]$metodoPago")
            appendLine("[L]--------------------------------")
            appendLine("[C]Gracias por su compra")
            appendLine("[L]--------------------------------")
            repeat(5) {
                appendLine()
            }

            /*
             * Espacio para poder cortar el papel.
             */
            appendLine()
            appendLine()
            appendLine()
        }
    }

    /**
     * Intenta separar la descripción y el precio
     * respetando las 32 columnas de la impresora.
     */
    private fun imprimirProducto(
        lineaOriginal: String
    ): String {

        val linea =
            limpiarTexto(
                lineaOriginal
            )

        val indicePrecio =
            linea.lastIndexOf("$")

        /*
         * Algunas órdenes antiguas no tienen
         * precio individual por producto.
         */
        if (indicePrecio == -1) {
            return dividirLinea(
                linea,
                ANCHO_CARACTERES
            ).joinToString("\n") { parte ->
                "[L]$parte"
            }
        }

        val precio =
            linea.substring(indicePrecio)
                .trim()

        val descripcion =
            linea.substring(
                startIndex = 0,
                endIndex = indicePrecio
            )
                .trim()
                .removeSuffix("-")
                .trim()

        /*
         * Se reserva espacio para mostrar el precio.
         */
        val anchoDescripcion =
            (
                    ANCHO_CARACTERES -
                            precio.length -
                            1
                    ).coerceAtLeast(8)

        val partes =
            dividirLinea(
                descripcion,
                anchoDescripcion
            )

        if (partes.isEmpty()) {
            return "[R]$precio"
        }

        return buildString {
            partes.forEachIndexed { index, parte ->

                if (index == 0) {
                    append("[L]")
                    append(parte)
                    append("[R]")
                    append(precio)
                } else {
                    appendLine()
                    append("[L]")
                    append(parte)
                }
            }
        }
    }

    /**
     * Evita que el contenido de una orden sea interpretado
     * como comandos de formato por la librería ESC/POS.
     */
    private fun limpiarTexto(
        texto: String
    ): String {

        return texto
            .replace("[", "(")
            .replace("]", ")")
            .replace("<", "(")
            .replace(">", ")")

            /*
             * La mayoría de impresoras económicas no pueden
             * representar correctamente estos emojis.
             */
            .replace("💵", "")
            .replace("💳", "")
            .replace("📱", "")
            .replace("🏷️", "")
            .replace("🏷", "")
            .replace("📝", "")

            .replace("\t", " ")
            .trim()
    }

    /**
     * Divide textos largos sin cortar palabras normales.
     */
    private fun dividirLinea(
        texto: String,
        ancho: Int
    ): List<String> {

        if (texto.isBlank()) {
            return emptyList()
        }

        val resultado =
            mutableListOf<String>()

        var lineaActual = ""

        texto
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .forEach { palabra ->

                /*
                 * Si una palabra por sí sola supera el ancho,
                 * se divide en fragmentos.
                 */
                if (palabra.length > ancho) {

                    if (lineaActual.isNotBlank()) {
                        resultado.add(lineaActual)
                        lineaActual = ""
                    }

                    palabra
                        .chunked(ancho)
                        .forEach { fragmento ->
                            resultado.add(fragmento)
                        }

                } else if (lineaActual.isBlank()) {
                    lineaActual = palabra

                } else if (
                    lineaActual.length +
                    palabra.length +
                    1 <= ancho
                ) {
                    lineaActual += " $palabra"

                } else {
                    resultado.add(lineaActual)
                    lineaActual = palabra
                }
            }

        if (lineaActual.isNotBlank()) {
            resultado.add(lineaActual)
        }

        return resultado
    }

    private fun error(
        mensaje: String,
        causa: Throwable? = null
    ): ResultadoImpresion.Error {

        if (causa == null) {
            Log.e(TAG, mensaje)
        } else {
            Log.e(TAG, mensaje, causa)
        }

        return ResultadoImpresion.Error(
            mensaje = mensaje,
            causa = causa
        )
    }
}