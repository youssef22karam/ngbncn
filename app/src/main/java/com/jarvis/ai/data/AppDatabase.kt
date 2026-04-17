package com.jarvis.ai.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM models")
    fun getAllModels(): Flow<List<ModelInfo>>

    @Query("SELECT * FROM models WHERE id = :id")
    suspend fun getModel(id: String): ModelInfo?

    @Query("SELECT * FROM models WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveModel(): ModelInfo?

    @Upsert
    suspend fun upsertModel(model: ModelInfo)

    @Query("UPDATE models SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: ModelStatus)

    @Query("UPDATE models SET downloadProgress = :progress, status = :status WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int, status: ModelStatus)

    @Query("UPDATE models SET isActive = 0")
    suspend fun clearActiveModel()

    @Query("UPDATE models SET isActive = 1 WHERE id = :id")
    suspend fun setActiveModel(id: String)

    @Query("UPDATE models SET localPath = :path, status = :status WHERE id = :id")
    suspend fun setLocalPath(id: String, path: String, status: ModelStatus)

    @Delete
    suspend fun deleteModel(model: ModelInfo)
}

@Database(
    entities = [ModelInfo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modelDao(): ModelDao
}

class Converters {
    @TypeConverter
    fun fromModelType(value: ModelType): String = value.name

    @TypeConverter
    fun toModelType(value: String): ModelType = ModelType.valueOf(value)

    @TypeConverter
    fun fromModelStatus(value: ModelStatus): String = value.name

    @TypeConverter
    fun toModelStatus(value: String): ModelStatus = ModelStatus.valueOf(value)
}
