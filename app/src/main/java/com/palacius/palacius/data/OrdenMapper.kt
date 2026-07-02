package com.palacius.palacius.data

import com.palacius.palacius.ArticuloTicket
import com.palacius.palacius.ExtraTicket
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Convierte un artículo del carrito en una entidad
 * persistente relacionada con una orden.
 */
internal fun ArticuloTicket.toOrdenArticuloEntity(
    ordenId: Int
): OrdenArticuloEntity {

    require(ordenId > 0) {
        "La orden debe tener un ID válido."
    }

    return OrdenArticuloEntity(
        ordenId = ordenId,
        productoId = productoId,
        productoNombre = nombreProducto.trim(),

        /*
         * Las variantes todavía no tienen tabla propia,
         * por lo que varianteId permanece null.
         */
        varianteId = null,
        varianteNombre = varianteNombre
            ?.trim()
            ?.takeIf { it.isNotBlank() },

        precioBaseCentavos =
            precioBase.aCentavos(),

        cantidad = cantidad,
        nota = nota.trim(),
        propietarioProducto =
            propietarioProducto.trim()
    )
}

/**
 * Convierte un extra del carrito en una entidad
 * relacionada con el artículo ya guardado.
 */
internal fun ExtraTicket.toOrdenArticuloExtraEntity(
    articuloId: Long
): OrdenArticuloExtraEntity {

    require(articuloId > 0) {
        "El artículo debe tener un ID válido."
    }

    return OrdenArticuloExtraEntity(
        articuloId = articuloId,
        toppingId = toppingId,
        nombre = nombre.trim(),
        precioUnitarioCentavos =
            precio.aCentavos(),

        /*
         * Actualmente el configurador permite seleccionar
         * una vez cada extra.
         */
        cantidadPorProducto = 1,
        propietario = propietario.trim()
    )
}

/**
 * Conversión temporal de Double a centavos.
 *
 * BigDecimal evita errores como:
 * 89.90 * 100 = 8989.999999...
 */
internal fun Double.aCentavos(): Long {

    require(isFinite()) {
        "El importe debe ser un número válido."
    }

    require(this >= 0.0) {
        "El importe no puede ser negativo."
    }

    return BigDecimal
        .valueOf(this)
        .movePointRight(2)
        .setScale(
            0,
            RoundingMode.HALF_UP
        )
        .longValueExact()
}