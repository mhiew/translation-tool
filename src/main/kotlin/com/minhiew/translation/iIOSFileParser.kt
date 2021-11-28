package com.minhiew.translation

import java.io.BufferedReader
import java.io.File

object IOSFileParser {
    fun parse(file: File): Map<String, String> = parse(file.bufferedReader())

    private fun parse(fileContents: BufferedReader): Map<String, String> {
        val result = mutableMapOf<String, String>()
        fileContents.use { reader ->
            reader.forEachLine { line ->
                if (line.isNotBlank()) {
                    "\"(.+?)\" = \"(.+?)\";".toRegex().findAll(line).forEach {
                        val key = it.groupValues[1]
                        val value = it.groupValues[2]
                        result[key] = value
                    }
                }
            }
        }
        return result
    }
}