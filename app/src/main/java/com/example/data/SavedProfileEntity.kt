package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_profiles")
data class SavedProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstNameBn: String,
    val lastNameBn: String,
    val firstNameEn: String,
    val lastNameEn: String,
    val usernameHandle: String,
    val genderCategory: String,
    val birthDay: Int,
    val birthMonthNumber: Int,
    val birthMonthEn: String,
    val birthMonthBn: String,
    val birthYear: Int,
    val ageYears: Int,
    val dobFormattedEn: String,
    val dobFormattedBn: String,
    val dobNumeric: String,
    val password: String,
    val tempEmail: String,
    val verificationCode: String = "",
    val accountStatus: String = "SAVED",
    val accountNote: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullNameBn: String get() = "$firstNameBn $lastNameBn"
    val fullNameEn: String get() = "$firstNameEn $lastNameEn"
}
