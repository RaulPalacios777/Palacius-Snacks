package com.palacius.palacius.data

import android.util.Log
import androidx.room.withTransaction
import com.palacius.palacius.ArticuloTicket

class OrdenRepository(
    private val database: PalaciusDatabase,
    private val ordenDao: OrdenDao,
    private val ordenDetalleDao: OrdenDetalleDao
) {

    companion object {
        private const val TAG =
            "PALACIUS_ORDEN_REPOSITORY"
    }

    /**
     * Guarda una orden completa:
     *
     * 1. Orden principal.
     * 2. Artículos.
     * 3. Extras de cada artículo.
     *
     * Todo ocurre en una sola transacción.
     */
    suspend fun guardarOrdenCompleta(
        orden: OrdenEntity,
        articulos: List<ArticuloTicket>
    ): OrdenEntity {

        require(orden.id == 0) {
            "Esta función solamente registra órdenes nuevas."
        }

        require(orden.identificador.isNotBlank()) {
            "La orden debe tener un identificador."
        }

        require(articulos.isNotEmpty()) {
            "No se puede guardar una orden sin productos."
        }

        articulos.forEach { articulo ->
            require(articulo.productoId > 0) {
                "Existe un artículo sin producto válido."
            }

            require(articulo.cantidad > 0) {
                "La cantidad debe ser mayor que cero."
            }

            require(articulo.precioBase >= 0.0) {
                "El precio base no puede ser negativo."
            }
        }

        /*
         * Recalculamos los importes para no confiar
         * en cifras externas potencialmente desactualizadas.
         */
        val totalCalculado =
            articulos.sumOf {
                it.subtotal
            }

        val totalRaulCalculado =
            articulos.sumOf {
                it.subtotalParaPropietario(
                    "Raúl"
                )
            }

        val totalCristianCalculado =
            articulos.sumOf {
                it.subtotalParaPropietario(
                    "Cristian"
                )
            }

        val ordenNormalizada =
            orden.copy(
                total = totalCalculado,
                totalRaul = totalRaulCalculado,
                totalCristian =
                    totalCristianCalculado,
                resumenProductos =
                    orden.resumenProductos.trim()
            )

        return database.withTransaction {

            /*
             * Primero guardamos la cabecera de la orden.
             */
            val ordenIdLong =
                ordenDao.guardarOrden(
                    ordenNormalizada
                )

            require(
                ordenIdLong in 1..Int.MAX_VALUE.toLong()
            ) {
                "Room devolvió un ID de orden inválido."
            }

            val ordenId =
                ordenIdLong.toInt()

            /*
             * Después guardamos cada artículo.
             */
            articulos.forEach { articulo ->

                val articuloId =
                    ordenDetalleDao.guardarArticulo(
                        articulo
                            .toOrdenArticuloEntity(
                                ordenId = ordenId
                            )
                    )

                /*
                 * Finalmente guardamos sus extras.
                 */
                val extrasEntities =
                    articulo.extras.map { extra ->
                        extra
                            .toOrdenArticuloExtraEntity(
                                articuloId =
                                    articuloId
                            )
                    }

                if (extrasEntities.isNotEmpty()) {
                    ordenDetalleDao.guardarExtras(
                        extrasEntities
                    )
                }
            }

            Log.d(
                TAG,
                "Orden $ordenId guardada con " +
                        "${articulos.size} líneas y " +
                        "${articulos.sumOf { it.extras.size }} extras."
            )

            ordenNormalizada.copy(
                id = ordenId
            )
        }
    }

    suspend fun obtenerOrdenCompleta(
        ordenId: Int
    ): OrdenConArticulos? {

        if (ordenId <= 0) {
            return null
        }

        return ordenDetalleDao
            .obtenerOrdenCompleta(
                ordenId
            )
    }
}