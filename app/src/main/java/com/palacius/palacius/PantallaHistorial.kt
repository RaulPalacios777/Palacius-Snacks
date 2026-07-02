package com.palacius.palacius

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palacius.palacius.data.*
import com.palacius.palacius.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PantallaHistorial(ordenDao: OrdenDao, egresoDao: EgresoDao) {
    val context = LocalContext.current
    val formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))
    val formatoHora = SimpleDateFormat("HH:mm a", Locale.getDefault())
    val scope = rememberCoroutineScope()

    val ventas by ordenDao.obtenerVentasActivas().collectAsState(initial = emptyList<OrdenEntity>())
    val egresos by egresoDao.obtenerEgresosActivos().collectAsState(initial = emptyList<EgresoEntity>())

    val compartidos = context.getSharedPreferences("PalaciusPrefs", Context.MODE_PRIVATE)
    val macImpresora = compartidos.getString("mac_impresora", "5A:4A:11:6D:00:03") ?: "5A:4A:11:6D:00:03"
    val printer = remember(macImpresora) { TicketPrinter(context, macImpresora) }

    val ventasEfectivo = ventas.filter { it.metodoPago.contains("Efectivo", ignoreCase = true) }.sumOf { it.total }
    val ventasTarjeta = ventas.filter { it.metodoPago.contains("Tarjeta", ignoreCase = true) }.sumOf { it.total }
    val ventasTransf = ventas.filter { it.metodoPago.contains("Transf", ignoreCase = true) }.sumOf { it.total }
    val totalIngresos = ventas.sumOf { it.total }
    val totalEgresos = egresos.sumOf { it.total }
    val efectivoFisicoEnCaja = ventasEfectivo - totalEgresos
    val netoRaul = ventas.sumOf { it.totalRaul } - egresos.filter { it.cajaDestino.contains("Palacius") }.sumOf { it.total } - (egresos.filter { it.cajaDestino.contains("Compartido") }.sumOf { it.total } / 2)
    val netoCristian = ventas.sumOf { it.totalCristian } - egresos.filter { it.cajaDestino.contains("Frappés") }.sumOf { it.total } - (egresos.filter { it.cajaDestino.contains("Compartido") }.sumOf { it.total } / 2)

    val historialUnificado = (ventas.map { HistorialItem.Venta(it) } + egresos.map { HistorialItem.Gasto(it) })
        .sortedByDescending { it.fechaHora }

    var mostrarConfirmacionDiaria by remember { mutableStateOf(false) }
    var mostrarConfirmacionSemanal by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Finanzas de la Semana", color = PalaciusPrimaryMustard, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { mostrarConfirmacionDiaria = true }, colors = ButtonDefaults.buttonColors(containerColor = PalaciusSurfaceDark)) {
                    Text("Cerrar Día", color = PalaciusTextLight)
                }
                Button(onClick = { mostrarConfirmacionSemanal = true }, colors = ButtonDefaults.buttonColors(containerColor = PalaciusPrimaryMustard)) {
                    Text("CORTE SEMANAL", color = PalaciusBackgroundDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1.5f).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2E7D32)).padding(16.dp)) {
                Text("Caja Registradora (Físico)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(formatoMoneda.format(efectivoFisicoEnCaja), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Cobrado: ${formatoMoneda.format(ventasEfectivo)}\nGastado: -${formatoMoneda.format(totalEgresos)}", color = Color(0xFFA5D6A7), fontSize = 10.sp, lineHeight = 14.sp)
            }
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(PalaciusSurfaceDark).padding(16.dp)) {
                Text("📱 Transferencias", color = PalaciusTextMuted, fontSize = 12.sp)
                Text(formatoMoneda.format(ventasTransf), color = PalaciusTextLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(PalaciusSurfaceDark).padding(16.dp)) {
                Text("💳 Tarjeta", color = PalaciusTextMuted, fontSize = 12.sp)
                Text(formatoMoneda.format(ventasTarjeta), color = PalaciusTextLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(PalaciusBackgroundDark).padding(16.dp)) {
                Text("Total Ventas", color = PalaciusSecondaryGold, fontSize = 12.sp)
                Text(formatoMoneda.format(totalIngresos), color = PalaciusPrimaryMustard, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Historial Tipo Libreta", color = PalaciusSecondaryGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (historialUnificado.isEmpty()) {
                item { Text("La libreta está en blanco. No hay movimientos esta semana.", color = PalaciusTextMuted, modifier = Modifier.padding(16.dp)) }
            } else {
                items(historialUnificado) { item ->
                    when (item) {
                        is HistorialItem.Venta -> {
                            TarjetaLibretaVenta(
                                orden = item.orden,
                                formatoMoneda = formatoMoneda,
                                formatoHora = formatoHora,
                                onImprimir = {
                                    scope.launch(Dispatchers.IO) {
                                        printer.imprimirTicketOrden(item.orden)
                                    }
                                }
                            )
                        }
                        is HistorialItem.Gasto -> TarjetaLibretaGasto(item.egreso, formatoMoneda, formatoHora)
                    }
                }
            }
        }
    }

    if (mostrarConfirmacionDiaria) {
        AlertDialog(onDismissRequest = { mostrarConfirmacionDiaria = false }, containerColor = PalaciusSurfaceDark,
            title = { Text("¿Cerrar labores de hoy?", color = PalaciusPrimaryMustard) },
            text = { Text("Esto agrupará los tickets de hoy, pero el dinero seguirá acumulándose en la caja para la semana.", color = PalaciusTextLight) },
            confirmButton = {
                Button(onClick = { scope.launch { ordenDao.cerrarTurnoDiario(); egresoDao.cerrarTurnoDiario() }; mostrarConfirmacionDiaria = false }, colors = ButtonDefaults.buttonColors(containerColor = PalaciusPrimaryMustard)) { Text("Sí, cerrar día", color = PalaciusBackgroundDark) }
            }, dismissButton = { TextButton(onClick = { mostrarConfirmacionDiaria = false }) { Text("Cancelar", color = PalaciusTextLight) } }
        )
    }

    if (mostrarConfirmacionSemanal) {
        AlertDialog(onDismissRequest = { mostrarConfirmacionSemanal = false }, containerColor = PalaciusSurfaceDark,
            title = { Text("⚠️ CORTE SEMANAL CONTABLE", color = PalaciusPrimaryMustard, fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de cerrar la semana?\n\nAl confirmar, se aislarán los datos para los reportes. Próximamente, el sistema descontará automáticamente el Fondo de Reinversión para tus compras de insumos, separando las utilidades netas.\n\nUtilidades estimadas actuales:\n- Raúl: ${formatoMoneda.format(netoRaul)}\n- Cristian: ${formatoMoneda.format(netoCristian)}", color = PalaciusTextLight) },
            confirmButton = {
                Button(onClick = { scope.launch { ordenDao.cerrarSemana(); egresoDao.cerrarSemana() }; mostrarConfirmacionSemanal = false }, colors = ButtonDefaults.buttonColors(containerColor = PalaciusPrimaryMustard)) {
                    Text("CERRAR SEMANA", color = PalaciusBackgroundDark, fontWeight = FontWeight.Bold)
                }
            }, dismissButton = { TextButton(onClick = { mostrarConfirmacionSemanal = false }) { Text("Cancelar", color = PalaciusTextLight) } }
        )
    }
}

sealed class HistorialItem(val fechaHora: Long) {
    data class Venta(val orden: OrdenEntity) : HistorialItem(orden.fechaHora)
    data class Gasto(val egreso: EgresoEntity) : HistorialItem(egreso.fechaHora)
}

@Composable
fun TarjetaLibretaVenta(orden: OrdenEntity, formatoMoneda: NumberFormat, formatoHora: SimpleDateFormat, onImprimir: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF1B4D3E)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(orden.identificador, color = PalaciusTextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(formatoHora.format(Date(orden.fechaHora)), color = Color(0xFFA5D6A7), fontSize = 12.sp)
            }
            Button(onClick = onImprimir, colors = ButtonDefaults.buttonColors(containerColor = PalaciusPrimaryMustard)) {
                Text("Imprimir", color = PalaciusBackgroundDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text("--------------------------------------------------", color = Color(0xFF2E7D32), maxLines = 1)

        val productos = orden.resumenProductos.split("\n").filter { it.isNotBlank() }
        productos.forEach { linea ->
            val limpia = linea.removePrefix("+").trim()
            val indicePrecio = limpia.lastIndexOf("$")

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (indicePrecio != -1) {
                    val precioStr = limpia.substring(indicePrecio).trim()
                    var descStr = limpia.substring(0, indicePrecio).trim()
                    if (descStr.endsWith("-")) {
                        descStr = descStr.substring(0, descStr.length - 1).trim()
                    }
                    Text("• $descStr", color = PalaciusTextLight, fontSize = 14.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    Text(precioStr, color = PalaciusSecondaryGold, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                } else {
                    Text("• $limpia", color = PalaciusTextLight, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Text("--------------------------------------------------", color = Color(0xFF2E7D32), maxLines = 1)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOTAL COBRADO (${orden.metodoPago}):", color = PalaciusSecondaryGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(formatoMoneda.format(orden.total), color = Color(0xFF81C784), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TarjetaLibretaGasto(egreso: EgresoEntity, formatoMoneda: NumberFormat, formatoHora: SimpleDateFormat) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF3E2723)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SALIDA DE DINERO (COMPRA)", color = Color(0xFFE57373), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(formatoHora.format(Date(egreso.fechaHora)), color = PalaciusTextMuted, fontSize = 12.sp)
        }
        Text("--------------------------------------------------", color = Color(0xFF5D4037), maxLines = 1)
        Text("- ${egreso.cantidad}x ${egreso.concepto} (\$${egreso.costoUnitario} c/u)", color = PalaciusTextLight, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text("  (Descontado de: ${egreso.cajaDestino})", color = PalaciusTextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text("--------------------------------------------------", color = Color(0xFF5D4037), maxLines = 1)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOTAL PAGADO EN EFECTIVO:", color = Color(0xFFE57373), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("-${formatoMoneda.format(egreso.total)}", color = Color(0xFFE57373), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}