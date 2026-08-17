package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentPageDao {
    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getPagesForDocument(documentId: String): Flow<List<DocumentPageEntity>>

    @Query("SELECT * FROM document_pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getPagesListForDocument(documentId: String): List<DocumentPageEntity>

    @Query("SELECT * FROM document_pages WHERE id = :pageId LIMIT 1")
    suspend fun getPageById(pageId: String): DocumentPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DocumentPageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: DocumentPageEntity)

    @Update
    suspend fun updatePage(page: DocumentPageEntity)

    @Delete
    suspend fun deletePage(page: DocumentPageEntity)

    @Query("DELETE FROM document_pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: String)
}
