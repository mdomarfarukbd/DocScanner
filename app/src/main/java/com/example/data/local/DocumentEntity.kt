package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val tag: String = "Document",
    val isVault: Boolean = false,
    val pageCount: Int = 1,
    val coverImagePath: String = "",
    val ocrFullText: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
