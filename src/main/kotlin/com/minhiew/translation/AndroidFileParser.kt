package com.minhiew.translation

import org.dom4j.io.SAXReader
import java.io.BufferedReader
import java.io.File
import java.io.StringReader

object AndroidFileParser {
    fun parse(file: File): Map<String, String> = parse(file.bufferedReader())

    fun parse(fileContents: String): Map<String, String> = parse(BufferedReader(StringReader(fileContents)))

    fun parse(fileContents: BufferedReader): Map<String, String> {
        val document = SAXReader().read(fileContents)
        val stringElements = document.rootElement.elements("string")

        return stringElements
            .filterNot { it.text.isNullOrBlank() }
            .associate { element ->
                element.attributeValue("name") to element.text.trim()
            }
    }
}