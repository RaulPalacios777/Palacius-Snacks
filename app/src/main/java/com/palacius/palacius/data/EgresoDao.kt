package com.palacius.palacius.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EgresoDao {
    @Insert
    suspend fun guardarEgreso(egreso: EgresoEntity)

    @Delete
    suspend fun eliminarEgreso(egreso: EgresoEntity)

    // TRAE TODOS LOS GASTOS (CITY CLUB, VERDULERÍA, ETC) DE LA SEMANA
    @Query("SELECT * FROM tabla_egresos WHERE corteSemanal = 0 ORDER BY fechaHora DESC")
    fun obtenerEgresosActivos(): Flow<List<EgresoEntity>>

    @Query("SELECT * FROM tabla_egresos WHERE corteDiario = 0 ORDER BY fechaHora DESC")
    fun obtenerEgresosDelDia(): Flow<List<EgresoEntity>>

    // BOTONES DE CIERRE
    @Query("UPDATE tabla_egresos SET corteDiario = 1 WHERE corteDiario = 0")
    suspend fun cerrarTurnoDiario()

    @Query("UPDATE tabla_egresos SET corteSemanal = 1 WHERE corteSemanal = 0")
    suspend fun cerrarSemana()
}