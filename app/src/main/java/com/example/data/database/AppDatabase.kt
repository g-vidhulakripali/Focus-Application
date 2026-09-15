package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.FocusSessionDao
import com.example.data.dao.QuestDao
import com.example.data.dao.UserRpgProfileDao
import com.example.data.model.FocusSessionEntity
import com.example.data.model.ThesisMilestoneQuestEntity
import com.example.data.model.UserRpgProfileEntity

@Database(
    entities = [
        FocusSessionEntity::class,
        UserRpgProfileEntity::class,
        ThesisMilestoneQuestEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun userRpgProfileDao(): UserRpgProfileDao
    abstract fun questDao(): QuestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thesis_grove_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
