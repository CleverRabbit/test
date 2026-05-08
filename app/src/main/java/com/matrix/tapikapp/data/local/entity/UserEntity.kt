package com.matrix.tapikapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.matrix.tapikapp.data.local.converter.Converters

/**
 * Entity пользователя для Room database.
 * 
 * Хранит данные пользователя в локальной БД для offline-доступа.
 */
@Entity(tableName = "users")
@TypeConverters(Converters::class)
data class UserEntity(
    @PrimaryKey
    val id: String,
    
    val phone: String,
    
    val firstName: String,
    
    val lastName: String? = null,
    
    val avatarUrl: String? = null,
    
    val isOnline: Boolean = false,
    
    val lastSeen: Long? = null,
    
    // Метка времени последней синхронизации
    val syncedAt: Long = System.currentTimeMillis()
)
