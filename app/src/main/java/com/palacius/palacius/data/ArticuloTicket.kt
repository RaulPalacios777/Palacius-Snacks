package com.palacius.palacius.data

import java.text.Normalizer
import java.util.Locale
data class ExtraSeleccionado(
    val nombre: String,
    val precio: Double
)

data class ArticuloTicket(

    val producto: String,

    val sabor: String? = null,

    val extras: MutableList<ExtraSeleccionado> = mutableListOf(),

    val precioBase: Double,

    var cantidad: Int = 1,

    var nota: String = "",

    val propietario: String

) {

    val precioUnitario: Double
        get() = precioBase + extras.sumOf { it.precio }

    val subtotal: Double
        get() = precioUnitario * cantidad

    fun descripcionCorta(): String {

        val texto = StringBuilder()

        texto.append(producto)

        sabor?.let {
            texto.append("\n   • $it")
        }

        extras.forEach {
            texto.append("\n   + ${it.nombre}")
        }

        if (nota.isNotBlank()) {
            texto.append("\n   📝 $nota")
        }

        return texto.toString()
    }

}