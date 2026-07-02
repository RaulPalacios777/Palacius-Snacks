package com.palacius.palacius

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.palacius.palacius.data.MenuDao
import com.palacius.palacius.data.ProductoMenuEntity
import com.palacius.palacius.data.ToppingEntity
import com.palacius.palacius.ui.theme.*
import kotlinx.coroutines.launch
import android.widget.Toast

data class SaborTemp(val nombre: String, val precio: Double)
private enum class FiltroEstadoCatalogo {
    ACTIVOS,
    INACTIVOS
}

private data class OpcionPropietario(
    val valor: String,
    val etiqueta: String
)

private val opcionesPropietario = listOf(
    OpcionPropietario(
        valor = "Raúl",
        etiqueta = "Raúl (Parrilla/Snacks)"
    ),
    OpcionPropietario(
        valor = "Cristian",
        etiqueta = "Cristian (Frappés/Postres)"
    ),
    OpcionPropietario(
        /*
         * Se mantiene "Caja" porque los registros anteriores
         * fueron guardados utilizando split(" ")[0].
         */
        valor = "Caja",
        etiqueta = "Caja Compartida"
    )
)

private fun obtenerOpcionPropietario(
    valorGuardado: String
): OpcionPropietario {
    return opcionesPropietario.firstOrNull { opcion ->
        opcion.valor.equals(
            valorGuardado.trim(),
            ignoreCase = true
        ) ||
                opcion.etiqueta.startsWith(
                    valorGuardado.trim(),
                    ignoreCase = true
                )
    } ?: opcionesPropietario.first()
}

