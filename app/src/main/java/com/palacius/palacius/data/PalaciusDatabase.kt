package com.palacius.palacius.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OrdenEntity::class,
        ProductoMenuEntity::class,
        EgresoEntity::class,
        ToppingEntity::class,
        OrdenArticuloEntity::class,
        OrdenArticuloExtraEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class PalaciusDatabase : RoomDatabase() {

    abstract fun ordenDao(): OrdenDao

    abstract fun ordenDetalleDao(): OrdenDetalleDao

    abstract fun menuDao(): MenuDao

    abstract fun egresoDao(): EgresoDao

    companion object {

        @Volatile
        private var INSTANCE: PalaciusDatabase? = null

        fun obtenerInstancia(
            context: Context
        ): PalaciusDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val nuevaInstancia =
                        Room.databaseBuilder(
                            context.applicationContext,
                            PalaciusDatabase::class.java,
                            "palacius_database"
                        )
                            /*
                             * Room puede encadenar estas migraciones:
                             *
                             * 8 -> 9 -> 10
                             * 9 -> 10
                             */
                            .addMigrations(
                                MIGRATION_8_9,
                                MIGRATION_9_10
                            )

                            /*
                             * No usamos fallbackToDestructiveMigration().
                             *
                             * Si falta una migración, preferimos que la
                             * aplicación reporte el problema en vez de
                             * borrar ventas, menú y egresos.
                             */
                            .build()

                    INSTANCE = nuevaInstancia

                    nuevaInstancia
                }
        }
    }
}