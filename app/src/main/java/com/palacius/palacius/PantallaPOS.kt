package com.palacius.palacius

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palacius.palacius.data.MenuDao
import com.palacius.palacius.data.OrdenRepository
import com.palacius.palacius.data.OrdenEntity
import com.palacius.palacius.data.ProductoMenuEntity
import com.palacius.palacius.data.ToppingEntity
import com.palacius.palacius.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PantallaPOS(
    ordenRepository: OrdenRepository,
    menuDao: MenuDao
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val formatoMoneda = remember {
        NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-MX")
        )
    }

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

    val menuCompleto by
    menuDao
        .obtenerMenuCompleto()
        .collectAsState(
            initial = emptyList<ProductoMenuEntity>()
        )

    val toppingsGlobales by
    menuDao
        .obtenerToppings()
        .collectAsState(
            initial = emptyList<ToppingEntity>()
        )

    var categoriaSeleccionada by
    remember {
        mutableStateOf("Todos")
    }

    val categoriasDisponibles =
        remember(menuCompleto) {
            listOf("Todos") +
                    menuCompleto
                        .map { it.categoria }
                        .distinct()
                        .sorted()
        }

    val ticketActual =
        remember {
            mutableStateListOf<ArticuloTicket>()
        }

    val totalCuenta =
        ticketActual.sumOf {
            it.subtotal
        }

    var lineaParaNotaId by
    remember {
        mutableStateOf<String?>(null)
    }

    var textoNota by
    remember {
        mutableStateOf("")
    }

    var mostrarDialogoMesa by
    remember {
        mutableStateOf(false)
    }

    var nombreMesa by
    remember {
        mutableStateOf("")
    }

    var mostrarDialogoCobroPOS by
    remember {
        mutableStateOf(false)
    }

    var procesandoVenta by
    remember {
        mutableStateOf(false)
    }
    /*
     * Guarda temporalmente la línea que el usuario
     * desea eliminar del carrito.
     */
    var lineaPendienteEliminarId by
    remember {
        mutableStateOf<String?>(null)
    }
    /*
     * Estados del configurador.
     */
    var productoAConfigurar by
    remember {
        mutableStateOf<ProductoMenuEntity?>(null)
    }

    var varianteSeleccionada by
    remember {
        mutableStateOf<Pair<String, Double>?>(null)
    }

    /*
     * Guardamos IDs, no entidades completas.
     * Esto evita problemas si Room actualiza una entidad.
     */
    var extrasSeleccionadosIds by
    remember {
        mutableStateOf<Set<Int>>(emptySet())
    }

    fun limpiarConfigurador() {
        productoAConfigurar = null
        varianteSeleccionada = null
        extrasSeleccionadosIds = emptySet()
    }

    fun abrirConfigurador(
        producto: ProductoMenuEntity
    ) {
        productoAConfigurar = producto
        varianteSeleccionada =
            producto
                .obtenerVariantes()
                .firstOrNull()

        extrasSeleccionadosIds = emptySet()
    }

    fun agregarAlTicket(
        producto: ProductoMenuEntity,
        variante: Pair<String, Double>?,
        extrasSeleccionados: List<ToppingEntity>
    ) {
        val extrasTicket =
            extrasSeleccionados
                .map { extra ->
                    ExtraTicket(
                        toppingId = extra.id,
                        nombre = extra.nombre,
                        precio = extra.precio,
                        propietario = extra.propietario
                    )
                }
                .sortedBy {
                    it.toppingId
                }

        val nuevoArticulo =
            ArticuloTicket(
                productoId = producto.id,
                nombreProducto = producto.nombre,
                varianteNombre = variante?.first,
                precioBase =
                    variante?.second
                        ?: producto.precioBase,
                extras = extrasTicket,
                propietarioProducto = producto.propietario
            )

        val indiceExistente =
            ticketActual.indexOfFirst { articulo ->
                articulo.nota.isBlank() &&
                        articulo.mismaConfiguracionQue(
                            nuevoArticulo
                        )
            }

        if (indiceExistente >= 0) {
            val articuloExistente =
                ticketActual[indiceExistente]

            ticketActual[indiceExistente] =
                articuloExistente.copy(
                    cantidad =
                        articuloExistente.cantidad + 1
                )
        } else {
            ticketActual.add(nuevoArticulo)
        }
    }

    /**
     * Adaptador temporal.
     *
     * Convierte el ticket estructurado al resumen antiguo,
     * porque historial e impresora todavía utilizan
     * OrdenEntity.resumenProductos.
     */
    fun construirResumenLegacy(
        articulos: List<ArticuloTicket>
    ): String {

        return buildString {

            articulos.forEachIndexed { indice, articulo ->

                /*
                 * Importe correspondiente solamente
                 * al producto base o variante.
                 */
                val totalProductoBase =
                    articulo.precioBase *
                            articulo.cantidad

                val precioProductoTexto =
                    String.format(
                        Locale.US,
                        "%.2f",
                        totalProductoBase
                    )

                /*
                 * Línea principal:
                 *
                 * x2 Hamburguesa (Hawaiana) - $180.00
                 */
                append("x")
                append(articulo.cantidad)
                append(" ")
                append(articulo.nombreProducto)

                articulo.varianteNombre
                    ?.takeIf { it.isNotBlank() }
                    ?.let { variante ->
                        append(" (")
                        append(variante.trim())
                        append(")")
                    }

                append(" - $")
                append(precioProductoTexto)
                appendLine()

                /*
                 * Cada extra se imprime en su propia línea.
                 *
                 * Cuando la cantidad del producto es mayor
                 * que uno, se muestra también la cantidad
                 * total de extras cobrados.
                 */
                articulo.extras.forEach { extra ->

                    val totalExtra =
                        extra.precio *
                                articulo.cantidad

                    val precioExtraTexto =
                        String.format(
                            Locale.US,
                            "%.2f",
                            totalExtra
                        )

                    append("+ ")
                    append(extra.nombre.trim())

                    if (articulo.cantidad > 1) {
                        append(" x")
                        append(articulo.cantidad)
                    }

                    append(" - $")
                    append(precioExtraTexto)
                    appendLine()
                }

                /*
                 * La nota también se imprime separada.
                 */
                if (articulo.nota.isNotBlank()) {
                    append("Nota: ")
                    append(articulo.nota.trim())
                    appendLine()
                }

                /*
                 * No agregamos una línea vacía después
                 * del último producto.
                 */
                if (indice < articulos.lastIndex) {
                    appendLine()
                }
            }
        }.trim()
    }

    fun totalParaPropietario(
        articulos: List<ArticuloTicket>,
        propietario: String
    ): Double {
        return articulos.sumOf {
            it.subtotalParaPropietario(
                propietario
            )
        }
    }
    /**
     * Aumenta una unidad a una línea específica.
     */
    fun aumentarCantidad(
        lineaId: String
    ) {
        if (procesandoVenta) return

        val indice =
            ticketActual.indexOfFirst {
                it.lineaId == lineaId
            }

        if (indice < 0) return

        val articuloActual =
            ticketActual[indice]

        ticketActual[indice] =
            articuloActual.copy(
                cantidad =
                    articuloActual.cantidad + 1
            )
    }

    /**
     * Disminuye una unidad.
     *
     * Si solamente queda una, solicita confirmación
     * antes de eliminar completamente la línea.
     */
    fun disminuirCantidad(
        lineaId: String
    ) {
        if (procesandoVenta) return

        val indice =
            ticketActual.indexOfFirst {
                it.lineaId == lineaId
            }

        if (indice < 0) return

        val articuloActual =
            ticketActual[indice]

        if (articuloActual.cantidad > 1) {
            ticketActual[indice] =
                articuloActual.copy(
                    cantidad =
                        articuloActual.cantidad - 1
                )
        } else {
            lineaPendienteEliminarId =
                articuloActual.lineaId
        }
    }

    /**
     * Solicita eliminar toda la línea,
     * independientemente de su cantidad.
     */
    fun solicitarEliminarLinea(
        lineaId: String
    ) {
        if (procesandoVenta) return

        lineaPendienteEliminarId = lineaId
    }

    /**
     * Confirma y elimina la línea seleccionada.
     */
    fun confirmarEliminarLinea() {

        val lineaId =
            lineaPendienteEliminarId
                ?: return

        val indice =
            ticketActual.indexOfFirst {
                it.lineaId == lineaId
            }

        if (indice >= 0) {
            ticketActual.removeAt(indice)
        }

        /*
         * Si estaba abierto el editor de notas
         * para esa línea, también se cierra.
         */
        if (lineaParaNotaId == lineaId) {
            lineaParaNotaId = null
            textoNota = ""
        }

        lineaPendienteEliminarId = null

        /*
         * Protección adicional por si el carrito
         * queda vacío.
         */
        if (ticketActual.isEmpty()) {
            mostrarDialogoMesa = false
            mostrarDialogoCobroPOS = false
        }
    }
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        /*
         * LADO IZQUIERDO: MENÚ
         */
        Column(
            modifier = Modifier
                .weight(0.65f)
                .fillMaxHeight()
        ) {
            Text(
                text = "Punto de Venta",
                color = PalaciusPrimaryMustard,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            val indiceCategoria =
                categoriasDisponibles
                    .indexOf(categoriaSeleccionada)
                    .coerceAtLeast(0)

            ScrollableTabRow(
                selectedTabIndex = indiceCategoria,
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                indicator = {}
            ) {
                categoriasDisponibles.forEach { categoria ->

                    val seleccionada =
                        categoriaSeleccionada == categoria

                    Tab(
                        selected = seleccionada,
                        onClick = {
                            categoriaSeleccionada = categoria
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (seleccionada) {
                                    PalaciusPrimaryMustard
                                } else {
                                    PalaciusSurfaceDark
                                }
                            )
                            .height(40.dp)
                    ) {
                        Text(
                            text = categoria,
                            color =
                                if (seleccionada) {
                                    PalaciusBackgroundDark
                                } else {
                                    PalaciusTextLight
                                },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = 16.dp
                            )
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            val productosFiltrados =
                if (categoriaSeleccionada == "Todos") {
                    menuCompleto
                } else {
                    menuCompleto.filter {
                        it.categoria ==
                                categoriaSeleccionada
                    }
                }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                gridItems(
                    items = productosFiltrados,
                    key = { producto ->
                        producto.id
                    }
                ) { producto ->

                    Box(
                        modifier = Modifier
                            .aspectRatio(1.1f)
                            .clip(
                                RoundedCornerShape(12.dp)
                            )
                            .background(
                                PalaciusSurfaceDark
                            )
                            .clickable {
                                abrirConfigurador(producto)
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = producto.icono,
                                fontSize = 28.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            Text(
                                text = producto.nombre,
                                color = PalaciusTextLight,
                                fontSize = 14.sp,
                                fontWeight =
                                    FontWeight.Bold,
                                textAlign =
                                    TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(4.dp)
                            )

                            if (
                                producto
                                    .obtenerVariantes()
                                    .isEmpty()
                            ) {
                                Text(
                                    text =
                                        formatoMoneda.format(
                                            producto.precioBase
                                        ),
                                    color =
                                        PalaciusPrimaryMustard,
                                    fontSize = 12.sp
                                )
                            } else {
                                Text(
                                    text = "Personalizar...",
                                    color =
                                        PalaciusTextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        /*
         * LADO DERECHO: TICKET
         */
        Column(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight()
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .background(
                    PalaciusSurfaceDark
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Cuenta Actual",
                color = PalaciusPrimaryMustard,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    bottom = 16.dp
                )
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = ticketActual,
                    key = { articulo ->
                        articulo.lineaId
                    }
                ) { articulo ->

                    TarjetaArticuloTicket(
                        articulo = articulo,
                        formatoMoneda = formatoMoneda,
                        habilitado = !procesandoVenta,

                        onAumentar = {
                            aumentarCantidad(
                                articulo.lineaId
                            )
                        },

                        onDisminuir = {
                            disminuirCantidad(
                                articulo.lineaId
                            )
                        },

                        onEliminar = {
                            solicitarEliminarLinea(
                                articulo.lineaId
                            )
                        },

                        onEditarNota = {
                            lineaParaNotaId =
                                articulo.lineaId

                            textoNota =
                                articulo.nota
                        }
                    )

                    HorizontalDivider(
                        color =
                            PalaciusDivider.copy(
                                alpha = 0.3f
                            )
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total:",
                    color = PalaciusTextLight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text =
                        formatoMoneda.format(
                            totalCuenta
                        ),
                    color = PalaciusPrimaryMustard,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(
                            RoundedCornerShape(12.dp)
                        )
                        .background(
                            PalaciusBackgroundDark
                        )
                        .clickable(
                            enabled =
                                ticketActual.isNotEmpty()
                        ) {
                            mostrarDialogoMesa = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pendiente",
                        color = PalaciusTextLight,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .height(60.dp)
                        .clip(
                            RoundedCornerShape(12.dp)
                        )
                        .background(
                            PalaciusPrimaryMustard
                        )
                        .clickable(
                            enabled =
                                ticketActual.isNotEmpty()
                        ) {
                            mostrarDialogoCobroPOS = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cobrar",
                        color =
                            PalaciusBackgroundDark,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }

    /*
     * CONFIGURADOR DE PRODUCTO
     */
    /*
     * CONFIRMACIÓN PARA ELIMINAR UNA LÍNEA
     */
    val articuloPendienteEliminar =
        lineaPendienteEliminarId
            ?.let { lineaId ->
                ticketActual.firstOrNull {
                    it.lineaId == lineaId
                }
            }

articuloPendienteEliminar?.let { articulo ->

    AlertDialog(
        onDismissRequest = {
            lineaPendienteEliminarId = null
        },
        containerColor =
            PalaciusSurfaceDark,

        title = {
            Text(
                text = "Eliminar producto",
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
                        "¿Deseas eliminar este producto de la cuenta?",
                    color =
                        PalaciusTextLight
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        "${articulo.cantidad}x ${articulo.descripcion}",
                    color =
                        PalaciusSecondaryGold,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        formatoMoneda.format(
                            articulo.subtotal
                        ),
                    color =
                        PalaciusTextLight
                )
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    confirmarEliminarLinea()
                },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF8B0000)
                    )
            ) {
                Text(
                    text = "Sí, eliminar",
                    color = Color.White,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        },

        dismissButton = {
            TextButton(
                onClick = {
                    lineaPendienteEliminarId =
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
    productoAConfigurar?.let { producto ->

        val variantes =
            producto.obtenerVariantes()

        val precioBaseActual =
            varianteSeleccionada?.second
                ?: producto.precioBase

        val extrasSeleccionados =
            toppingsGlobales.filter {
                it.id in extrasSeleccionadosIds
            }

        val precioExtras =
            extrasSeleccionados.sumOf {
                it.precio
            }

        val costoTotalActual =
            precioBaseActual + precioExtras

        AlertDialog(
            onDismissRequest = {
                limpiarConfigurador()
            },
            containerColor =
                PalaciusSurfaceDark,

            title = {
                Text(
                    text =
                        "Personalizar ${producto.nombre}",
                    color =
                        PalaciusPrimaryMustard,
                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(
                            rememberScrollState()
                        )
                ) {
                    if (variantes.isNotEmpty()) {
                        Text(
                            text =
                                "1. Elige la variante",
                            color =
                                PalaciusSecondaryGold,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        LazyVerticalGrid(
                            columns =
                                GridCells.Fixed(2),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                ),
                            modifier =
                                Modifier.heightIn(
                                    max = 150.dp
                                )
                        ) {
                            gridItems(
                                items = variantes,
                                key = {
                                    "${it.first}-${it.second}"
                                }
                            ) { variante ->

                                val seleccionada =
                                    varianteSeleccionada ==
                                            variante

                                Button(
                                    onClick = {
                                        varianteSeleccionada =
                                            variante
                                    },
                                    colors =
                                        ButtonDefaults
                                            .buttonColors(
                                                containerColor =
                                                    if (
                                                        seleccionada
                                                    ) {
                                                        PalaciusPrimaryMustard
                                                    } else {
                                                        PalaciusBackgroundDark
                                                    }
                                            ),
                                    modifier =
                                        Modifier.height(
                                            50.dp
                                        )
                                ) {
                                    Text(
                                        text =
                                            "${variante.first} " +
                                                    formatoMoneda.format(
                                                        variante.second
                                                    ),
                                        color =
                                            if (
                                                seleccionada
                                            ) {
                                                PalaciusBackgroundDark
                                            } else {
                                                PalaciusTextLight
                                            },
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(
                            modifier =
                                Modifier.height(16.dp)
                        )
                    }

                    if (
                        toppingsGlobales.isNotEmpty()
                    ) {
                        Text(
                            text =
                                if (
                                    variantes.isNotEmpty()
                                ) {
                                    "2. Extras (opcional)"
                                } else {
                                    "Extras (opcional)"
                                },
                            color =
                                PalaciusSecondaryGold,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        LazyVerticalGrid(
                            columns =
                                GridCells.Fixed(2),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                ),
                            modifier =
                                Modifier.heightIn(
                                    max = 200.dp
                                )
                        ) {
                            gridItems(
                                items =
                                    toppingsGlobales,
                                key = { extra ->
                                    extra.id
                                }
                            ) { extra ->

                                val seleccionado =
                                    extra.id in
                                            extrasSeleccionadosIds

                                Button(
                                    onClick = {
                                        extrasSeleccionadosIds =
                                            if (
                                                seleccionado
                                            ) {
                                                extrasSeleccionadosIds -
                                                        extra.id
                                            } else {
                                                extrasSeleccionadosIds +
                                                        extra.id
                                            }
                                    },
                                    colors =
                                        ButtonDefaults
                                            .buttonColors(
                                                containerColor =
                                                    if (
                                                        seleccionado
                                                    ) {
                                                        PalaciusPrimaryMustard
                                                            .copy(
                                                                alpha =
                                                                    0.65f
                                                            )
                                                    } else {
                                                        PalaciusBackgroundDark
                                                    }
                                            ),
                                    modifier =
                                        Modifier.height(
                                            54.dp
                                        )
                                ) {
                                    Text(
                                        text =
                                            "+ ${extra.nombre} " +
                                                    formatoMoneda.format(
                                                        extra.precio
                                                    ),
                                        color =
                                            PalaciusTextLight,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },

            confirmButton = {
                Button(
                    enabled =
                        variantes.isEmpty() ||
                                varianteSeleccionada != null,
                    onClick = {
                        agregarAlTicket(
                            producto = producto,
                            variante =
                                varianteSeleccionada,
                            extrasSeleccionados =
                                extrasSeleccionados
                        )

                        limpiarConfigurador()
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PalaciusPrimaryMustard
                        )
                ) {
                    Text(
                        text =
                            "Agregar: " +
                                    formatoMoneda.format(
                                        costoTotalActual
                                    ),
                        color =
                            PalaciusBackgroundDark,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        limpiarConfigurador()
                    }
                ) {
                    Text(
                        text = "Cancelar",
                        color = PalaciusTextLight
                    )
                }
            }
        )
    }

    /*
     * NOTA DE COCINA
     */
    val articuloParaNota =
        lineaParaNotaId?.let { lineaId ->
            ticketActual.firstOrNull {
                it.lineaId == lineaId
            }
        }

    articuloParaNota?.let { articulo ->

        AlertDialog(
            onDismissRequest = {
                lineaParaNotaId = null
            },
            containerColor =
                PalaciusSurfaceDark,

            title = {
                Text(
                    text = "Notas de Cocina",
                    color =
                        PalaciusPrimaryMustard,
                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {
                OutlinedTextField(
                    value = textoNota,
                    onValueChange = {
                        textoNota = it
                    },
                    placeholder = {
                        Text(
                            text =
                                "Ej: Sin cebolla...",
                            color =
                                PalaciusTextMuted
                        )
                    },
                    colors =
                        OutlinedTextFieldDefaults
                            .colors(
                                focusedTextColor =
                                    PalaciusTextLight,
                                unfocusedTextColor =
                                    PalaciusTextLight
                            ),
                    modifier =
                        Modifier.fillMaxWidth()
                )
            },

            confirmButton = {
                Button(
                    onClick = {
                        val indice =
                            ticketActual
                                .indexOfFirst {
                                    it.lineaId ==
                                            articulo.lineaId
                                }

                        if (indice >= 0) {
                            ticketActual[indice] =
                                ticketActual[indice]
                                    .copy(
                                        nota =
                                            textoNota.trim()
                                    )
                        }

                        lineaParaNotaId = null
                        textoNota = ""
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PalaciusPrimaryMustard
                        )
                ) {
                    Text(
                        text = "Guardar Nota",
                        color =
                            PalaciusBackgroundDark,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        lineaParaNotaId = null
                        textoNota = ""
                    }
                ) {
                    Text(
                        text = "Cancelar",
                        color = PalaciusTextLight
                    )
                }
            }
        )
    }

    /*
     * GUARDAR COMO ORDEN PENDIENTE
     */
    if (mostrarDialogoMesa) {
        val atajosRapidos =
            listOf(
                "Para llevar",
                "A domicilio",
                "Barra",
                "Mesa 1",
                "Mesa 2",
                "Mesa 3",
                "Pareja",
                "Familia"
            )

        AlertDialog(
            onDismissRequest = {
                if (!procesandoVenta) {
                    mostrarDialogoMesa = false
                }
            },
            containerColor =
                PalaciusSurfaceDark,

            title = {
                Text(
                    text =
                        "¿A dónde va el pedido?",
                    color =
                        PalaciusPrimaryMustard,
                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {
                Column {
                    LazyVerticalGrid(
                        columns =
                            GridCells.Fixed(2),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            ),
                        modifier = Modifier
                            .heightIn(
                                max = 200.dp
                            )
                            .fillMaxWidth()
                    ) {
                        gridItems(
                            items = atajosRapidos,
                            key = { it }
                        ) { atajo ->

                            val seleccionado =
                                nombreMesa == atajo

                            Button(
                                onClick = {
                                    nombreMesa = atajo
                                },
                                colors =
                                    ButtonDefaults
                                        .buttonColors(
                                            containerColor =
                                                if (
                                                    seleccionado
                                                ) {
                                                    PalaciusPrimaryMustard
                                                } else {
                                                    PalaciusBackgroundDark
                                                }
                                        )
                            ) {
                                Text(
                                    text = atajo,
                                    color =
                                        if (
                                            seleccionado
                                        ) {
                                            PalaciusBackgroundDark
                                        } else {
                                            PalaciusTextLight
                                        }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )

                    Text(
                        text =
                            "O escribe un nombre:",
                        color =
                            PalaciusSecondaryGold,
                        fontSize = 12.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    OutlinedTextField(
                        value = nombreMesa,
                        onValueChange = {
                            nombreMesa = it
                        },
                        placeholder = {
                            Text(
                                text = "Ej: Juan...",
                                color =
                                    PalaciusTextMuted
                            )
                        },
                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedTextColor =
                                        PalaciusTextLight,
                                    unfocusedTextColor =
                                        PalaciusTextLight
                                ),
                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }
            },

            confirmButton = {
                Button(
                    enabled =
                        !procesandoVenta &&
                                nombreMesa.isNotBlank() &&
                                ticketActual.isNotEmpty(),
                    onClick = {
                        procesandoVenta = true

                        val articulos =
                            ticketActual.toList()

                        val identificador =
                            nombreMesa.trim()

                        val ordenNueva =
                            OrdenEntity(
                                identificador =
                                    identificador,
                                estado = "Pendiente",
                                total =
                                    articulos.sumOf {
                                        it.subtotal
                                    },
                                resumenProductos =
                                    construirResumenLegacy(
                                        articulos
                                    ),
                                totalRaul =
                                    totalParaPropietario(
                                        articulos,
                                        "Raúl"
                                    ),
                                totalCristian =
                                    totalParaPropietario(
                                        articulos,
                                        "Cristian"
                                    )
                            )

                        scope.launch {
                            try {
                                ordenRepository.guardarOrdenCompleta(
                                    orden = ordenNueva,
                                    articulos = articulos
                                )

                                ticketActual.clear()
                                nombreMesa = ""
                                mostrarDialogoMesa =
                                    false

                                Toast.makeText(
                                    context,
                                    "Orden enviada a pendientes.",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } catch (
                                exception: Exception
                            ) {
                                Toast.makeText(
                                    context,
                                    "No se pudo guardar la orden: ${exception.message}",
                                    Toast.LENGTH_LONG
                                ).show()

                            } finally {
                                procesandoVenta = false
                            }
                        }
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PalaciusPrimaryMustard
                        )
                ) {
                    Text(
                        text =
                            if (procesandoVenta) {
                                "Guardando..."
                            } else {
                                "Mandar a Cocina"
                            },
                        color =
                            PalaciusBackgroundDark,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            },

            dismissButton = {
                TextButton(
                    enabled = !procesandoVenta,
                    onClick = {
                        mostrarDialogoMesa = false
                    }
                ) {
                    Text(
                        text = "Cancelar",
                        color = PalaciusTextLight
                    )
                }
            }
        )
    }

    /*
     * COBRO DIRECTO
     */
    if (mostrarDialogoCobroPOS) {
        AlertDialog(
            onDismissRequest = {
                if (!procesandoVenta) {
                    mostrarDialogoCobroPOS =
                        false
                }
            },
            containerColor =
                PalaciusSurfaceDark,

            title = {
                Text(
                    text = "Cobrar Exacto",
                    color =
                        PalaciusPrimaryMustard,
                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {
                Text(
                    text =
                        "Total: " +
                                formatoMoneda.format(
                                    totalCuenta
                                ),
                    color = PalaciusTextLight,
                    fontSize = 24.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            },

            confirmButton = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "💵 Efectivo",
                        "💳 Tarjeta",
                        "📱 Transf."
                    ).forEach { metodo ->

                        Button(
                            modifier =
                                Modifier.weight(1f),
                            enabled =
                                !procesandoVenta,
                            colors =
                                ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            PalaciusBackgroundDark
                                    ),
                            onClick = {
                                if (
                                    ticketActual.isEmpty()
                                ) {
                                    return@Button
                                }

                                procesandoVenta = true

                                val articulos =
                                    ticketActual.toList()

                                val ordenNueva =
                                    OrdenEntity(
                                        identificador =
                                            "Venta Directa",
                                        estado = "Cobrado",
                                        metodoPago =
                                            metodo,
                                        total =
                                            articulos.sumOf {
                                                it.subtotal
                                            },
                                        resumenProductos =
                                            construirResumenLegacy(
                                                articulos
                                            ),
                                        totalRaul =
                                            totalParaPropietario(
                                                articulos,
                                                "Raúl"
                                            ),
                                        totalCristian =
                                            totalParaPropietario(
                                                articulos,
                                                "Cristian"
                                            )
                                    )

                                scope.launch {
                                    try {
                                        val ordenGuardada =
                                            ordenRepository
                                                .guardarOrdenCompleta(
                                                    orden = ordenNueva,
                                                    articulos = articulos
                                                )

                                        ticketActual.clear()
                                        mostrarDialogoCobroPOS =
                                            false

                                        val resultado =
                                            withContext(
                                                Dispatchers.IO
                                            ) {
                                                printer
                                                    .imprimirTicketOrden(
                                                        ordenGuardada
                                                    )
                                            }

                                        when (
                                            resultado
                                        ) {
                                            is ResultadoImpresion.Exito -> {
                                                Toast.makeText(
                                                    context,
                                                    "Venta registrada e impresa.",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                            is ResultadoImpresion.Error -> {
                                                Toast.makeText(
                                                    context,
                                                    "Venta guardada, pero no se imprimió: ${resultado.mensaje}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }

                                    } catch (
                                        exception: Exception
                                    ) {
                                        Toast.makeText(
                                            context,
                                            "No se pudo guardar la venta: ${exception.message}",
                                            Toast.LENGTH_LONG
                                        ).show()

                                    } finally {
                                        procesandoVenta =
                                            false
                                    }
                                }
                            }
                        ) {
                            Text(
                                text = metodo,
                                color =
                                    PalaciusTextLight,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },

            dismissButton = {
                TextButton(
                    enabled =
                        !procesandoVenta,
                    onClick = {
                        mostrarDialogoCobroPOS =
                            false
                    }
                ) {
                    Text(
                        text =
                            if (
                                procesandoVenta
                            ) {
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
private fun TarjetaArticuloTicket(
        articulo: ArticuloTicket,
        formatoMoneda: NumberFormat,
        habilitado: Boolean,
        onAumentar: () -> Unit,
        onDisminuir: () -> Unit,
        onEliminar: () -> Unit,
        onEditarNota: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(
                    PalaciusBackgroundDark.copy(
                        alpha = 0.45f
                    )
                )
                .padding(10.dp)
        ) {

            /*
             * Nombre, variante y subtotal.
             */
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
                            articulo.nombreProducto +
                                    (
                                            articulo
                                                .varianteNombre
                                                ?.takeIf {
                                                    it.isNotBlank()
                                                }
                                                ?.let {
                                                    " ($it)"
                                                } ?: ""
                                            ),
                        color =
                            PalaciusTextLight,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    /*
                     * Precio unitario del producto completo.
                     */
                    Text(
                        text =
                            "Precio unitario: " +
                                    formatoMoneda.format(
                                        articulo.precioUnitario
                                    ),
                        color =
                            PalaciusTextMuted,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text =
                        formatoMoneda.format(
                            articulo.subtotal
                        ),
                    color =
                        PalaciusPrimaryMustard,
                    fontWeight =
                        FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            /*
             * Extras separados.
             */
            if (articulo.extras.isNotEmpty()) {
                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                articulo.extras.forEach { extra ->
                    Text(
                        text =
                            "+ ${extra.nombre} " +
                                    formatoMoneda.format(
                                        extra.precio
                                    ),
                        color =
                            PalaciusSecondaryGold,
                        fontSize = 12.sp
                    )
                }
            }

            /*
             * Nota de cocina.
             */
            if (articulo.nota.isNotBlank()) {
                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Text(
                    text =
                        "Nota: ${articulo.nota}",
                    color =
                        PalaciusSecondaryGold,
                    fontSize = 12.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            /*
             * Controles de cantidad.
             */
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDisminuir,
                    enabled = habilitado,
                    modifier =
                        Modifier.size(38.dp),
                    contentPadding =
                        PaddingValues(0.dp),
                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    PalaciusTextLight
                            )
                ) {
                    Text(
                        text = "−",
                        fontSize = 20.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Text(
                    text =
                        articulo.cantidad.toString(),
                    color =
                        PalaciusTextLight,
                    fontSize = 17.sp,
                    fontWeight =
                        FontWeight.Bold,
                    textAlign =
                        TextAlign.Center,
                    modifier =
                        Modifier.width(38.dp)
                )

                OutlinedButton(
                    onClick = onAumentar,
                    enabled = habilitado,
                    modifier =
                        Modifier.size(38.dp),
                    contentPadding =
                        PaddingValues(0.dp),
                    colors =
                        ButtonDefaults
                            .outlinedButtonColors(
                                contentColor =
                                    PalaciusTextLight
                            )
                ) {
                    Text(
                        text = "+",
                        fontSize = 20.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                TextButton(
                    onClick = onEditarNota,
                    enabled = habilitado,
                    contentPadding =
                        PaddingValues(
                            horizontal = 6.dp
                        )
                ) {
                    Text(
                        text =
                            if (
                                articulo.nota.isBlank()
                            ) {
                                "Nota"
                            } else {
                                "Editar nota"
                            },
                        color =
                            PalaciusSecondaryGold,
                        fontSize = 11.sp
                    )
                }

                TextButton(
                    onClick = onEliminar,
                    enabled = habilitado,
                    contentPadding =
                        PaddingValues(
                            horizontal = 6.dp
                        ),
                    colors =
                        ButtonDefaults
                            .textButtonColors(
                                contentColor =
                                    Color(0xFFE57373)
                            )
                ) {
                    Text(
                        text = "Eliminar",
                        fontSize = 11.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }