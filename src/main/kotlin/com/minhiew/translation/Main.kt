package com.minhiew.translation

import com.opencsv.CSVWriter
import java.io.File
import java.io.Writer
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardOpenOption

lateinit var exportFolder: File

fun main(args: Array<String>) {
    exportFolder = File(args[2])

    val androidFile = File(args[0])
    println("Parsing android file: ${androidFile.path} ...")
    val androidStringsPool: MutableMap<String, String> = AndroidFileParser.parse(androidFile).toMutableMap()
    println("Android strings: ${androidStringsPool.size}")


    val iosFile = File(args[1])
    println("Parsing iOS file: ${iosFile.path} ...")
    val iosStringsPool: MutableMap<String, String> = iOSFileParser.parse(iosFile).toMutableMap()
    println("iOS strings: ${iosStringsPool.size}")


    findIdenticalValues(androidStringsPool, iosStringsPool)
    findIdenticalValuesCaseInsensitive(androidStringsPool, iosStringsPool)
    findSimilarValues(androidStringsPool, iosStringsPool)
    writeRemainingValues(androidStringsPool, iosStringsPool)
}

fun String.toAndroidString(): String {
    return this.replace("$@", "\$s")
}

private fun findIdenticalValues(
    androidStringsPool: MutableMap<String, String>,
    iOsStringsPool: MutableMap<String, String>
) {
    println("Looking for identical strings ...")
    val identicalStrings = mutableMapOf<StringKey, String>()
    androidStringsPool.forEach { (androidKey, androidValue) ->
        var key: StringKey? = null
        val value: String = androidValue

        iOsStringsPool.forEach loop@{ iosKey, iOsValue ->
            if (androidValue == iOsValue.toAndroidString()) {
                key = StringKey(androidKey, iosKey)
                return@loop
            }
        }

        key?.let { identicalStrings[it] = value }
    }

    println("Identical strings: ${identicalStrings.entries.size}. Separating from the remaining pools.")
    identicalStrings.keys.forEach { key ->
        androidStringsPool.remove(key.androidKey)
        iOsStringsPool.remove(key.iosKey)
    }

    writeToCsv(fileName = "identical.csv") {
        val header = arrayOf("android key", "iOS key", "value")
        writeNext(header)
        identicalStrings.forEach { (key, value) ->
            writeNext(arrayOf(key.androidKey, key.iosKey, value))
        }
    }
}

private fun findIdenticalValuesCaseInsensitive(
    androidStringsPool: MutableMap<String, String>,
    iOsStringsPool: MutableMap<String, String>
) {
    println("Looking for identical strings case insensitive ...")
    val strings = mutableMapOf<StringKey, StringValue>()
    androidStringsPool.forEach { (androidKey, androidValue) ->

        iOsStringsPool.forEach loop@{ (iosKey, iOsValue) ->
            if (androidValue.lowercase() == iOsValue.toAndroidString().lowercase()) {
                val key = StringKey(androidKey, iosKey)
                val value = StringValue(androidValue, iOsValue)
                strings[key] = value
                return@loop
            }
        }
    }

    println("Identical case insensitive strings: ${strings.entries.size}. Separating from the remaining pools.")
    strings.keys.forEach { key ->
        androidStringsPool.remove(key.androidKey)
        iOsStringsPool.remove(key.iosKey)
    }

    writeToCsv(fileName = "identical-cis.csv") {
        val header = arrayOf("android key", "iOS key", "android value", "iOS value")
        writeNext(header)
        strings.forEach { (key, value) ->
            writeNext(arrayOf(key.androidKey, key.iosKey, value.androidValue, value.iosValue))
        }
    }
}

private fun findSimilarValues(
    androidStringsPool: MutableMap<String, String>,
    iOsStringsPool: MutableMap<String, String>
) {
    val maxPercent = 10
    println("Looking for similar strings with tolerance of $maxPercent% ...")
    val similarStrings = mutableMapOf<StringKey, StringValue>()
    androidStringsPool.forEach { (androidKey, androidValue) ->
        iOsStringsPool.forEach loop@{ (iosKey, iOsValue) ->
            val levenshtein = Levenshtein.calculate(androidValue, iOsValue.toAndroidString())
            val percent = levenshtein.toFloat() / androidValue.length
            if (percent < (maxPercent.toFloat() / 100.0)) {
                val key = StringKey(androidKey, iosKey)
//                println("Similarity percent for $androidValue and $iOsValue: $percent")
                similarStrings[key] = StringValue(androidValue, iOsValue)
                return@loop
            }
        }
    }

    println("Similar strings: ${similarStrings.entries.size}. Separating from the remaining pools.")
    similarStrings.keys.forEach { key ->
        androidStringsPool.remove(key.androidKey)
        iOsStringsPool.remove(key.iosKey)
    }

    writeToCsv(fileName = "similar.csv") {
        val header = arrayOf("android key", "android value", "iOS key", "iOS value")
        writeNext(header)
        similarStrings.forEach { (key, value) ->
            writeNext(arrayOf(key.androidKey, value.androidValue, key.iosKey, value.iosValue))
        }
    }
}

private fun writeRemainingValues(androidStringsPool: Map<String, String>, iOsStringsPool: Map<String, String>) {
    println("Writing remaining strings to csv ...")
    writeToCsv(fileName = "remaining.csv") {
        val header = arrayOf("android key", "android value", "iOS key", "iOS value")
        writeNext(header)

        androidStringsPool.forEach { (key, value) ->
            writeNext(arrayOf(key, value, "", ""))
        }

        iOsStringsPool.forEach { (key, value) ->
            writeNext(arrayOf("", "", key, value))
        }
    }
}

//helper function to write to csv
private fun writeToCsv(outputFolder: File = exportFolder, fileName: String, lambda: CSVWriter.() -> Unit) {
    val file = File(outputFolder, fileName)
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