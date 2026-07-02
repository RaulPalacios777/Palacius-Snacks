package com.palacius.palacius

import java.text.Normalizer
import java.util.Locale
import java.util.UUID

data class ExtraTicket(
    val toppingId: Int,
    val nombre: String,
    val precio: Double,
    val propietario: String
) {
    init {
        require(toppingId > 0) {
            "El topping debe tener un ID válido."
        }

        require(nombre.isNotBlank()) {
            "El nombre del extra no puede estar vacío."
        }

        require(precio >= 0.0) {
            "El precio del extra no puede ser negativo."
        }
    }
}

data class ArticuloTicket(
    val lineaId: String = UUID.randomUUID().toString(),

    val productoId: Int,
    val nombreProducto: String,

    /*
     * Actualmente las variantes aún provienen de variantesRaw,
     * por eso temporalmente no existe varianteId.
     */
    val varianteNombre: String? = null,

    /*
     * Si existe variante, este valor contiene el precio
     * de la variante. De lo contrario, contiene el precio base.
     */
    val precioBase: Double,

    val extras: List<ExtraTicket> = emptyList(),

    val cantidad: Int = 1,
    val nota: String = "",

    val propietarioProducto: String
) {
    init {
        require(productoId > 0) {
            "El producto debe tener un ID válido."
        }

        require(nombreProducto.isNotBlank()) {
            "El nombre del producto no puede estar vacío."
        }

        require(precioBase >= 0.0) {
            "El precio base no puede ser negativo."
        }

        require(cantidad > 0) {
            "La cantidad debe ser mayor que cero."
        }
    }

    val precioExtrasUnitario: Double
        get() = extras.sumOf { it.precio }

    val precioUnitario: Double
        get() = precioBase + precioExtrasUnitario

    val subtotal: Double
        get() = precioUnitario * cantidad

    val descripcion: String
        get() = buildString {
            append(nombreProducto)

            varianteNombre
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    append(" (")
                    append(it)
                    append(")")
                }

            if (extras.isNotEmpty()) {
                append(" [+ ")
                append(
                    extras.joinToString(", ") {
                        it.nombre
                    }
                )
                append("]")
            }
        }

    /**
     * Indica si dos artículos representan exactamente
     * la misma configuración.
     *
     * No compara cantidad, nota ni lineaId.
     */
    fun mismaConfiguracionQue(
        otro: ArticuloTicket
    ): Boolean {
        return productoId == otro.productoId &&
                varianteNombre == otro.varianteNombre &&
                precioBase == otro.precioBase &&
                propietarioProducto == otro.propietarioProducto &&
                extras.sortedBy { it.toppingId } ==
                otro.extras.sortedBy { it.toppingId }
    }

    /**
     * Calcula cuánto corresponde a un propietario.
     *
     * El precio del producto pertenece al propietario del producto.
     * Cada extra pertenece al propietario configurado en el topping.
     */
    fun subtotalParaPropietario(
        propietarioBuscado: String
    ): Double {
        val importeProducto =
            if (
                mismoPropietario(
                    propietarioProducto,
                    propietarioBuscado
                )
            ) {
                precioBase
            } else {
                0.0
            }

        val importeExtras =
            extras
                .filter {
                    mismoPropietario(
                        it.propietario,
                        propietarioBuscado
                    )
                }
                .sumOf {
                    it.precio
                }

        return (importeProducto + importeExtras) * cantidad
    }

    private fun mismoPropietario(
        propietarioActual: String,
        propietarioBuscado: String
    ): Boolean {

        return normalizarPropietario(
            propietarioActual
        ) == normalizarPropietario(
            propietarioBuscado
        )
    }

    private fun normalizarPropietario(
        propietario: String
    ): String {

        val sinAcentos =
            Normalizer.normalize(
                propietario.trim(),
                Normalizer.Form.NFD
            )
                .replace(
                    Regex("\\p{M}+"),
                    ""
                )
                .lowercase(
                    Locale.ROOT
                )

        /*
         * Convierte:
         *
         * Raúl (Parrilla/Snacks) -> raul
         * Cristian (Postres)     -> cristian
         * Caja Compartida        -> caja
         */
        return sinAcentos
            .substringBefore("(")
            .trim()
            .split(
                Regex("\\s+")
            )
            .firstOrNull()
            .orEmpty()
    }
}