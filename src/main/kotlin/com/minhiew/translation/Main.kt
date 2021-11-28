package com.minhiew.translation

import com.opencsv.CSVWriter
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardOpenOption

private const val DEFAULT_CONFIG_FILE = "translation-tool.conf"
private const val UNIQUE_ANDROID_STRINGS_FILE = "unique-android-strings.csv"
private const val UNIQUE_IOS_STRINGS_FILE = "unique-ios-strings.csv"
private const val EXACT_MATCH_FILE = "exact-matches.csv"
private const val DIFFERENCES_FILE = "differences.csv"

private var blockPlaceholderMismatch: Boolean = true

fun main(args: Array<String>) {
    val configFile = DEFAULT_CONFIG_FILE
    val config = ConfigFactory.parseFile(File(DEFAULT_CONFIG_FILE))
    println("Loading $configFile contents: $config\n")
    val appConfig = config.extract<AppConfig>()
    println("Parsed Config: $appConfig\n")

    val outputFolder = appConfig.outputDirectory.toFile()
    outputFolder.createDirectory()

    blockPlaceholderMismatch = appConfig.blockPlaceholderMismatch

    syncLanguage(outputFolder = outputFolder, bundle = appConfig.main)

    appConfig.localizations.forEach {
        syncLanguage(outputFolder = outputFolder, bundle = it)
    }
}

private fun syncLanguage(outputFolder: File, bundle: LocalizationBundle) {
    syncStrings(outputFolder = outputFolder, language = bundle.language, androidFile = bundle.androidFile.toFile(), iosFile = bundle.iosFile.toFile())
}

private fun syncStrings(outputFolder: File, language: String, androidFile: File, iosFile: File) {
    val subDirectory = File(outputFolder, language)
    subDirectory.createDirectory()

    println("Synchronizing language: $language for Android: $androidFile from iOS: $iosFile")

    val androidStrings: Map<String, String> = AndroidFileParser.parse(androidFile)
    println("Total Android strings: ${androidStrings.size}")

    val iosStrings: Map<String, String> = IOSFileParser.parse(iosFile)
    println("Total iOS strings: ${iosStrings.size}")

    val report: LocalizationReport = Analyzer.compare(androidStrings = androidStrings, iosStrings = iosStrings)

    writeUniqueAndroidStrings(outputFolder = subDirectory, report = report)
    writeUniqueIOSStrings(outputFolder = subDirectory, report = report)
    writeExactMatches(outputFolder = subDirectory, report = report)
    writeDifferences(outputFolder = subDirectory, report = report)

    writeFixedAndroidXmlFile(
        outputFolder = subDirectory,
        androidStringsFile = androidFile,
        report = report,
        blockPlaceholderMismatch = blockPlaceholderMismatch
    )
}

private fun writeUniqueAndroidStrings(outputFolder: File, report: LocalizationReport) {
    val uniqueStrings = report.uniqueAndroidStrings
    println("Unique Android Strings: ${uniqueStrings.size}")

    writeToCsv(outputFolder = outputFolder, fileName = UNIQUE_ANDROID_STRINGS_FILE) {
        writeNext(arrayOf("Android Key", "Android Value"))
        uniqueStrings.forEach { (key, value) ->
            writeNext(arrayOf(key, value))
        }
    }
}

private fun writeUniqueIOSStrings(outputFolder: File, report: LocalizationReport) {
    val uniqueStrings = report.uniqueIosStrings
    println("Unique iOS Strings: ${uniqueStrings.size}")
    writeToCsv(outputFolder = outputFolder, fileName = UNIQUE_IOS_STRINGS_FILE) {
        writeNext(arrayOf("iOS Key", "iOS Value"))
        uniqueStrings.forEach { (key, value) ->
            writeNext(arrayOf(key, value))
        }
    }
}

private fun writeExactMatches(outputFolder: File, report: LocalizationReport) {
    val exactMatches: List<StringComparison> = report.exactMatches.sortedBy { it.key }

    println("Exact matches: ${exactMatches.size}")
    writeToCsv(outputFolder = outputFolder, fileName = EXACT_MATCH_FILE) {
        writeNext(arrayOf("Key", "Android Value", "iOS Value"))
        exactMatches.forEach {
            writeNext(arrayOf(it.key, it.androidValue, it.iosValue))
        }
    }
}

private fun writeDifferences(outputFolder: File, report: LocalizationReport) {
    val differences: List<StringComparison> = report.differences
        .sortedWith(compareBy({ !it.hasMismatchedPlaceholders }, { !it.isCaseInsensitiveMatch }, { it.key }))

    println("Total Differences: ${differences.size}")

    val numberOfWarnings = report.mismatchedPlaceholders.size
    if (numberOfWarnings > 0) {
        println("\n!! WARNING: Detected $numberOfWarnings strings with mismatched placeholder counts !!\n")
    }

    val outputFileName = if (numberOfWarnings > 0) "$numberOfWarnings WARNINGS - $DIFFERENCES_FILE" else DIFFERENCES_FILE
    writeToCsv(outputFolder = outputFolder, fileName = outputFileName) {
        writeNext(arrayOf("Key", "Android Value", "iOS Value", "Has Mismatched Placeholder", "Is Case Insensitive Match"))
        differences.forEach {
            writeNext(arrayOf(it.key, it.androidValue, it.iosValue, it.hasMismatchedPlaceholders.toString(), it.isCaseInsensitiveMatch.toString()))
        }
    }
}

private fun writeFixedAndroidXmlFile(
    outputFolder: File,
    androidStringsFile: File,
    report: LocalizationReport,
    blockPlaceholderMismatch: Boolean
) {
    if (report.differences.isEmpty()) {
        println("Platform localizations match for shared keys!")
        return
    }

    println("Generating corrected android strings file where text differences are replaced with ios values.")
    val correctedAndroidStrings = AndroidTranslationGenerator.generateFixedAndroidXML(
        originalStringsXmlFile = androidStringsFile,
        report = report,
        blockPlaceholderMismatch = blockPlaceholderMismatch
    )

    val outputFile = File(outputFolder, androidStringsFile.name)
    outputFile.recreate()
    val fileWriter = Files.newBufferedWriter(outputFile.toPath(), Charset.forName("UTF-8"), StandardOpenOption.CREATE)
    fileWriter.use {
        val outputFormat = OutputFormat().apply {
            isExpandEmptyElements = true
        }
        val xmlWriter = XMLWriter(it, outputFormat)
        xmlWriter.write(correctedAndroidStrings)
        xmlWriter.close()
    }
}

//helper function to write to csv
private fun writeToCsv(outputFolder: File, fileName: String, lambda: CSVWriter.() -> Unit) {
    val file = File(outputFolder, fileName)
    file.recreate()
    val fileWriter = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"), StandardOpenOption.CREATE)
    getCsvWriter(fileWriter).use(lambda)
}

private fun getCsvWriter(writer: Writer): CSVWriter {
    return CSVWriter(
        writer,
        '\t',
        CSVWriter.NO_QUOTE_CHARACTER,
        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
        CSVWriter.DEFAULT_LINE_END
    )
}

private fun File.createDirectory() {
    if (!this.exists()) {
        this.mkdir()
    }
}

private fun File.recreate() {
    if (this.exists() && !this.isDirectory) {
        this.delete()
    }
}