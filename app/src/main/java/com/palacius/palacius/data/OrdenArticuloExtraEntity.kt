package com.palacius.palacius.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orden_articulo_extras",
    foreignKeys = [
        ForeignKey(
            entity = OrdenArticuloEntity::class,
            parentColumns = ["id_articulo"],
            childColumns = ["articulo_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(
            value = ["articulo_id"],
            name = "index_orden_articulo_extras_articulo_id"
        ),
        Index(
            value = ["topping_id"],
            name = "index_orden_articulo_extras_topping_id"
        )
    ]
)
data class OrdenArticuloExtraEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_articulo_extra")
    val id: Long = 0,

    /*
     * Producto del ticket al que pertenece este extra.
     */
    @ColumnInfo(name = "articulo_id")
    val articuloId: Long,

    /*
     * Puede ser null para permitir en el futuro extras manuales
     * o conservar extras cuyo registro original haya sido eliminado.
     */
    @ColumnInfo(name = "topping_id")
    val toppingId: Int?,

    /*
     * Nombre histórico del extra.
     */
    @ColumnInfo(name = "extra_nombre")
    val nombre: String,

    /*
     * Precio de una unidad del extra en centavos.
     */
    @ColumnInfo(name = "precio_unitario_centavos")
    val precioUnitarioCentavos: Long,

    /*
     * Ejemplo:
     *
     * Hamburguesa
     * + 2 carnes extras
     */
    @ColumnInfo(name = "cantidad_por_producto")
    val cantidadPorProducto: Int = 1,

    /*
     * El propietario del extra puede ser diferente
     * al propietario del producto.
     */
    @ColumnInfo(name = "propietario_extra")
    val propietario: String
)