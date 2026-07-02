package com.palacius.palacius.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_egresos")
data class EgresoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val concepto: String,
    val cantidad: Int = 1,
    val costoUnitario: Double = 0.0,
    val total: Double,
    val cajaDestino: String,
    val fechaHora: Long = System.currentTimeMillis(),
    val corteDiario: Boolean = false,
    val corteSemanal: Boolean = false
)