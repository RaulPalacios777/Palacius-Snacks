package com.palacius.palacius

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palacius.palacius.data.OrdenDao
import com.palacius.palacius.data.OrdenEntity
import com.palacius.palacius.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PantallaOrdenes(
    ordenDao: OrdenDao
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val formatoMoneda = remember {
        NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-MX")
        )
    }

    val formatoFecha = remember {
        SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        )
    }

    val ordenesPendientes by
    ordenDao
        .obtenerOrdenesPendientes()
        .collectAsState(
            initial = emptyList()
        )

    val preferencias = remember(context) {
        context.getSharedPreferences(
            "PalaciusPrefs",
            Context.MODE_PRIVATE
        )
    }

    val macImpresora =
        preferencias.getString(
            "mac_impresora",
            "5A:4A:11:6D:00:03"
        ) ?: "5A:4A:11:6D:00:03"

    val printer = remember(
        context,
        macImpresora
    ) {
        TicketPrinter(
            context = context,
            macAddress = macImpresora
        )
    }

    var ordenSeleccionada by remember {
        mutableStateOf<OrdenEntity?>(null)
    }

    var ordenPendienteEliminar by remember {
        mutableStateOf<OrdenEntity?>(null)
    }

    var ordenPendienteCobrar by remember {
        mutableStateOf<OrdenEntity?>(null)
    }

    var procesandoOrden by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Órdenes Activas",
                    color =
                        PalaciusPrimaryMustard,
                    fontSize = 24.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "Pedidos pendientes de cobro",
                    color =
                        PalaciusTextMuted,
                    fontSize = 12.sp
                )
            }

            Text(
                text =
                    "${ordenesPendientes.size} pendientes",
                color =
                    PalaciusTextLight,
                fontSize = 16.sp,
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(8.dp)
                    )
                    .background(
                        PalaciusSurfaceDark
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    )
            )
        }

        if (ordenesPendientes.isEmpty()) {
            Box(
                modifier =
                    Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center
            ) {
                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✓",
                        color =
                            Color(0xFF81C784),
                        fontSize = 48.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "No hay órdenes pendientes.",
                        color =
                            PalaciusTextLight,
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "Las nuevas órdenes aparecerán aquí.",
                        color =
                            PalaciusTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement =
                    Arrangement.spacedBy(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(16.dp),
                modifier =
                    Modifier.fillMaxSize()
            ) {
                items(
                    items = ordenesPendientes,
                    key = { orden ->
                        orden.id
                    }
                ) { orden ->

                    TarjetaOrdenPendiente(
                        orden = orden,
                        formatoMoneda =
                            formatoMoneda,
                        formatoFecha =
                            formatoFecha,
                        habilitada =
                            !procesandoOrden,
                        onClick = {
                            if (!procesandoOrden) {
                                ordenSeleccionada =
                                    orden
                            }
                        }
                    )
                }
            }
        }
    }

    /*
     * DETALLE DE LA ORDEN.
     *
     * Es de solo lectura para proteger cantidades,
     * precios y reparto financiero.
     */
    ordenSeleccionada?.let { orden ->

        AlertDialog(
            onDismissRequest = {
                if (!procesandoOrden) {
                    ordenSeleccionada = null
                }
            },
            containerColor =
                PalaciusSurfaceDark,

            title = {
                Column {
                    Text(
                        text =
                            orden.identificador,
                        color =
                            PalaciusPrimaryMustard,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize = 20.sp
                    )

                    Text(
                        text =
                            formatoFecha.format(
                                Date(orden.fechaHora)
                            ),
                        color =
                            PalaciusTextMuted,
                        fontSize = 12.sp
                    )
                }
            },

            text = {
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    Text(
                        text =
                            "Desglose del pedido",
                        color =
                            PalaciusSecondaryGold,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(
                                max = 300.dp
                            )
                    ) {
                        ResumenOrdenPendiente(
                            resumen =
                                orden.resumenProductos,
                            formatoMoneda =
                                formatoMoneda
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    HorizontalDivider(
                        color =
                            PalaciusDivider
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total",
                            color =
                                PalaciusTextLight,
                            fontSize = 20.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                formatoMoneda.format(
                                    orden.total
                                ),
                            color =
                                PalaciusPrimaryMustard,
                            fontSize = 22.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "La edición de productos está temporalmente deshabilitada para proteger el total y el reparto financiero.",
                        color =
                            PalaciusTextMuted,
                        fontSize = 11.sp
                    )
                }
            },

            confirmButton = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        enabled =
                            !procesandoOrden,
                        onClick = {
                            ordenSeleccionada = null
                            ordenPendienteEliminar =
                                orden
                        },
                        modifier =
                            Modifier.weight(1f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Color(0xFF8B0000)
                            )
                    ) {
                        Text(
                            text = "Eliminar",
                            color =
                                Color.White
                        )
                    }

                    Button(
                        enabled =
                            !procesandoOrden,
                        onClick = {
                            ordenSeleccionada = null
                            ordenPendienteCobrar =
                                orden
                        },
                        modifier =
                            Modifier.weight(1.5f),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    PalaciusPrimaryMustard
                            )
                    ) {
                        Text(
                            text = "Cobrar",
                            color =
                                PalaciusBackgroundDark,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }
            },

            dismissButton = {
                TextButton(
                    enabled =
                        !procesandoOrden,
                    onClick = {
                        ordenSeleccionada = null
                    }
                ) {
                    Text(
                        text = "Cerrar",
                        color =
                            PalaciusTextLight
                    )
                }
            }
        )
    }

    /*
     * CONFIRMACIÓN PARA ELIMINAR.
     */
    ordenPendienteEliminar?.let { orden ->

        AlertDialog(
            onDismissRequest = {
                if (!procesandoOrden) {
                    ordenPendienteEliminar =
                        null
                }
            },
            containerColor =
                PalaciusSurfaceDark,

            title = {
                Text(
                    text = "Eliminar orden",
                    color =
                        Color(0xFFE57373),
                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {
                Column {
                    Text(
                        text =
                            "¿Deseas eliminar definitivamente esta orden pendiente?",
                        color =
                            PalaciusTextLight
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            orden.identificador,
                        color =
                            PalaciusSecondaryGold,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            formatoMoneda.format(
                                orden.total
                            ),
                        color =
                            PalaciusTextLight
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            "Esta acción también eliminará sus artículos y extras relacionados.",
                        color =
                            PalaciusTextMuted,
                        fontSize = 11.sp
                    )
                }
            },

            confirmButton = {
                Button(
                    enabled =
                        !procesandoOrden,
                    onClick = {
                        procesandoOrden = true

                        scope.launch {
                            try {
                                val filasEliminadas =
                                    ordenDao
                                        .eliminarOrden(
                                            orden
                                        )

                                if (
                                    filasEliminadas != 1
                                ) {
                                    error(
                                        "No se encontró la orden que intentabas eliminar."
                                    )
                                }

                                ordenPendienteEliminar =
                                    null

                                Toast.makeText(
                                    context,
                                    "Orden eliminada.",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (
                                exception: Exception
                            ) {
                                Toast.makeText(
                                    context,
                                    "No se pudo eliminar: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                            } finally {
                                procesandoOrden =
                                    false
                            }
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF8B0000)
                        )
                ) {
                    Text(
                        text =
                            if (procesandoOrden) {
                                "Eliminando..."
                            } else {
                                "Sí, eliminar"
                            },
                        color =
                            Color.White
                    )
                }
            },

            dismissButton = {
                TextButton(
                    enabled =
                        !procesandoOrden,
                    onClick = {
                        ordenPendienteEliminar =
                            null
                    }
                ) {
                    Text(
                        text = "Cancelar",
                        color =
                            PalaciusTextLight
                    )
                }
            }
        )
    }

    /*
     * COBRO DE ORDEN PENDIENTE.
     */
    ordenPendienteCobrar?.let { orden ->

        AlertDialog(
            onDismissRequest = {
                if (!procesandoOrden) {
                    ordenPendienteCobrar =
                        null
                }
            },
            containerColor =
                PalaciusSurfaceDark,

            title = {
                Text(
                    text = "Cerrar cuenta",
                    color =
                        PalaciusPrimaryMustard,
                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {
                Column {
                    Text(
                        text =
                            orden.identificador,
                        color =
                            PalaciusTextLight,
                        fontSize = 17.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Cobrar exactamente:",
                        color =
                            PalaciusTextMuted,
                        fontSize = 13.sp
                    )

                    Text(
                        text =
                            formatoMoneda.format(
                                orden.total
                            ),
                        color =
                            PalaciusPrimaryMustard,
                        fontSize = 26.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Selecciona el método de pago. La orden se guardará antes de intentar imprimir.",
                        color =
                            PalaciusTextMuted,
                        fontSize = 11.sp
                    )
                }
            },

            confirmButton = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "💵 Efectivo",
                        "💳 Tarjeta",
                        "📱 Transf."
                    ).forEach { metodo ->

                        Button(
                            enabled =
                                !procesandoOrden,
                            modifier =
                                Modifier.weight(1f),
                            colors =
                                ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            PalaciusBackgroundDark
                                    ),
                            onClick = {
                                procesandoOrden =
                                    true

                                /*
                                 * Conservamos total,
                                 * totalRaul, totalCristian,
                                 * productos y extras.
                                 *
                                 * Solo cambia el estado
                                 * y el método de pago.
                                 */
                                val ordenCobrada =
                                    orden.copy(
                                        estado =
                                            "Cobrado",
                                        metodoPago =
                                            metodo
                                    )

                                scope.launch {
                                    try {
                                        val filasActualizadas =
                                            ordenDao
                                                .actualizarOrden(
                                                    ordenCobrada
                                                )

                                        if (
                                            filasActualizadas !=
                                            1
                                        ) {
                                            error(
                                                "No se encontró la orden que intentabas cobrar."
                                            )
                                        }

                                        /*
                                         * La venta ya está guardada.
                                         * Si la impresión falla,
                                         * no se pierde el cobro.
                                         */
                                        val resultado =
                                            withContext(
                                                Dispatchers.IO
                                            ) {
                                                printer
                                                    .imprimirTicketOrden(
                                                        ordenCobrada
                                                    )
                                            }

                                        ordenPendienteCobrar =
                                            null

                                        when (
                                            resultado
                                        ) {
                                            is ResultadoImpresion.Exito -> {
                                                Toast.makeText(
                                                    context,
                                                    "Orden cobrada e impresa.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            is ResultadoImpresion.Error -> {
                                                Toast.makeText(
                                                    context,
                                                    "Orden cobrada, pero no se imprimió: ${resultado.mensaje}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }

                                    } catch (
                                        exception: Exception
                                    ) {
                                        Toast.makeText(
                                            context,
                                            "No se pudo cobrar la orden: ${exception.message}",
                                            Toast.LENGTH_LONG
                                        ).show()

                                    } finally {
                                        procesandoOrden =
                                            false
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = metodo,
                                color =
                                    PalaciusTextLight,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },

            dismissButton = {
                TextButton(
                    enabled =
                        !procesandoOrden,
                    onClick = {
                        ordenPendienteCobrar =
                            null
                    }
                ) {
                    Text(
                        text =
                            if (procesandoOrden) {
                                "Procesando..."
                            } else {
                                "Atrás"
                            },
                        color =
                            PalaciusTextLight
                    )
                }
            }
        )
    }
}

@Composable
private fun TarjetaOrdenPendiente(
    orden: OrdenEntity,
    formatoMoneda: NumberFormat,
    formatoFecha: SimpleDateFormat,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                PalaciusSurfaceDark
            )
            .clickable(
                enabled = habilitada,
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.Top
        ) {
            Column(
                modifier =
                    Modifier.weight(1f)
            ) {
                Text(
                    text =
                        orden.identificador,
                    color =
                        PalaciusTextLight,
                    fontSize = 18.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        formatoFecha.format(
                            Date(orden.fechaHora)
                        ),
                    color =
                        PalaciusTextMuted,
                    fontSize = 11.sp
                )
            }

            Text(
                text =
                    formatoMoneda.format(
                        orden.total
                    ),
                color =
                    PalaciusPrimaryMustard,
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        val lineasResumen =
            orden.resumenProductos
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(5)
                .toList()

        lineasResumen.forEach { linea ->
            val esExtra =
                linea.startsWith("+")

            val esNota =
                linea.startsWith(
                    "Nota:",
                    ignoreCase = true
                )

            Text(
                text = linea,
                color =
                    when {
                        esExtra ->
                            PalaciusSecondaryGold

                        esNota ->
                            PalaciusTextMuted

                        else ->
                            PalaciusTextLight
                    },
                fontSize =
                    if (
                        esExtra ||
                        esNota
                    ) {
                        12.sp
                    } else {
                        13.sp
                    },
                fontWeight =
                    if (
                        !esExtra &&
                        !esNota
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                fontStyle =
                    if (esNota) {
                        FontStyle.Italic
                    } else {
                        FontStyle.Normal
                    },
                modifier =
                    if (esExtra || esNota) {
                        Modifier.padding(
                            start = 8.dp
                        )
                    } else {
                        Modifier
                    }
            )
        }

        if (
            orden.resumenProductos
                .lineSequence()
                .filter {
                    it.isNotBlank()
                }
                .count() > 5
        ) {
            Text(
                text = "Ver pedido completo...",
                color =
                    PalaciusTextMuted,
                fontSize = 11.sp
            )
        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(
                        RoundedCornerShape(50)
                    )
                    .background(
                        PalaciusSecondaryGold
                    )
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Text(
                text = "Pendiente",
                color =
                    PalaciusSecondaryGold,
                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ResumenOrdenPendiente(
    resumen: String,
    formatoMoneda: NumberFormat
) {
    val lineas =
        resumen
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

    if (lineas.isEmpty()) {
        Text(
            text =
                "La orden no tiene un desglose disponible.",
            color =
                PalaciusTextMuted
        )

        return
    }

    lineas.forEach { linea ->
        val esExtra =
            linea.startsWith("+")

        val esNota =
            linea.startsWith(
                "Nota:",
                ignoreCase = true
            )

        val indicePrecio =
            linea.lastIndexOf("$")

        if (
            indicePrecio > 0 &&
            !esNota
        ) {
            var descripcion =
                linea.substring(
                    0,
                    indicePrecio
                ).trim()

            if (
                descripcion.endsWith("-")
            ) {
                descripcion =
                    descripcion
                        .dropLast(1)
                        .trim()
            }

            val precio =
                linea.substring(
                    indicePrecio
                ).trim()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start =
                            if (esExtra) {
                                10.dp
                            } else {
                                0.dp
                            },
                        bottom = 5.dp
                    ),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.Top
            ) {
                Text(
                    text = descripcion,
                    color =
                        if (esExtra) {
                            PalaciusSecondaryGold
                        } else {
                            PalaciusTextLight
                        },
                    fontSize =
                        if (esExtra) {
                            13.sp
                        } else {
                            14.sp
                        },
                    fontWeight =
                        if (esExtra) {
                            FontWeight.Normal
                        } else {
                            FontWeight.Bold
                        },
                    modifier =
                        Modifier.weight(1f)
                )

                Text(
                    text = precio,
                    color =
                        PalaciusSecondaryGold,
                    fontSize = 13.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        } else {
            Text(
                text = linea,
                color =
                    if (esNota) {
                        PalaciusTextMuted
                    } else {
                        PalaciusTextLight
                    },
                fontSize = 13.sp,
                fontStyle =
                    if (esNota) {
                        FontStyle.Italic
                    } else {
                        FontStyle.Normal
                    },
                modifier = Modifier
                    .padding(
                        start =
                            if (
                                esNota ||
                                esExtra
                            ) {
                                10.dp
                            } else {
                                0.dp
                            },
                        bottom = 5.dp
                    )
            )
        }
    }
}