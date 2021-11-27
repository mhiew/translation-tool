package com.minhiew.translation

import com.opencsv.CSVWriter
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardOpenOption

private const val UNIQUE_ANDROID_STRINGS_FILE = "unique-android-strings.csv"
private const val UNIQUE_IOS_STRINGS_FILE = "unique-ios-strings.csv"
private const val EXACT_MATCH_FILE = "exact-matches.csv"
private const val DIFFERENCES_FILE = "differences.csv"

lateinit var outputFolder: File

fun main(args: Array<String>) {
    outputFolder = File(args[2])
    if (!outputFolder.exists()) {
        outputFolder.mkdir()
    }

    val androidFile = File(args[0])
    val iosFile = File(args[1])

    compareStrings(androidFile = androidFile, iosFile = iosFile)
}

private fun compareStrings(androidFile: File, iosFile: File) {
    val androidStrings: Map<String, String> = AndroidFileParser.parse(androidFile)
    println("Total Android strings: ${androidStrings.size}")

    val iosStrings: Map<String, String> = IOSFileParser.parse(iosFile)
    println("Total iOS strings: ${iosStrings.size}")

    val report: LocalizationReport = Analyzer.compare(androidStrings = androidStrings, iosStrings = iosStrings)

    writeUniqueAndroidStrings(report)
    writeUniqueIOSStrings(report)
    writeExactMatches(report)
    writeDifferences(report)
    writeFixedAndroidXmlFile(androidStringsFile = androidFile, report = report)
}

private fun writeUniqueAndroidStrings(report: LocalizationReport) {
    val uniqueStrings = report.uniqueAndroidStrings
    println("Unique Android Strings: ${uniqueStrings.size}")

    writeToCsv(fileName = UNIQUE_ANDROID_STRINGS_FILE) {
        writeNext(arrayOf("Android Key", "Android Value"))
        uniqueStrings.forEach { (key, value) ->
            writeNext(arrayOf(key, value))
        }
    }
}

private fun writeUniqueIOSStrings(report: LocalizationReport) {
    val uniqueStrings = report.uniqueIosStrings
    println("Unique iOS Strings: ${uniqueStrings.size}")
    writeToCsv(fileName = UNIQUE_IOS_STRINGS_FILE) {
        writeNext(arrayOf("iOS Key", "iOS Value"))
        uniqueStrings.forEach { (key, value) ->
            writeNext(arrayOf(key, value))
        }
    }
}

private fun writeExactMatches(report: LocalizationReport) {
    val exactMatches: List<StringComparison> = report.exactMatches.sortedBy { it.key }

    println("Exact matches: ${exactMatches.size}")
    writeToCsv(fileName = EXACT_MATCH_FILE) {
        writeNext(arrayOf("Key", "Android Value", "iOS Value"))
        exactMatches.forEach {
            writeNext(arrayOf(it.key, it.androidValue, it.iosValue))
        }
    }
}

private fun writeDifferences(report: LocalizationReport) {
    val differences: List<StringComparison> = report.differences
        .sortedWith(compareBy({ !it.isCaseInsensitiveMatch }, { it.key })) //case-insensitive matches first then keys alphabetical ascending

    val caseInsensitiveMatches: List<StringComparison> = differences.filter { it.isCaseInsensitiveMatch }
    println("Total Differences: ${differences.size} Case Sensitive: ${caseInsensitiveMatches.size}, Other: ${differences.size - caseInsensitiveMatches.size}")

    writeToCsv(fileName = DIFFERENCES_FILE) {
        writeNext(arrayOf("Key", "Android Value", "iOS Value", "Is Case Insensitive Match"))
        differences.forEach {
            writeNext(arrayOf(it.key, it.androidValue, it.iosValue, it.isCaseInsensitiveMatch.toString()))
        }
    }
}

private fun writeFixedAndroidXmlFile(fileOutputFolder: File = outputFolder, androidStringsFile: File, report: LocalizationReport) {
    if (report.differences.isEmpty()) {
        println("Platform localizations match for shared keys!")
        return
    }

    println("Generating corrected android strings file with mismatched text replaced with ios values.")
    val correctedAndroidStrings = AndroidTranslationGenerator.generateFixedAndroidXML(androidStringsFile, report)

    val outputFile = File(fileOutputFolder, androidStringsFile.name)
    if (outputFile.exists()) outputFile.delete()
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
private fun writeToCsv(fileOutputFolder: File = outputFolder, fileName: String, lambda: CSVWriter.() -> Unit) {
    val file = File(fileOutputFolder, fileName)
    if (file.exists()) file.delete()
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