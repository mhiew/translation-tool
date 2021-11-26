package com.minhiew.translation

import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File

object AndroidFileParser {
    fun getDocument(file: File): Document {
        val bufferedReader = file.bufferedReader()
        return SAXReader().read(bufferedReader)
    }

    fun parse(file: File): Map<String, String> = parse(document = getDocument(file))

    fun parse(document: Document): Map<String, String> {
        val stringElements = document.rootElement.elements("string")

        return stringElements
            .filterNot { it.text.isNullOrBlank() }
            .associate { element ->
                element.attributeValue("name") to element.text.trim()
            }
    }
}