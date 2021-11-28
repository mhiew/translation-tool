package com.minhiew.translation

import java.nio.file.Path

data class AppConfig(
    val outputDirectory: Path,
    val blockPlaceholderMismatch: Boolean = true,
    val useMainAsBaseAndroidTemplate: Boolean = true,
    val main: LocalizationBundle,
    val localizations: List<LocalizationBundle> = emptyList()
)

data class LocalizationBundle(
    val language: String,
    val androidFile: Path,
    val iosFile: Path,
)