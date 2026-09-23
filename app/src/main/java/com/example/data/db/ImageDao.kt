package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AssetCodePreset
import com.example.data.model.CapturedImageItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM captured_images ORDER BY id DESC")
    fun getAllImages(): Flow<List<CapturedImageItem>>

    @Query("SELECT * FROM captured_images WHERE id = :id")
    suspend fun getImageById(id: Long): CapturedImageItem?

    @Query("SELECT * FROM captured_images WHERE isMarked = 1")
    suspend fun getMarkedImages(): List<CapturedImageItem>

    @Query("SELECT * FROM captured_images WHERE isExported = 1 ORDER BY id DESC")
    fun getExportedImages(): Flow<List<CapturedImageItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: CapturedImageItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<CapturedImageItem>): List<Long>

    @Update
    suspend fun updateImage(image: CapturedImageItem)

    @Delete
    suspend fun deleteImage(image: CapturedImageItem)

    @Query("UPDATE captured_images SET isMarked = :marked")
    suspend fun setAllMarked(marked: Boolean)

    @Query("UPDATE captured_images SET isMarked = :marked WHERE id = :id")
    suspend fun setMarked(id: Long, marked: Boolean)

    @Query("DELETE FROM captured_images WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM captured_images")
    suspend fun getImageCount(): Int

    // Asset code presets
    @Query("SELECT * FROM asset_code_presets ORDER BY id ASC")
    fun getAllPresets(): Flow<List<AssetCodePreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: AssetCodePreset): Long

    @Delete
    suspend fun deletePreset(preset: AssetCodePreset)
}
