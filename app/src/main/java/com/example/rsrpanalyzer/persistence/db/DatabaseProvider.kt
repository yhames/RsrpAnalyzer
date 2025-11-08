package com.example.rsrpanalyzer.persistence.db

import android.content.Context
import androidx.room.Room

/**
 * TODO: Replace with DI Injection for AppDatabase
 */
object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        INSTANCE?.let { return it } // Return if the instance is not null

        return synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                AppDatabase.DB_NAME
            )
                .fallbackToDestructiveMigration(true)   // TODO: Remove this in production
                .build()
                .also { INSTANCE = it } // Set the instance to the new instance
        }
    }
}