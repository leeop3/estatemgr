package com.estate.manager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.estate.manager.data.db.*
import com.estate.manager.data.models.*

@Database(
    entities = [
        BunchRecord::class,
        TractorLocation::class,
        GangTrack::class,
        ChatMessage::class,
        Alert::class,
        Peer::class
    ],
    version  = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bunchDao():   BunchDao
    abstract fun tractorDao(): TractorDao
    abstract fun gangDao():    GangDao
    abstract fun chatDao():    ChatDao
    abstract fun alertDao():   AlertDao
    abstract fun peerDao():    PeerDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "estate.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
    }
}
