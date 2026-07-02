package com.palacius.palacius.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orden_articulos",
    foreignKeys = [
        ForeignKey(
            entity = OrdenEntity::class,
            parentColumns = ["id"],
            childColumns = ["orden_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(
            value = ["orden_id"],
            name = "index_orden_articulos_orden_id"
        ),
        Index(
            value = ["producto_id"],
            name = "index_orden_articulos_producto_id"
        )
    ]
)
data class OrdenArticuloEntity(

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_articulo")
    val id: Long = 0,

    /*
     * Orden a la que pertenece este producto.
     */
    @ColumnInfo(name = "orden_id")
    val ordenId: Int,

    /*
     * ID original del producto del menú.
     *
     * No se utiliza como llave foránea porque más adelante un producto
     * podría desactivarse o eliminarse del menú sin afectar tickets antiguos.
     */
    @ColumnInfo(name = "producto_id")
    val productoId: Int,

    /*
     * Copia histórica del nombre.
     *
     * Aunque después cambie el nombre del producto en el menú,
     * el ticket conservará el nombre con el que fue vendido.
     */
    @ColumnInfo(name = "producto_nombre")
    val productoNombre: String,

    /*
     * Actualmente las variantes todavía están en variantesRaw.
     * Por ahora este campo permanecerá null y se utilizará cuando
     * normalicemos las variantes.
     */
    @ColumnInfo(name = "variante_id")
    val varianteId: Int? = null,

    @ColumnInfo(name = "variante_nombre")
    val varianteNombre: String? = null,

    /*
     * Precio de una unidad, sin contar extras.
     *
     * Se almacena en centavos:
     * $85.50 = 8550
     */
    @ColumnInfo(name = "precio_base_centavos")
    val precioBaseCentavos: Long,

    @ColumnInfo(name = "cantidad")
    val cantidad: Int = 1,

    @ColumnInfo(name = "nota")
    val nota: String = "",

    /*
     * Propietario únicamente del producto base.
     * Los extras guardarán su propio propietario.
     */
    @ColumnInfo(name = "propietario_producto")
    val propietarioProducto: String,

    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: Long = System.currentTimeMillis()
)