@Composable
private fun SelectorEstadoCatalogo(
    filtroActual: FiltroEstadoCatalogo,
    cantidadActivos: Int,
    cantidadInactivos: Int,
    onCambiarFiltro: (FiltroEstadoCatalogo) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                onCambiarFiltro(
                    FiltroEstadoCatalogo.ACTIVOS
                )
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    if (
                        filtroActual ==
                        FiltroEstadoCatalogo.ACTIVOS
                    ) {
                        PalaciusPrimaryMustard
                    } else {
                        PalaciusBackgroundDark
                    }
            )
        ) {
            Text(
                text = "Activos ($cantidadActivos)",
                color =
                    if (
                        filtroActual ==
                        FiltroEstadoCatalogo.ACTIVOS
                    ) {
                        PalaciusBackgroundDark
                    } else {
                        PalaciusTextLight
                    },
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = {
                onCambiarFiltro(
                    FiltroEstadoCatalogo.INACTIVOS
                )
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor =
                    if (
                        filtroActual ==
                        FiltroEstadoCatalogo.INACTIVOS
                    ) {
                        PalaciusSecondaryGold
                    } else {
                        PalaciusBackgroundDark
                    }
            )
        ) {
            Text(
                text = "Inactivos ($cantidadInactivos)",
                color =
                    if (
                        filtroActual ==
                        FiltroEstadoCatalogo.INACTIVOS
                    ) {
                        PalaciusBackgroundDark
                    } else {
                        PalaciusTextLight
                    },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PantallaAdmin(menuDao: MenuDao) {
    var pestanaActual by remember { mutableIntStateOf(1) }
    // AÑADIMOS LA PESTAÑA DE AJUSTES
    val titulosPestanas = listOf("📊 Dashboard", "🍔 Menú", "🥓 Extras", "🧮 Calculadora", "⚙️ Ajustes")

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Centro de Mando", color = PalaciusPrimaryMustard, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        ScrollableTabRow(
            selectedTabIndex = pestanaActual,
            containerColor = PalaciusSurfaceDark,
            contentColor = PalaciusPrimaryMustard,
            edgePadding = 0.dp,
            indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[pestanaActual]), color = PalaciusPrimaryMustard) }
        ) {
            titulosPestanas.forEachIndexed { index, titulo ->
                Tab(selected = pestanaActual == index, onClick = { pestanaActual = index }, text = { Text(titulo, fontWeight = FontWeight.Bold, color = if (pestanaActual == index) PalaciusPrimaryMustard else PalaciusTextMuted) })
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (pestanaActual) {
            0 -> VistaDashboardProximamente()
            1 -> VistaGestionMenu(menuDao)
            2 -> VistaGestionToppings(menuDao)
            3 -> VistaCalculadoraProximamente()
            4 -> VistaAjustes() // <-- NUEVA VISTA CONECTADA
        }
    }
}
@Composable
fun VistaAjustes() {
    val context = LocalContext.current
    val compartidos = context.getSharedPreferences("PalaciusPrefs", Context.MODE_PRIVATE)

    // Cargamos la MAC guardada o usamos la tuya por defecto
    var macImpresora by remember { mutableStateOf(compartidos.getString("mac_impresora", "5A:4A:11:6D:00:03") ?: "") }
    var mensajeGuardado by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Configuración de Hardware", color = PalaciusSecondaryGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PalaciusSurfaceDark).padding(16.dp)) {
            Text("Impresora Térmica Bluetooth", color = PalaciusTextLight, fontWeight = FontWeight.Bold)
            Text("Ingresa la dirección MAC de la ticketera para vincularla al sistema POS.", color = PalaciusTextMuted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = macImpresora,
                onValueChange = { macImpresora = it.uppercase() },
                label = { Text("Dirección MAC (Ej: 5A:4A:11:...)", color = PalaciusTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PalaciusTextLight, unfocusedTextColor = PalaciusTextLight, focusedBorderColor = PalaciusPrimaryMustard)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    compartidos.edit().putString("mac_impresora", macImpresora.trim()).apply()
                    mensajeGuardado = "¡MAC de impresora guardada con éxito!"
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PalaciusPrimaryMustard)
            ) {
                Text("Guardar Configuración", color = PalaciusBackgroundDark, fontWeight = FontWeight.Bold)
            }

            if (mensajeGuardado.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(mensajeGuardado, color = Color(0xFF81C784), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF3E2723)).padding(16.dp)) {
            Text("Próximas Actualizaciones (Hoja de Ruta)", color = PalaciusPrimaryMustard, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("• Sistema de Costo de Producción (Fondo de Reinversión).\n• Cierres estructurados por días (Viernes, Sábado, Domingo).\n• Exportación a PDF y JSON para IA.", color = PalaciusTextLight, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}
// --- NUEVA VISTA PARA GESTIONAR EXTRAS (Queso, Papas, Carne) ---
@Composable

fun VistaGestionToppings(
    menuDao: MenuDao
) {
    val scope = rememberCoroutineScope()


    /*
     * Esta consulta incluye activos e inactivos.
     */
    val toppingsTodos by
    menuDao
        .obtenerToppingsAdministracion()
        .collectAsState(
            initial = emptyList<ToppingEntity>()
        )

    var filtroActual by remember {
        mutableStateOf(
            FiltroEstadoCatalogo.ACTIVOS
        )
    }

    val toppingsFiltrados =
        toppingsTodos.filter { topping ->
            when (filtroActual) {
                FiltroEstadoCatalogo.ACTIVOS ->
                    topping.activo

                FiltroEstadoCatalogo.INACTIVOS ->
                    !topping.activo
            }
        }

    val cantidadActivos =
        toppingsTodos.count {
            it.activo
        }

    val cantidadInactivos =
        toppingsTodos.count {
            !it.activo
        }

    var nombreExtra by remember {
        mutableStateOf("")
    }

    var precioExtra by remember {
        mutableStateOf("")
    }

    var propietarioSeleccionado by remember {
        mutableStateOf(
            opcionesPropietario.first()
        )
    }

    var expandirPropietario by remember {
        mutableStateOf(false)
    }

    var mensajeFormulario by remember {
        mutableStateOf("")
    }



    var toppingPendienteDesactivar by remember {
        mutableStateOf<ToppingEntity?>(null)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Alta de Extras / Toppings",
                color = PalaciusSecondaryGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Ejemplo: Extra queso, carne adicional o papas.",
                color = PalaciusTextMuted,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
                    .background(
                        PalaciusSurfaceDark
                    )
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = nombreExtra,
                        onValueChange = {
                            nombreExtra = it
                            mensajeFormulario = ""
                        },
                        label = {
                            Text(
                                text = "Nombre del extra",
                                color = PalaciusTextMuted
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor =
                                    PalaciusTextLight,
                                unfocusedTextColor =
                                    PalaciusTextLight
                            )
                    )

                    OutlinedTextField(
                        value = precioExtra,
                        onValueChange = {
                            precioExtra = it
                            mensajeFormulario = ""
                        },
                        label = {
                            Text(
                                text = "Precio",
                                color = PalaciusTextMuted
                            )
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Decimal
                            ),
                        modifier =
                            Modifier.weight(0.6f),
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor =
                                    PalaciusTextLight,
                                unfocusedTextColor =
                                    PalaciusTextLight
                            )
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value =
                            propietarioSeleccionado.etiqueta,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                text = "Ganancia del extra para",
                                color = PalaciusTextMuted
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector =
                                    Icons.Default.ArrowDropDown,
                                contentDescription =
                                    "Desplegar propietarios",
                                tint =
                                    PalaciusPrimaryMustard
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth(),
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedTextColor =
                                    PalaciusTextLight,
                                unfocusedTextColor =
                                    PalaciusTextLight
                            )
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Color.Transparent
                            )
                            .clickable {
                                expandirPropietario = true
                            }
                    )

                    DropdownMenu(
                        expanded =
                            expandirPropietario,
                        onDismissRequest = {
                            expandirPropietario = false
                        },
                        modifier = Modifier.background(
                            PalaciusSurfaceDark
                        )
                    ) {
                        opcionesPropietario.forEach {
                                opcion ->

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text =
                                            opcion.etiqueta,
                                        color =
                                            PalaciusTextLight
                                    )
                                },
                                onClick = {
                                    propietarioSeleccionado =
                                        opcion

                                    expandirPropietario =
                                        false
                                }
                            )
                        }
                    }
                }

                if (mensajeFormulario.isNotBlank()) {
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text = mensajeFormulario,
                        color = Color(0xFFE57373),
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Button(
                    onClick = {
                        val nombreLimpio =
                            nombreExtra.trim()

                        val precio =
                            precioExtra
                                .replace(",", ".")
                                .toDoubleOrNull()

                        when {
                            nombreLimpio.isBlank() -> {
                                mensajeFormulario =
                                    "Ingresa el nombre del extra."
                            }

                            precio == null ||
                                    precio <= 0.0 -> {
                                mensajeFormulario =
                                    "Ingresa un precio mayor que cero."
                            }

                            else -> {
                                scope.launch {
                                    menuDao.guardarTopping(
                                        ToppingEntity(
                                            nombre =
                                                nombreLimpio,
                                            precio = precio,
                                            propietario =
                                                propietarioSeleccionado
                                                    .valor,
                                            activo = true
                                        )
                                    )
                                }

                                nombreExtra = ""
                                precioExtra = ""
                                mensajeFormulario = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PalaciusPrimaryMustard
                        )
                ) {
                    Text(
                        text = "Guardar extra",
                        color =
                            PalaciusBackgroundDark,
                        fontWeight =
                            FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Extras registrados",
                color = PalaciusSecondaryGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            SelectorEstadoCatalogo(
                filtroActual = filtroActual,
                cantidadActivos = cantidadActivos,
                cantidadInactivos =
                    cantidadInactivos,
                onCambiarFiltro = {
                    filtroActual = it
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        if (toppingsFiltrados.isEmpty()) {
            item {
                Text(
                    text =
                        if (
                            filtroActual ==
                            FiltroEstadoCatalogo.ACTIVOS
                        ) {
                            "No hay extras activos."
                        } else {
                            "No hay extras inactivos."
                        },
                    color = PalaciusTextMuted,
                    modifier =
                        Modifier.padding(16.dp)
                )
            }
        } else {
            items(
                items = toppingsFiltrados,
                key = { topping ->
                    topping.id
                }
            ) { topping ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            if (topping.activo) {
                                PalaciusSurfaceDark
                            } else {
                                Color(0xFF3E2723)
                            }
                        )
                        .padding(16.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "+ ${topping.nombre}",
                            color =
                                if (topping.activo) {
                                    PalaciusTextLight
                                } else {
                                    PalaciusTextMuted
                                },
                            fontSize = 16.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "Dueño: ${topping.propietario}",
                            color =
                                PalaciusTextMuted,
                            fontSize = 12.sp
                        )

                        Text(
                            text =
                                if (topping.activo) {
                                    "Estado: activo"
                                } else {
                                    "Estado: inactivo"
                                },
                            color =
                                if (topping.activo) {
                                    Color(0xFF81C784)
                                } else {
                                    Color(0xFFE57373)
                                },
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text =
                                "+$${topping.precio}",
                            color =
                                PalaciusPrimaryMustard,
                            fontWeight =
                                FontWeight.Bold
                        )

                        if (topping.activo) {
                            Button(
                                onClick = {
                                    toppingPendienteDesactivar =
                                        topping
                                },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            Color(0xFF8B0000)
                                    )
                            ) {
                                Text(
                                    text = "Desactivar",
                                    color = Color.White
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        menuDao
                                            .reactivarToppingPorId(
                                                topping.id
                                            )
                                    }
                                },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            Color(0xFF2E7D32)
                                    )
                            ) {
                                Text(
                                    text = "Reactivar",
                                    color = Color.White,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    toppingPendienteDesactivar?.let {
            topping ->

        AlertDialog(
            onDismissRequest = {
                toppingPendienteDesactivar =
                    null
            },
            containerColor =
                PalaciusSurfaceDark,
            title = {
                Text(
                    text = "Desactivar extra",
                    color =
                        PalaciusPrimaryMustard,
                    fontWeight =
                        FontWeight.Bold
                )
            },
            text = {
                Text(
                    text =
                        "El extra “${topping.nombre}” dejará de aparecer en el POS, pero conservará su información histórica.",
                    color =
                        PalaciusTextLight
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            menuDao
                                .desactivarToppingPorId(
                                    topping.id
                                )
                        }

                        toppingPendienteDesactivar =
                            null
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF8B0000)
                        )
                ) {
                    Text(
                        text = "Desactivar",
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        toppingPendienteDesactivar =
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
}
@Composable
fun VistaGestionMenu(
    menuDao: MenuDao
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val menuTodos by menuDao
        .obtenerMenuAdministracion()
        .collectAsState(
            initial = emptyList<ProductoMenuEntity>()
        )

    var filtroActual by remember {
        mutableStateOf(
            FiltroEstadoCatalogo.ACTIVOS
        )
    }

    val productosFiltrados =
        menuTodos.filter { producto ->
            when (filtroActual) {
                FiltroEstadoCatalogo.ACTIVOS ->
                    producto.activo

                FiltroEstadoCatalogo.INACTIVOS ->
                    !producto.activo
            }
        }

    val cantidadActivos =
        menuTodos.count {
            it.activo
        }

    val cantidadInactivos =
        menuTodos.count {
            !it.activo
        }

    var productoEditando by remember {
        mutableStateOf<ProductoMenuEntity?>(null)
    }

    var productoPendienteDesactivar by remember {
        mutableStateOf<ProductoMenuEntity?>(null)
    }

    var nombre by remember {
        mutableStateOf("")
    }

    var precioBase by remember {
        mutableStateOf("")
    }

    var icono by remember {
        mutableStateOf("🍔")
    }

    val categorias =
        listOf(
            "Hamburguesas",
            "Alitas",
            "Snacks",
            "Frappés",
            "Micheladas",
            "Bebidas",
            "Postres",
            "Extras"
        )

    var categoriaSeleccionada by remember {
        mutableStateOf(
            categorias.first()
        )
    }

    var expandirCategoria by remember {
        mutableStateOf(false)
    }

    var propietarioSeleccionado by remember {
        mutableStateOf(
            opcionesPropietario.first()
        )
    }

    var expandirPropietario by remember {
        mutableStateOf(false)
    }

    var listaSabores by remember {
        mutableStateOf(
            emptyList<SaborTemp>()
        )
    }

    var nombreSaborTemp by remember {
        mutableStateOf("")
    }

    var precioSaborTemp by remember {
        mutableStateOf("")
    }

    var mensajeFormulario by remember {
        mutableStateOf("")
    }

    var guardandoProducto by remember {
        mutableStateOf(false)
    }

    fun limpiarFormulario() {
        productoEditando = null
        nombre = ""
        precioBase = ""
        icono = "🍔"
        categoriaSeleccionada =
            categorias.first()
        propietarioSeleccionado =
            opcionesPropietario.first()
        listaSabores = emptyList()
        nombreSaborTemp = ""
        precioSaborTemp = ""
        mensajeFormulario = ""
    }

    fun cargarProductoParaEditar(
        producto: ProductoMenuEntity
    ) {
        productoEditando = producto
        nombre = producto.nombre
        precioBase =
            producto.precioBase.toString()
        icono = producto.icono
        categoriaSeleccionada =
            producto.categoria

        propietarioSeleccionado =
            obtenerOpcionPropietario(
                producto.propietario
            )

        listaSabores =
            producto
                .obtenerVariantes()
                .map { variante ->
                    SaborTemp(
                        nombre = variante.first,
                        precio = variante.second
                    )
                }

        nombreSaborTemp = ""
        precioSaborTemp = ""
        mensajeFormulario = ""
    }

    fun guardarOActualizarProducto() {
        val nombreLimpio =
            nombre.trim()

        val precio =
            precioBase
                .replace(",", ".")
                .toDoubleOrNull()

        when {
            nombreLimpio.isBlank() -> {
                mensajeFormulario =
                    "Ingresa el nombre del producto."
            }

            listaSabores.isEmpty() &&
                    (
                            precio == null ||
                                    precio <= 0.0
                            ) -> {
                mensajeFormulario =
                    "Ingresa un precio mayor que cero."
            }

            propietarioSeleccionado
                .valor
                .isBlank() -> {
                mensajeFormulario =
                    "Selecciona el propietario del producto."
            }

            else -> {
                val variantesTexto =
                    listaSabores.joinToString(
                        separator = ","
                    ) { sabor ->
                        "${sabor.nombre}|${sabor.precio}"
                    }

                val productoAnterior =
                    productoEditando

                val productoAGuardar =
                    ProductoMenuEntity(
                        id =
                            productoAnterior?.id
                                ?: 0,

                        nombre =
                            nombreLimpio,

                        precioBase =
                            if (
                                listaSabores.isEmpty()
                            ) {
                                precio ?: 0.0
                            } else {
                                productoAnterior
                                    ?.precioBase
                                    ?: 0.0
                            },

                        costoProduccion =
                            productoAnterior
                                ?.costoProduccion
                                ?: 0.0,

                        icono =
                            icono
                                .trim()
                                .ifBlank {
                                    "🍔"
                                },

                        categoria =
                            categoriaSeleccionada,

                        propietario =
                            propietarioSeleccionado
                                .valor,

                        variantesRaw =
                            variantesTexto,

                        activo =
                            productoAnterior
                                ?.activo
                                ?: true
                    )

                guardandoProducto = true
                mensajeFormulario = ""

                scope.launch {
                    try {
                        if (productoAnterior == null) {
                            val nuevoId =
                                menuDao.insertarProducto(
                                    productoAGuardar
                                )

                            if (nuevoId <= 0L) {
                                error(
                                    "Room no devolvió un ID válido."
                                )
                            }

                            Toast.makeText(
                                context,
                                "Producto guardado correctamente.",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {
                            val filasActualizadas =
                                menuDao.actualizarProducto(
                                    productoAGuardar
                                )

                            if (filasActualizadas != 1) {
                                error(
                                    "No se encontró el producto que intentabas actualizar."
                                )
                            }

                            Toast.makeText(
                                context,
                                "Producto actualizado correctamente.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        limpiarFormulario()

                    } catch (
                        exception: Exception
                    ) {
                        mensajeFormulario =
                            exception.message
                                ?: "No se pudo guardar el producto."

                        Toast.makeText(
                            context,
                            "Error: $mensajeFormulario",
                            Toast.LENGTH_LONG
                        ).show()

                    } finally {
                        guardandoProducto = false
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text =
                    if (productoEditando != null) {
                        "Editando producto"
                    } else {
                        "Alta de nuevo producto"
                    },
                color =
                    PalaciusSecondaryGold,
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
                    .background(
                        PalaciusSurfaceDark
                    )
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = icono,
                        onValueChange = {
                            icono = it
                        },
                        label = {
                            Text(
                                text = "Icono",
                                color =
                                    PalaciusTextMuted
                            )
                        },
                        modifier =
                            Modifier.weight(0.3f),
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedTextColor =
                                        PalaciusTextLight,
                                    unfocusedTextColor =
                                        PalaciusTextLight
                                )
                    )

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = {
                            nombre = it
                            mensajeFormulario = ""
                        },
                        label = {
                            Text(
                                text = "Nombre",
                                color =
                                    PalaciusTextMuted
                            )
                        },
                        modifier =
                            Modifier.weight(1f),
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedTextColor =
                                        PalaciusTextLight,
                                    unfocusedTextColor =
                                        PalaciusTextLight
                                )
                    )

                    if (listaSabores.isEmpty()) {
                        OutlinedTextField(
                            value = precioBase,
                            onValueChange = {
                                precioBase = it
                                mensajeFormulario = ""
                            },
                            label = {
                                Text(
                                    text = "Precio base",
                                    color =
                                        PalaciusTextMuted
                                )
                            },
                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Decimal
                                ),
                            modifier =
                                Modifier.weight(0.6f),
                            singleLine = true,
                            colors =
                                OutlinedTextFieldDefaults
                                    .colors(
                                        focusedTextColor =
                                            PalaciusTextLight,
                                        unfocusedTextColor =
                                            PalaciusTextLight
                                    )
                        )
                    } else {
                        Text(
                            text =
                                "El precio base se ignora cuando existen variantes.",
                            color =
                                PalaciusTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .weight(0.6f)
                                .align(
                                    Alignment.CenterVertically
                                )
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value =
                                categoriaSeleccionada,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    text = "Categoría",
                                    color =
                                        PalaciusTextMuted
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default
                                            .ArrowDropDown,
                                    contentDescription =
                                        "Seleccionar categoría",
                                    tint =
                                        PalaciusPrimaryMustard
                                )
                            },
                            modifier =
                                Modifier.fillMaxWidth(),
                            colors =
                                OutlinedTextFieldDefaults
                                    .colors(
                                        focusedTextColor =
                                            PalaciusTextLight,
                                        unfocusedTextColor =
                                            PalaciusTextLight
                                    )
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Color.Transparent
                                )
                                .clickable {
                                    expandirCategoria = true
                                }
                        )

                        DropdownMenu(
                            expanded =
                                expandirCategoria,
                            onDismissRequest = {
                                expandirCategoria = false
                            },
                            modifier =
                                Modifier.background(
                                    PalaciusSurfaceDark
                                )
                        ) {
                            categorias.forEach {
                                    categoria ->

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text =
                                                categoria,
                                            color =
                                                PalaciusTextLight
                                        )
                                    },
                                    onClick = {
                                        categoriaSeleccionada =
                                            categoria

                                        expandirCategoria =
                                            false
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value =
                                propietarioSeleccionado
                                    .etiqueta,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    text = "Ganancia para",
                                    color =
                                        PalaciusTextMuted
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector =
                                        Icons.Default
                                            .ArrowDropDown,
                                    contentDescription =
                                        "Seleccionar propietario",
                                    tint =
                                        PalaciusPrimaryMustard
                                )
                            },
                            modifier =
                                Modifier.fillMaxWidth(),
                            colors =
                                OutlinedTextFieldDefaults
                                    .colors(
                                        focusedTextColor =
                                            PalaciusTextLight,
                                        unfocusedTextColor =
                                            PalaciusTextLight
                                    )
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Color.Transparent
                                )
                                .clickable {
                                    expandirPropietario =
                                        true
                                }
                        )

                        DropdownMenu(
                            expanded =
                                expandirPropietario,
                            onDismissRequest = {
                                expandirPropietario =
                                    false
                            },
                            modifier =
                                Modifier.background(
                                    PalaciusSurfaceDark
                                )
                        ) {
                            opcionesPropietario.forEach {
                                    opcion ->

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text =
                                                opcion.etiqueta,
                                            color =
                                                PalaciusTextLight
                                        )
                                    },
                                    onClick = {
                                        propietarioSeleccionado =
                                            opcion

                                        expandirPropietario =
                                            false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Text(
                    text = "Variantes o sabores",
                    color =
                        PalaciusSecondaryGold,
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nombreSaborTemp,
                        onValueChange = {
                            nombreSaborTemp = it
                            mensajeFormulario = ""
                        },
                        label = {
                            Text(
                                text =
                                    "Variante (Ej. Oreo)",
                                color =
                                    PalaciusTextMuted
                            )
                        },
                        modifier =
                            Modifier.weight(1f),
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedTextColor =
                                        PalaciusTextLight,
                                    unfocusedTextColor =
                                        PalaciusTextLight
                                )
                    )

                    OutlinedTextField(
                        value = precioSaborTemp,
                        onValueChange = {
                            precioSaborTemp = it
                            mensajeFormulario = ""
                        },
                        label = {
                            Text(
                                text = "Precio",
                                color =
                                    PalaciusTextMuted
                            )
                        },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType =
                                    KeyboardType.Decimal
                            ),
                        modifier =
                            Modifier.weight(0.6f),
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedTextColor =
                                        PalaciusTextLight,
                                    unfocusedTextColor =
                                        PalaciusTextLight
                                )
                    )

                    Button(
                        onClick = {
                            val nombreVariante =
                                nombreSaborTemp.trim()

                            val precioVariante =
                                precioSaborTemp
                                    .replace(",", ".")
                                    .toDoubleOrNull()

                            when {
                                nombreVariante.isBlank() -> {
                                    mensajeFormulario =
                                        "Escribe el nombre de la variante."
                                }

                                nombreVariante.contains(",") ||
                                        nombreVariante.contains("|") -> {
                                    mensajeFormulario =
                                        "La variante no puede contener comas ni el símbolo |."
                                }

                                precioVariante == null ||
                                        precioVariante <= 0.0 -> {
                                    mensajeFormulario =
                                        "La variante debe tener un precio mayor que cero."
                                }

                                listaSabores.any {
                                    it.nombre.equals(
                                        nombreVariante,
                                        ignoreCase = true
                                    )
                                } -> {
                                    mensajeFormulario =
                                        "Esa variante ya fue agregada."
                                }

                                else -> {
                                    listaSabores =
                                        listaSabores +
                                                SaborTemp(
                                                    nombre =
                                                        nombreVariante,
                                                    precio =
                                                        precioVariante
                                                )

                                    nombreSaborTemp = ""
                                    precioSaborTemp = ""
                                    mensajeFormulario = ""
                                }
                            }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    PalaciusBackgroundDark
                            ),
                        modifier =
                            Modifier.height(56.dp)
                    ) {
                        Text(
                            text = "+",
                            color =
                                PalaciusPrimaryMustard,
                            fontSize = 24.sp
                        )
                    }
                }

                if (listaSabores.isNotEmpty()) {
                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(
                                rememberScrollState()
                            ),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        listaSabores.forEach {
                                sabor ->

                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            8.dp
                                        )
                                    )
                                    .background(
                                        PalaciusBackgroundDark
                                    )
                                    .padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    )
                            ) {
                                Text(
                                    text =
                                        "${sabor.nombre} ($${sabor.precio})",
                                    color =
                                        PalaciusTextLight,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                listaSabores =
                                    emptyList()
                            }
                        ) {
                            Text(
                                text = "Borrar variantes",
                                color =
                                    Color(0xFFE57373),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (mensajeFormulario.isNotBlank()) {
                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text = mensajeFormulario,
                        color =
                            Color(0xFFE57373),
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    if (productoEditando != null) {
                        Button(
                            enabled =
                                !guardandoProducto,
                            onClick = {
                                limpiarFormulario()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        PalaciusDivider
                                )
                        ) {
                            Text(
                                text = "Cancelar",
                                color =
                                    PalaciusTextLight,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        enabled =
                            !guardandoProducto,
                        onClick = {
                            guardarOActualizarProducto()
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    PalaciusPrimaryMustard
                            )
                    ) {
                        Text(
                            text =
                                when {
                                    guardandoProducto &&
                                            productoEditando != null ->
                                        "Actualizando..."

                                    guardandoProducto ->
                                        "Guardando..."

                                    productoEditando != null ->
                                        "Actualizar producto"

                                    else ->
                                        "Guardar producto"
                                },
                            color =
                                PalaciusBackgroundDark,
                            fontWeight =
                                FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Menú registrado",
                color =
                    PalaciusSecondaryGold,
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            SelectorEstadoCatalogo(
                filtroActual = filtroActual,
                cantidadActivos =
                    cantidadActivos,
                cantidadInactivos =
                    cantidadInactivos,
                onCambiarFiltro = {
                    filtroActual = it
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        if (productosFiltrados.isEmpty()) {
            item {
                Text(
                    text =
                        if (
                            filtroActual ==
                            FiltroEstadoCatalogo.ACTIVOS
                        ) {
                            "No hay productos activos."
                        } else {
                            "No hay productos inactivos."
                        },
                    color =
                        PalaciusTextMuted,
                    modifier =
                        Modifier.padding(16.dp)
                )
            }
        } else {
            items(
                items = productosFiltrados,
                key = {
                    it.id
                }
            ) { producto ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            if (producto.activo) {
                                PalaciusSurfaceDark
                            } else {
                                Color(0xFF3E2723)
                            }
                        )
                        .padding(16.dp),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {
                        Text(
                            text =
                                "${producto.icono} ${producto.nombre}",
                            color =
                                if (producto.activo) {
                                    PalaciusTextLight
                                } else {
                                    PalaciusTextMuted
                                },
                            fontSize = 16.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            text =
                                "${producto.categoria} | Dueño: ${producto.propietario}",
                            color =
                                PalaciusTextMuted,
                            fontSize = 12.sp
                        )

                        val variantes =
                            producto.obtenerVariantes()

                        if (variantes.isNotEmpty()) {
                            Text(
                                text =
                                    variantes.joinToString(
                                        separator = " • "
                                    ) {
                                        "${it.first} $${it.second}"
                                    },
                                color =
                                    PalaciusSecondaryGold,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text =
                                    "Precio: $${producto.precioBase}",
                                color =
                                    PalaciusSecondaryGold,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text =
                                if (producto.activo) {
                                    "Estado: activo"
                                } else {
                                    "Estado: inactivo"
                                },
                            color =
                                if (producto.activo) {
                                    Color(0xFF81C784)
                                } else {
                                    Color(0xFFE57373)
                                },
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                cargarProductoParaEditar(
                                    producto
                                )
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        PalaciusSecondaryGold
                                )
                        ) {
                            Text(
                                text = "Editar",
                                color =
                                    PalaciusBackgroundDark
                            )
                        }

                        if (producto.activo) {
                            Button(
                                onClick = {
                                    productoPendienteDesactivar =
                                        producto
                                },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            Color(0xFF8B0000)
                                    )
                            ) {
                                Text(
                                    text = "Desactivar",
                                    color = Color.White
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        menuDao
                                            .reactivarProductoPorId(
                                                producto.id
                                            )
                                    }
                                },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            Color(0xFF2E7D32)
                                    )
                            ) {
                                Text(
                                    text = "Reactivar",
                                    color = Color.White,
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    productoPendienteDesactivar?.let {
            producto ->

        AlertDialog(
            onDismissRequest = {
                productoPendienteDesactivar =
                    null
            },
            containerColor =
                PalaciusSurfaceDark,
            title = {
                Text(
                    text = "Desactivar producto",
                    color =
                        PalaciusPrimaryMustard,
                    fontWeight =
                        FontWeight.Bold
                )
            },
            text = {
                Text(
                    text =
                        "“${producto.nombre}” dejará de aparecer en el punto de venta, pero conservará su historial.",
                    color =
                        PalaciusTextLight
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            menuDao
                                .desactivarProductoPorId(
                                    producto.id
                                )
                        }

                        if (
                            productoEditando?.id ==
                            producto.id
                        ) {
                            limpiarFormulario()
                        }

                        productoPendienteDesactivar =
                            null
                    },
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                Color(0xFF8B0000)
                        )
                ) {
                    Text(
                        text = "Desactivar",
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        productoPendienteDesactivar =
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
}
@Composable
fun VistaDashboardProximamente() { Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("📊", fontSize = 60.sp); Spacer(modifier = Modifier.height(16.dp)); Text("Aquí irán las gráficas de ventas del mes.", color = PalaciusTextMuted, fontSize = 16.sp, textAlign = TextAlign.Center) } }
@Composable
fun VistaCalculadoraProximamente() { Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("🧮", fontSize = 60.sp); Spacer(modifier = Modifier.height(16.dp)); Text("Aquí importaremos las fórmulas de tu Excel", color = PalaciusTextLight, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) } }