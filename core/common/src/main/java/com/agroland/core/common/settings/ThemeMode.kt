package com.agroland.core.common.settings

/** Тема режимі — DataStore-да «theme_mode» кілтінде сақталады. */
enum class ThemeMode(val persisted: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromPersisted(value: String?): ThemeMode =
            entries.firstOrNull { it.persisted == value } ?: SYSTEM
    }
}