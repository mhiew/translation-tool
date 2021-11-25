package com.minhiew.translation

import java.io.BufferedReader
import java.io.File
import java.io.StringReader

object AndroidFileParser {
    fun parse(file: File): Map<String, String> = parse(file.bufferedReader())

    fun parse(fileContents: String): Map<String, String> = parse(BufferedReader(StringReader(fileContents)))

    fun parse(fileContents: BufferedReader): Map<String, String> {
        val result = mutableMapOf<String, String>()
        fileContents.use {
            it.forEachLine { line ->
                if (line.isNotBlank()) {
                    "<string.+?name=\"(.+?)\".*?>(.+?)</.+?>".toRegex().findAll(line).forEach {
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