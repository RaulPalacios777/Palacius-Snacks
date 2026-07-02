package com.palacius.palacius.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_ordenes")
data class OrdenEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val identificador: String,
    val estado: String,
    val total: Double,
    val costoTotal: Double = 0.0, // <-- NUEVO: Para saber cuánto de esta venta es para reinvertir
    val resumenProductos: String,
    val metodoPago: String = "",
    val fechaHora: Long = System.currentTimeMillis(),
    val totalRaul: Double = 0.0,
    val totalCristian: Double = 0.0,
    val corteDiario: Boolean = false,   // <-- NUEVO: Se marca true al final del día
    val corteSemanal: Boolean = false   // <-- NUEVO: Se marca true el domingo al repartir
)