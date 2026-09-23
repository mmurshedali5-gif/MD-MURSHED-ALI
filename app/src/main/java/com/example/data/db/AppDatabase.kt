package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AssetCodePreset
import com.example.data.model.CapturedImageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CapturedImageItem::class, AssetCodePreset::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gps_cam_database.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate standard asset code presets
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(context).imageDao()
                            val defaults = listOf(
                                AssetCodePreset(code = "TLM-ER2-2026/09", description = "Transmission Line Module East-2"),
                                AssetCodePreset(code = "SUB-400KV-SEC3", description = "400kV Substation Yard Bay 3"),
                                AssetCodePreset(code = "TOW-HT-A44", description = "High Tension Tower Footing A44"),
                                AssetCodePreset(code = "DIST-TRANS-09", description = "Distribution Transformer Point 09"),
                                AssetCodePreset(code = "PIPE-VALVE-V12", description = "Gas Pipeline Section Valve 12")
                            )
                            defaults.forEach { dao.insertPreset(it) }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
