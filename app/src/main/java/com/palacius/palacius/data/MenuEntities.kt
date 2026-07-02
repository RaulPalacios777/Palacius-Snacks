package com.palacius.palacius.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_menu")
data class ProductoMenuEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val nombre: String,

    val precioBase: Double,

    /*
     * Costo estimado para producir una unidad.
     * Más adelante será alimentado por la calculadora de costos.
     */
    val costoProduccion: Double = 0.0,

    val icono: String,

    val categoria: String,

    val propietario: String,

    /*
     * Se mantiene temporalmente hasta que las variantes
     * tengan su propia tabla.
     */
    val variantesRaw: String = "",

    /*
     * Un producto inactivo deja de aparecer en el POS,
     * pero permanece en la base de datos.
     */
    @ColumnInfo(name = "activo")
    val activo: Boolean = true
) {

    fun obtenerVariantes(): List<Pair<String, Double>> {

        if (variantesRaw.isBlank()) {
            return emptyList()
        }

        return variantesRaw
            .split(",")
            .mapNotNull { varianteTexto ->

                val partes =
                    varianteTexto.split("|")

                if (partes.size != 2) {
                    return@mapNotNull null
                }

                val nombreVariante =
                    partes[0].trim()

                val precioVariante =
                    partes[1]
                        .trim()
                        .toDoubleOrNull()
                        ?: return@mapNotNull null

                if (nombreVariante.isBlank()) {
                    return@mapNotNull null
                }

                nombreVariante to precioVariante
            }
    }
}

@Entity(tableName = "tabla_extras_v3")
data class ToppingEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_extra")
    val id: Int = 0,

    @ColumnInfo(name = "nombre_extra")
    val nombre: String,

    @ColumnInfo(name = "precio_extra")
    val precio: Double,

    @ColumnInfo(name = "propietario_extra")
    val propietario: String,

    /*
     * Los extras desactivados se conservan para mantener
     * referencias históricas y futuras estadísticas.
     */
    @ColumnInfo(name = "activo_extra")
    val activo: Boolean = true
)