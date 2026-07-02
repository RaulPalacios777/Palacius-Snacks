package com.palacius.palacius.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdenDetalleDao {

    /*
     * Guarda un producto de una orden.
     *
     * Regresa el ID generado para posteriormente poder
     * relacionar los extras con este producto.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun guardarArticulo(
        articulo: OrdenArticuloEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun guardarExtras(
        extras: List<OrdenArticuloExtraEntity>
    ): List<Long>

    @Update
    suspend fun actualizarArticulo(
        articulo: OrdenArticuloEntity
    ): Int

    @Update
    suspend fun actualizarExtra(
        extra: OrdenArticuloExtraEntity
    ): Int

    @Delete
    suspend fun eliminarArticulo(
        articulo: OrdenArticuloEntity
    ): Int

    @Delete
    suspend fun eliminarExtra(
        extra: OrdenArticuloExtraEntity
    ): Int

    @Query(
        """
        DELETE FROM orden_articulo_extras
        WHERE articulo_id = :articuloId
        """
    )
    suspend fun eliminarExtrasDelArticulo(
        articuloId: Long
    ): Int

    /*
     * Elimina los extras existentes y registra los nuevos.
     *
     * Todo se realiza dentro de una misma transacción.
     */
    @Transaction
    suspend fun reemplazarExtras(
        articuloId: Long,
        extras: List<OrdenArticuloExtraEntity>
    ) {
        eliminarExtrasDelArticulo(articuloId)

        if (extras.isNotEmpty()) {
            guardarExtras(extras)
        }
    }

    @Transaction
    @Query(
        """
        SELECT *
        FROM tabla_ordenes
        WHERE id = :ordenId
        LIMIT 1
        """
    )
    suspend fun obtenerOrdenCompleta(
        ordenId: Int
    ): OrdenConArticulos?

    @Transaction
    @Query(
        """
        SELECT *
        FROM tabla_ordenes
        WHERE id = :ordenId
        LIMIT 1
        """
    )
    fun observarOrdenCompleta(
        ordenId: Int
    ): Flow<OrdenConArticulos?>

    @Transaction
    @Query(
        """
        SELECT *
        FROM tabla_ordenes
        WHERE estado = 'Pendiente'
        ORDER BY fechaHora DESC
        """
    )
    fun observarOrdenesPendientesCompletas():
            Flow<List<OrdenConArticulos>>

    @Transaction
    @Query(
        """
        SELECT *
        FROM tabla_ordenes
        WHERE estado = 'Cobrado'
        AND corteSemanal = 0
        ORDER BY fechaHora DESC
        """
    )
    fun observarVentasActivasCompletas():
            Flow<List<OrdenConArticulos>>
}