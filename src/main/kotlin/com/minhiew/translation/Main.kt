package com.minhiew.translation

import com.opencsv.CSVWriter
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.dom4j.Document
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

fun main(args: Array<String>) {
    val configFile = DEFAULT_CONFIG_FILE
    val config = ConfigFactory.parseFile(File(DEFAULT_CONFIG_FILE))
    println("Loading $configFile contents: $config\n")
    val appConfig = config.extract<AppConfig>()
    println("Parsed Config: $appConfig\n")

    val outputFolder = appConfig.outputDirectory.toFile()
    outputFolder.createDirectory()

    val blockPlaceholderMismatch = appConfig.blockPlaceholderMismatch

    syncMain(
        rootOutputFolder = outputFolder,
        bundle = appConfig.main,
        blockPlaceholderMismatch = blockPlaceholderMismatch
    )

    appConfig.localizations.forEach {
        syncOtherLocales(
            rootOutputFolder = outputFolder,
            mainBundle = appConfig.main,
            localeBundle = it,
            blockPlaceholderMismatch = blockPlaceholderMismatch,
            useMainAsBaseAndroidTemplate = appConfig.useMainAsBaseAndroidTemplate
        )
    }
}

private fun syncMain(rootOutputFolder: File, bundle: LocalizationBundle, blockPlaceholderMismatch: Boolean) {
    println("Synchronizing main language: ${bundle.language} for Android: ${bundle.androidFile} from iOS: ${bundle.iosFile}")

    val androidFile = bundle.androidFile.toFile()
    val androidStrings: Map<String, String> = AndroidFileParser.parse(androidFile)
    val androidXML = AndroidFileParser.getDocument(androidFile)

    val iosStrings: Map<String, String> = IOSFileParser.parse(bundle.iosFile.toFile())

    val subDirectory = File(rootOutputFolder, bundle.language)
    subDirectory.createDirectory()

    syncStrings(outputFolder = subDirectory, androidStrings = androidStrings, iosStrings = iosStrings, androidXMLDocument = androidXML, blockPlaceholderMismatch = blockPlaceholderMismatch)
}

private fun syncOtherLocales(
    rootOutputFolder: File,
    mainBundle: LocalizationBundle,
    localeBundle: LocalizationBundle,
    blockPlaceholderMismatch: Boolean,
    useMainAsBaseAndroidTemplate: Boolean
) {
    val localeLanguage = localeBundle.language
    println("Synchronizing localization language: $localeLanguage for Android: ${localeBundle.androidFile} from iOS: ${localeBundle.iosFile}")

    val androidLocaleFile = localeBundle.androidFile.toFile()
    val androidLocaleStrings: Map<String, String> = AndroidFileParser.parse(androidLocaleFile)
    val androidLocaleXML = if (useMainAsBaseAndroidTemplate) {
        println("Using main android strings as base template. Merging $localeLanguage ${localeBundle.androidFile.fileName} into main ${mainBundle.androidFile.fileName}")
        //merge this locale into the main android xml document.
        val mainAndroidXML = AndroidFileParser.getDocument(mainBundle.androidFile.toFile())
        AndroidTranslationGenerator.mergeAndroidTranslation(mainTemplate = mainAndroidXML, otherLocale = androidLocaleStrings)
    } else {
        print("Using $localeLanguage ${localeBundle.androidFile.fileName} directly")
        //use the locale xml document directly
        AndroidFileParser.getDocument(androidLocaleFile)
    }

    val iosStrings: Map<String, String> = IOSFileParser.parse(localeBundle.iosFile.toFile())

    val subDirectory = File(rootOutputFolder, localeBundle.language)
    subDirectory.createDirectory()

    syncStrings(outputFolder = subDirectory, androidStrings = androidLocaleStrings, iosStrings = iosStrings, androidXMLDocument = androidLocaleXML, blockPlaceholderMismatch = blockPlaceholderMismatch)
}

private fun syncStrings(
    outputFolder: File,
    androidStrings: Map<String, String>,
    iosStrings: Map<String, String>,
    androidXMLDocument: Document,
    blockPlaceholderMismatch: Boolean,
) {
    println("Total Android strings: ${androidStrings.size}")
    println("Total iOS strings: ${iosStrings.size}")

    val report: LocalizationReport = Analyzer.compare(androidStrings = androidStrings, iosStrings = iosStrings)

    writeUniqueAndroidStrings(outputFolder = outputFolder, report = report)
    writeUniqueIOSStrings(outputFolder = outputFolder, report = report)
    writeExactMatches(outputFolder = outputFolder, report = report)
    writeDifferences(outputFolder = outputFolder, report = report)

    writeFixedAndroidXmlFile(
        outputFolder = outputFolder,
        androidXMLDocument = androidXMLDocument,
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
    androidXMLDocument: Document,
    report: LocalizationReport,
    blockPlaceholderMismatch: Boolean
) {
    if (report.differences.isEmpty()) {
        println("Platform localizations match for shared keys!")
        return
    }

    println("Generating corrected android strings file where text differences are replaced with ios values.")
    val correctedAndroidStrings = AndroidTranslationGenerator.generateFixedAndroidXML(
        document = androidXMLDocument,
        report = report,
        blockPlaceholderMismatch = blockPlaceholderMismatch
    )

    val outputFile = File(outputFolder, "strings.xml")
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