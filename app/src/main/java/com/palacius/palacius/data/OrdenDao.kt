package com.palacius.palacius.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdenDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun guardarOrden(
        orden: OrdenEntity
    ): Long

    @Update
    suspend fun actualizarOrden(
        orden: OrdenEntity
    ): Int

    @Delete
    suspend fun eliminarOrden(
        orden: OrdenEntity
    ): Int

    @Query(
        """
        SELECT *
        FROM tabla_ordenes
        WHERE id = :ordenId
        LIMIT 1
        """
    )
    suspend fun obtenerOrdenPorId(
        ordenId: Int
    ): OrdenEntity?

    @Query(
        """
        SELECT *
        FROM tabla_ordenes
        WHERE estado = 'Pendiente'
        ORDER BY fechaHora DESC
        """
    )
    fun obtenerOrdenesPendientes():
            Flow<List<OrdenEntity>>

    /*
     * Obtiene solamente las ventas cobradas
     * que todavía no pertenecen a un corte semanal cerrado.
     */
    @Query(
        """
        SELECT *
        FROM tabla_ordenes
        WHERE estado = 'Cobrado'
        AND corteSemanal = 0
        ORDER BY fechaHora DESC
        """
    )
    fun obtenerVentasActivas():
            Flow<List<OrdenEntity>>

    /*
     * No se deben marcar órdenes pendientes como cerradas.
     */
    @Query(
        """
        UPDATE tabla_ordenes
        SET corteDiario = 1
        WHERE estado = 'Cobrado'
        AND corteDiario = 0
        """
    )
    suspend fun cerrarTurnoDiario(): Int

    /*
     * Cuando se cierra la semana también consideramos
     * terminado su corte diario.
     */
    @Query(
        """
        UPDATE tabla_ordenes
        SET corteSemanal = 1,
            corteDiario = 1
        WHERE estado = 'Cobrado'
        AND corteSemanal = 0
        """
    )
    suspend fun cerrarSemana(): Int
}