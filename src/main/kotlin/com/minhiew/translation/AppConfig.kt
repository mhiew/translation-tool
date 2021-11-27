package com.minhiew.translation

import java.io.File

data class AppConfig(
    val outputDirectory: File,
    val blockPlaceholderMismatch: Boolean = true,
    val main: LocalizationBundle,
    val localizations: List<LocalizationBundle> = emptyList()
)

data class LocalizationBundle(
    val language: String,
    val androidFile: File,
    val iosFile: File,
)