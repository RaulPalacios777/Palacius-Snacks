package com.palacius.palacius.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuDao {

    /*
     * Alta de un producto nuevo.
     *
     * ABORT evita sobrescribir accidentalmente
     * otro producto existente.
     */
    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertarProducto(
        producto: ProductoMenuEntity
    ): Long

    /*
     * Actualiza usando la llave primaria id.
     *
     * Devuelve la cantidad de filas modificadas.
     * El resultado correcto debe ser 1.
     */
    @Update
    suspend fun actualizarProducto(
        producto: ProductoMenuEntity
    ): Int

    /*
     * Se conserva temporalmente por compatibilidad.
     * Ya no debe utilizarse desde PantallaAdmin.
     */
    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun guardarProducto(
        producto: ProductoMenuEntity
    )

    @Query(
        """
        SELECT *
        FROM tabla_menu
        WHERE activo = 1
        ORDER BY categoria ASC, nombre ASC
        """
    )
    fun obtenerMenuCompleto():
            Flow<List<ProductoMenuEntity>>

    @Query(
        """
        SELECT *
        FROM tabla_menu
        ORDER BY activo DESC, categoria ASC, nombre ASC
        """
    )
    fun obtenerMenuAdministracion():
            Flow<List<ProductoMenuEntity>>

    @Query(
        """
        SELECT *
        FROM tabla_menu
        WHERE id = :productoId
        LIMIT 1
        """
    )
    suspend fun obtenerProductoPorId(
        productoId: Int
    ): ProductoMenuEntity?

    @Query(
        """
        UPDATE tabla_menu
        SET activo = 0
        WHERE id = :productoId
        """
    )
    suspend fun desactivarProductoPorId(
        productoId: Int
    ): Int

    @Query(
        """
        UPDATE tabla_menu
        SET activo = 1
        WHERE id = :productoId
        """
    )
    suspend fun reactivarProductoPorId(
        productoId: Int
    ): Int

    suspend fun eliminarProducto(
        producto: ProductoMenuEntity
    ) {
        desactivarProductoPorId(
            productoId = producto.id
        )
    }

    // =========================================================
    // TOPPINGS
    // =========================================================

    @Insert(
        onConflict = OnConflictStrategy.REPLACE
    )
    suspend fun guardarTopping(
        topping: ToppingEntity
    )

    @Query(
        """
        SELECT *
        FROM tabla_extras_v3
        WHERE activo_extra = 1
        ORDER BY nombre_extra ASC
        """
    )
    fun obtenerToppings():
            Flow<List<ToppingEntity>>

    @Query(
        """
        SELECT *
        FROM tabla_extras_v3
        ORDER BY activo_extra DESC, nombre_extra ASC
        """
    )
    fun obtenerToppingsAdministracion():
            Flow<List<ToppingEntity>>

    @Query(
        """
        SELECT *
        FROM tabla_extras_v3
        WHERE id_extra = :toppingId
        LIMIT 1
        """
    )
    suspend fun obtenerToppingPorId(
        toppingId: Int
    ): ToppingEntity?

    @Query(
        """
        UPDATE tabla_extras_v3
        SET activo_extra = 0
        WHERE id_extra = :toppingId
        """
    )
    suspend fun desactivarToppingPorId(
        toppingId: Int
    ): Int

    @Query(
        """
        UPDATE tabla_extras_v3
        SET activo_extra = 1
        WHERE id_extra = :toppingId
        """
    )
    suspend fun reactivarToppingPorId(
        toppingId: Int
    ): Int

    suspend fun eliminarTopping(
        topping: ToppingEntity
    ) {
        desactivarToppingPorId(
            toppingId = topping.id
        )
    }
}