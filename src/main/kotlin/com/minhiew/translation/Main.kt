package com.minhiew.translation

import com.opencsv.CSVWriter
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

private const val DEFAULT_CONFIG_FILE = "translation-tool.conf"

private const val UNIQUE_ANDROID_STRINGS_FILE = "unique-android-strings.csv"
private const val UNIQUE_IOS_STRINGS_FILE = "unique-ios-strings.csv"
private const val EXACT_MATCH_FILE = "exact-matches.csv"
private const val DIFFERENCES_FILE = "differences.csv"

fun main(args: Array<String>) {
    val config = ConfigFactory.parseFile(File(DEFAULT_CONFIG_FILE))
    println("Loading $DEFAULT_CONFIG_FILE contents: $config\n")
    val appConfig = config.extract<AppConfig>()
    println("Parsed Config: $appConfig\n")

    //create the root output directory
    val rootOutputDirectory = appConfig.outputDirectory
    if (appConfig.cleanOutputDirectory) {
        println("Deleting output directory: ${appConfig.outputDirectory}")
        rootOutputDirectory.toFile().deleteRecursively()
    }
    if (!rootOutputDirectory.exists()) {
        rootOutputDirectory.createDirectories()
    }

    //synchronize main locale
    synchronizeLocales(
        rootOutputFolder = rootOutputDirectory,
        mainBundle = appConfig.main,
        localeBundle = appConfig.main,
        blockReplacementOnPlaceholderCountMismatch = appConfig.blockReplacementOnPlaceholderCountMismatch,
        useMainAndroidFileAsBaseTemplate = false,
        replaceAndroidSourceFile = appConfig.replaceAndroidSourceFile
    )

    //synchronize other locales
    appConfig.localizations.forEach {
        synchronizeLocales(
            rootOutputFolder = rootOutputDirectory,
            mainBundle = appConfig.main,
            localeBundle = it,
            blockReplacementOnPlaceholderCountMismatch = appConfig.blockReplacementOnPlaceholderCountMismatch,
            useMainAndroidFileAsBaseTemplate = appConfig.useMainAndroidFileAsBaseTemplate,
            replaceAndroidSourceFile = appConfig.replaceAndroidSourceFile
        )
    }
}

private fun synchronizeLocales(
    rootOutputFolder: Path,
    mainBundle: LocalizationBundle,
    localeBundle: LocalizationBundle,
    blockReplacementOnPlaceholderCountMismatch: Boolean,
    useMainAndroidFileAsBaseTemplate: Boolean,
    replaceAndroidSourceFile: Boolean,
) {
    val localeLanguage = localeBundle.language
    println("Synchronizing localization language: $localeLanguage for Android: ${localeBundle.androidFile} from iOS: ${localeBundle.iosFile}")

    val outputFolder = rootOutputFolder.resolve(localeLanguage)
    outputFolder.createDirectories()

    val iosStrings: Map<String, String> = IOSFileParser.parse(localeBundle.iosFile.toFile())
    println("Total iOS strings: ${iosStrings.size}")

    val androidLocaleStrings: Map<String, String> = AndroidFileParser.parse(localeBundle.androidFile.toFile())
    println("Total Android strings: ${androidLocaleStrings.size}")

    val report: LocalizationReport = Analyzer.compare(androidStrings = androidLocaleStrings, iosStrings = iosStrings)

    //create logs for text copy comparisons between platforms
    writeUniqueAndroidStrings(outputFolder = outputFolder, report = report)
    writeUniqueIOSStrings(outputFolder = outputFolder, report = report)
    writeExactMatches(outputFolder = outputFolder, report = report)
    writeDifferences(outputFolder = outputFolder, report = report)

    //create synchronized android file with shared text copied from iOS
    val synchronizedFile = writeSynchronizedAndroidFile(
        outputFolder = outputFolder,
        mainAndroidFile = mainBundle.androidFile,
        localeAndroidFile = localeBundle.androidFile,
        report = report,
        blockReplacementOnPlaceholderCountMismatch = blockReplacementOnPlaceholderCountMismatch,
        useMainAndroidFileAsBaseTemplate = useMainAndroidFileAsBaseTemplate
    )

    if (replaceAndroidSourceFile && synchronizedFile?.exists() == true) {
        val from = synchronizedFile
        val to = localeBundle.androidFile
        println("Copying synchronized file from: $from to source: $to")
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING)
    }
}

private fun writeUniqueAndroidStrings(outputFolder: Path, report: LocalizationReport) {
    val uniqueStrings = report.uniqueAndroidStrings
    println("Unique Android Strings: ${uniqueStrings.size}")

    val outputFile = outputFolder.resolve(UNIQUE_ANDROID_STRINGS_FILE)
    writeToCsv(outputFile) {
        writeNext(arrayOf("Android Key", "Android Value"))
        uniqueStrings.forEach { (key, value) ->
            writeNext(arrayOf(key, value))
        }
    }
}

