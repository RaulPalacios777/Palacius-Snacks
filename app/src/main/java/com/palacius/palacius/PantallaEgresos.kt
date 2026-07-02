package com.palacius.palacius

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palacius.palacius.data.EgresoDao
import com.palacius.palacius.data.EgresoEntity
import com.palacius.palacius.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PantallaEgresos(egresoDao: EgresoDao) {
    val formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))
    val scope = rememberCoroutineScope()

    val egresosDelDia by egresoDao.obtenerEgresosDelDia().collectAsState(initial = emptyList<EgresoEntity>())

    var concepto by remember { mutableStateOf("") }
    var cantidad by remember { mutableStateOf("1") }
    var costoUnitario by remember { mutableStateOf("") }

    val opcionesCaja = listOf("Palacius (Snacks)", "Frappés / Postres", "Gasto Compartido (50/50)")
    var cajaSeleccionada by remember { mutableStateOf(opcionesCaja[0]) }

    val cantidadLimpia = cantidad.trim()
    val cantidadNum = cantidadLimpia.toIntOrNull() ?: 1
    val costoLimpio = costoUnitario.replace(",", ".").replace("$", "").trim()
    val costoNum = costoLimpio.toDoubleOrNull() ?: 0.0

    // Matemática pura: Si es 50/50, lo dividimos entre 2 desde aquí
    val totalGastoBruto = cantidadNum * costoNum
    val totalARestar = if (cajaSeleccionada == "Gasto Compartido (50/50)") totalGastoBruto / 2 else totalGastoBruto

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Registro de Gastos", color = PalaciusPrimaryMustard, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Insumos, pagos a proveedores o salidas de efectivo.", color = PalaciusTextMuted, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PalaciusSurfaceDark).padding(16.dp)) {
            OutlinedTextField(
                value = concepto, onValueChange = { concepto = it }, label = { Text("¿Qué se compró?", color = PalaciusTextMuted) },
                modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PalaciusTextLight, unfocusedTextColor = PalaciusTextLight, focusedBorderColor = PalaciusPrimaryMustard)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = cantidad, onValueChange = { cantidad = it }, label = { Text("Cantidad", color = PalaciusTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PalaciusTextLight, unfocusedTextColor = PalaciusTextLight)
                )
                OutlinedTextField(
                    value = costoUnitario, onValueChange = { costoUnitario = it }, label = { Text("Costo x Unidad ($)", color = PalaciusTextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1.5f),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PalaciusTextLight, unfocusedTextColor = PalaciusTextLight)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("¿A qué caja se descuenta?", color = PalaciusSecondaryGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                opcionesCaja.forEach { opcion ->
                    Button(
                        onClick = { cajaSeleccionada = opcion }, modifier = Modifier.weight(1f).height(45.dp), contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (cajaSeleccionada == opcion) PalaciusPrimaryMustard else PalaciusBackgroundDark)
                    ) { Text(opcion, color = if (cajaSeleccionada == opcion) PalaciusBackgroundDark else PalaciusTextLight, fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Total a restar de caja:", color = PalaciusTextLight, fontSize = 14.sp)
                    // Aquí ya mostramos el precio dividido visualmente
                    Text(formatoMoneda.format(totalARestar), color = Color(0xFFE57373), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (concepto.isNotBlank() && totalARestar > 0) {
                            scope.launch {
                                // Guardamos el precio ya dividido en la base de datos
                                egresoDao.guardarEgreso(
                                    EgresoEntity(concepto = concepto.trim(), cantidad = cantidadNum, costoUnitario = costoNum, total = totalARestar, cajaDestino = cajaSeleccionada)
                                )
                                concepto = ""
                                cantidad = "1"
                                costoUnitario = ""
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp).width(150.dp), colors = ButtonDefaults.buttonColors(containerColor = PalaciusPrimaryMustard)
                ) { Text("Registrar", color = PalaciusBackgroundDark, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Gastos registrados hoy:", color = PalaciusSecondaryGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (egresosDelDia.isEmpty()) {
                item { Text("No se han registrado gastos hoy.", color = PalaciusTextMuted) }
            } else {
                items(egresosDelDia) { egreso ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PalaciusSurfaceDark).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("${egreso.cantidad}x ${egreso.concepto}", color = PalaciusTextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(egreso.cajaDestino, color = if (egreso.cajaDestino.contains("Palacius")) PalaciusTextMuted else PalaciusSecondaryGold, fontSize = 12.sp)
                        }
                        Text("-${formatoMoneda.format(egreso.total)}", color = Color(0xFFE57373), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}