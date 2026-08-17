package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE isVault = :isVault ORDER BY updatedAt DESC")
    fun getDocumentsByVault(isVault: Boolean): Flow<List<DocumentEntity>>

    @Query("""
        SELECT * FROM documents 
        WHERE (title LIKE '%' || :query || '%' OR ocrFullText LIKE '%' || :query || '%' OR tag LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchDocuments(query: String): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    fun getDocumentById(id: String): Flow<DocumentEntity?>

    @Query("SELECT * FROM documents WHERE id = :id LIMIT 1")
    suspend fun getDocumentDirect(id: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity)

    @Update
    suspend fun updateDocument(document: DocumentEntity)

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: String)
}