private fun writeUniqueIOSStrings(outputFolder: Path, report: LocalizationReport) {
    val uniqueStrings = report.uniqueIosStrings
    println("Unique iOS Strings: ${uniqueStrings.size}")
    val outputFile = outputFolder.resolve(UNIQUE_IOS_STRINGS_FILE)
    writeToCsv(outputFile) {
        writeNext(arrayOf("iOS Key", "iOS Value"))
        uniqueStrings.forEach { (key, value) ->
            writeNext(arrayOf(key, value))
        }
    }
}

private fun writeExactMatches(outputFolder: Path, report: LocalizationReport) {
    val exactMatches: List<StringComparison> = report.exactMatches.sortedBy { it.key }

    println("Exact matches: ${exactMatches.size}")
    val outputFile = outputFolder.resolve(EXACT_MATCH_FILE)
    writeToCsv(outputFile) {
        writeNext(arrayOf("Key", "Android Value", "iOS Value"))
        exactMatches.forEach {
            writeNext(arrayOf(it.key, it.androidValue, it.iosValue))
        }
    }
}

private fun writeDifferences(outputFolder: Path, report: LocalizationReport) {
    val differences: List<StringComparison> = report.differences
        .sortedWith(compareBy({ !it.hasMismatchedPlaceholders }, { !it.isCaseInsensitiveMatch }, { it.key }))

    println("Total Differences: ${differences.size}")

    val numberOfWarnings = report.mismatchedPlaceholders.size
    if (numberOfWarnings > 0) {
        println("\n!! WARNING: Detected $numberOfWarnings strings with mismatched placeholder counts !!\n")
    }

    val outputFileName = if (numberOfWarnings > 0) "$numberOfWarnings WARNINGS - $DIFFERENCES_FILE" else DIFFERENCES_FILE
    val outputFile = outputFolder.resolve(outputFileName)
    writeToCsv(outputFile) {
        writeNext(arrayOf("Key", "Android Value", "iOS Value", "Has Mismatched Placeholder", "Is Case Insensitive Match"))
        differences.forEach {
            writeNext(arrayOf(it.key, it.androidValue, it.iosValue, it.hasMismatchedPlaceholders.toString(), it.isCaseInsensitiveMatch.toString()))
        }
    }
}

private fun writeSynchronizedAndroidFile(
    outputFolder: Path,
    mainAndroidFile: Path,
    localeAndroidFile: Path,
    report: LocalizationReport,
    blockReplacementOnPlaceholderCountMismatch: Boolean,
    useMainAndroidFileAsBaseTemplate: Boolean
): Path? {
    if (report.differences.isEmpty()) {
        println("All shared text copy match between platforms. No synchronized android file is required.")
        return null
    }

    //decide which base android file to use. either merging the other locale into the main android xml file or using the locale directly
    val androidXMLTemplate = if (useMainAndroidFileAsBaseTemplate) {
        println("Using the main android file as the base template. Merging $localeAndroidFile into $mainAndroidFile.")
        val mainAndroidXML = AndroidFileParser.getDocument(mainAndroidFile.toFile())
        val otherLocaleStrings = AndroidFileParser.parse(localeAndroidFile.toFile())
        AndroidTranslationGenerator.mergeAndroidTranslation(mainTemplate = mainAndroidXML, otherLocale = otherLocaleStrings)
    } else {
        print("Using the locale file directly: $localeAndroidFile")
        AndroidFileParser.getDocument(localeAndroidFile.toFile())
    }

    println("Generating synchronized android strings file where text differences are replaced with ios values.")
    val correctedAndroidStrings = AndroidTranslationGenerator.generateSynchronizedAndroidXML(
        document = androidXMLTemplate,
        report = report,
        blockReplacementOnPlaceholderCountMismatch = blockReplacementOnPlaceholderCountMismatch
    )

    //save the xml document to the file system
    val outputFile = outputFolder.resolve(localeAndroidFile.fileName)
    outputFile.deleteIfExists()
    val fileWriter = Files.newBufferedWriter(outputFile, Charset.forName("UTF-8"), StandardOpenOption.CREATE_NEW)
    fileWriter.use {
        val outputFormat = OutputFormat().apply {
            isExpandEmptyElements = true
        }
        val xmlWriter = XMLWriter(it, outputFormat)
        xmlWriter.write(correctedAndroidStrings)
        xmlWriter.close()
    }

    return outputFile
}

//helper function to write to csv
private fun writeToCsv(outputFile: Path, lambda: CSVWriter.() -> Unit) {
    outputFile.deleteIfExists()
    val fileWriter = Files.newBufferedWriter(outputFile, Charset.forName("UTF-8"), StandardOpenOption.CREATE_NEW)

    val csvWriter = CSVWriter(fileWriter, '\t', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)
    csvWriter.use(lambda)
}