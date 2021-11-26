package com.minhiew.translation

import com.opencsv.CSVWriter
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardOpenOption

lateinit var outputFolder: File

fun main(args: Array<String>) {
    outputFolder = File(args[2])
    if (!outputFolder.exists()) {
        outputFolder.mkdir()
    }

    val androidFile = File(args[0])
    val androidStrings: Map<String, String> = AndroidFileParser.parse(androidFile)
    println("Total Android strings: ${androidStrings.size}")


    val iosFile = File(args[1])
    val iosStrings: Map<String, String> = iOSFileParser.parse(iosFile)
    println("Total iOS strings: ${iosStrings.size}")

    val report: LocalizationReport = Analyzer.compare(androidStrings = androidStrings, iosStrings = iosStrings)

    writeUniqueAndroidStrings(report)
    writeUniqueIOSStrings(report)
    writeExactMatches(report)
    writeDifferences(report)
}

private fun writeUniqueAndroidStrings(report: LocalizationReport) {
    val uniqueStrings = report.uniqueAndroidStrings
    println("Unique Android Strings: ${uniqueStrings.size}")

    writeToCsv(fileName = "unique-android-strings.csv") {
        writeNext(arrayOf("Android Key", "Android Value"))
        uniqueStrings.forEach { (key, value) ->
            writeNext(arrayOf(key, value))
        }
    }
}

private fun writeUniqueIOSStrings(report: LocalizationReport) {
    val uniqueStrings = report.uniqueIosStrings
    println("Unique iOS Strings: ${uniqueStrings.size}")
    writeToCsv(fileName = "unique-ios-strings.csv") {
        writeNext(arrayOf("iOS Key", "iOS Value"))
        uniqueStrings.forEach { (key, value) ->
            writeNext(arrayOf(key, value))
        }
    }
}

private fun writeExactMatches(report: LocalizationReport) {
    val exactMatches: List<StringComparison> = report.stringComparisons.values
        .filter { it.isExactMatch }
        .sortedBy { it.key }

    println("Exact matches: ${exactMatches.size}")
    writeToCsv(fileName = "exact-matches.csv") {
        writeNext(arrayOf("Key", "Android Value", "iOS Value"))
        exactMatches.forEach {
            writeNext(arrayOf(it.key, it.androidValue, it.iosValue))
        }
    }
}

private fun writeDifferences(report: LocalizationReport) {
    val differences: List<StringComparison> = report.stringComparisons.values
        .filterNot { it.isExactMatch }
        .sortedWith(compareBy({ !it.isCaseInsensitiveMatch }, { it.key })) //exact matches first then keys alphabetical ascending

    val caseInsensitiveMatches: List<StringComparison> = differences.filter { it.isCaseInsensitiveMatch }
    println("Total Differences: ${differences.size} Case Sensitive: ${caseInsensitiveMatches.size}, Other: ${differences.size - caseInsensitiveMatches.size}")

    writeToCsv(fileName = "differences.csv") {
        writeNext(arrayOf("Key", "Android Value", "iOS Value", "Levenshtein Distance", "Similarity 0 to 1.0f (1.0 being an exact match)", "Is Case Insensitive Match"))
        differences.forEach {
            writeNext(arrayOf(it.key, it.androidValue, it.iosValue, it.levenshteinDistance.toString(), it.levenshteinPercentage.toString(), it.isCaseInsensitiveMatch.toString()))
        }
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