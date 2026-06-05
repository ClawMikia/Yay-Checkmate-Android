package com.yaycheckmate.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yaycheckmate.data.dao.ObjectItemDao
import com.yaycheckmate.data.dao.SearchSessionDao
import com.yaycheckmate.data.dao.UserStatsDao
import com.yaycheckmate.data.entity.ObjectItem
import com.yaycheckmate.data.entity.SearchSession
import com.yaycheckmate.data.entity.UserStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ObjectItem::class, SearchSession::class, UserStats::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun objectItemDao(): ObjectItemDao
    abstract fun searchSessionDao(): SearchSessionDao
    abstract fun userStatsDao(): UserStatsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yaycheckmate_db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            INSTANCE?.userStatsDao()?.insertStats(UserStats())
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
