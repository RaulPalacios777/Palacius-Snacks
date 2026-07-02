package com.palacius.palacius.data

import androidx.room.Embedded
import androidx.room.Relation

data class ArticuloConExtras(

    @Embedded
    val articulo: OrdenArticuloEntity,

    @Relation(
        parentColumn = "id_articulo",
        entityColumn = "articulo_id"
    )
    val extras: List<OrdenArticuloExtraEntity>
)

data class OrdenConArticulos(

    @Embedded
    val orden: OrdenEntity,

    @Relation(
        entity = OrdenArticuloEntity::class,
        parentColumn = "id",
        entityColumn = "orden_id"
    )
    val articulos: List<ArticuloConExtras>
)