package com.palacius.palacius.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/*
 * Migración de versión 8 a 9.
 *
 * Crea las tablas del ticket estructurado.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `orden_articulos` (
                `id_articulo` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `orden_id` INTEGER NOT NULL,
                `producto_id` INTEGER NOT NULL,
                `producto_nombre` TEXT NOT NULL,
                `variante_id` INTEGER,
                `variante_nombre` TEXT,
                `precio_base_centavos` INTEGER NOT NULL,
                `cantidad` INTEGER NOT NULL,
                `nota` TEXT NOT NULL,
                `propietario_producto` TEXT NOT NULL,
                `fecha_creacion` INTEGER NOT NULL,
                FOREIGN KEY(`orden_id`)
                    REFERENCES `tabla_ordenes`(`id`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_orden_articulos_orden_id`
            ON `orden_articulos` (`orden_id`)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_orden_articulos_producto_id`
            ON `orden_articulos` (`producto_id`)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `orden_articulo_extras` (
                `id_articulo_extra` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `articulo_id` INTEGER NOT NULL,
                `topping_id` INTEGER,
                `extra_nombre` TEXT NOT NULL,
                `precio_unitario_centavos` INTEGER NOT NULL,
                `cantidad_por_producto` INTEGER NOT NULL,
                `propietario_extra` TEXT NOT NULL,
                FOREIGN KEY(`articulo_id`)
                    REFERENCES `orden_articulos`(`id_articulo`)
                    ON UPDATE NO ACTION
                    ON DELETE CASCADE
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_orden_articulo_extras_articulo_id`
            ON `orden_articulo_extras` (`articulo_id`)
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
            `index_orden_articulo_extras_topping_id`
            ON `orden_articulo_extras` (`topping_id`)
            """.trimIndent()
        )
    }
}

/*
 * Migración de versión 9 a 10.
 *
 * Agrega desactivación lógica a productos y toppings.
 * Todos los registros actuales quedan activos por defecto.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {

        database.execSQL(
            """
            ALTER TABLE tabla_menu
            ADD COLUMN activo INTEGER NOT NULL DEFAULT 1
            """.trimIndent()
        )

        database.execSQL(
            """
            ALTER TABLE tabla_extras_v3
            ADD COLUMN activo_extra INTEGER NOT NULL DEFAULT 1
            """.trimIndent()
        )
    }
}

/*
 * Migración de versión 10 a 11.
 *
 * Agrega las categorías permitidas para toppings.
 *
 * Todos los toppings actuales quedan como "Todos"
 * para conservar el comportamiento existente hasta
 * que se configuren desde Administración.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {

    override fun migrate(
        database: SupportSQLiteDatabase
    ) {
        database.execSQL(
            """
            ALTER TABLE tabla_extras_v3
            ADD COLUMN categorias_permitidas_raw
            TEXT NOT NULL DEFAULT 'Todos'
            """.trimIndent()
        )
    }
}