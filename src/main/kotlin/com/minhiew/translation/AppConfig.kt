package com.minhiew.translation

import java.nio.file.Path

data class AppConfig(
    val outputDirectory: Path,
    val cleanOutputDirectory: Boolean = false,
    val blockReplacementOnPlaceholderCountMismatch: Boolean = true,
    val useMainAndroidFileAsBaseTemplate: Boolean = true,
    val replaceAndroidSourceFile: Boolean = false,
    val main: LocalizationBundle,
    val localizations: List<LocalizationBundle> = emptyList(),
    val textReplacements: List<TextReplacement> = emptyList()
)

data class LocalizationBundle(
    val language: String,
    val androidFile: Path,
    val iosFile: Path,
)

data class TextReplacement(
    val target: String,
    val replacementValue: String
)