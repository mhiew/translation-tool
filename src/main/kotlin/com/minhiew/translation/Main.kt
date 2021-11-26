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
    val androidStrings: Map<String, String> = AndroidFileParser.parse(androidFile)
    println("Android strings: ${androidStrings.size}")


    val iosFile = File(args[1])
    println("Parsing iOS file: ${iosFile.path} ...")
    val iosStrings: Map<String, String> = iOSFileParser.parse(iosFile)
    println("iOS strings: ${iosStrings.size}")
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