package com.minhiew.translation

import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File
import java.io.Reader

object AndroidTranslationGenerator {

    fun generateFixedAndroidXML(originalStringsXmlFile: File, report: LocalizationReport, blockPlaceholderMismatch: Boolean) =
        generateFixedAndroidXML(originalStringsXmlFile.bufferedReader(), report.differences, blockPlaceholderMismatch)

    // Standardizes the input android xml file with the ios localizations
    // This creates an in memory copy of the original android strings.xml document
    // It will then find any keys that have mismatched text copy and replace it with the ios version
    fun generateFixedAndroidXML(reader: Reader, differences: List<StringComparison>, blockPlaceholderMismatch: Boolean): Document {
        val document = SAXReader().read(reader)

        //modify the document replacing the text copy with the ios value for all differences
        val rootElement = document.rootElement
        differences.forEach { comparison: StringComparison ->
            if (comparison.hasMismatchedPlaceholders && blockPlaceholderMismatch) {
                println("Block replacing string due to placeholder mismatch:")
                println("${comparison.key} - placeholder count android ${comparison.androidPlaceholderCount} ios ${comparison.iosPlaceholderCount}\n")
                return@forEach
            }

            val stringElementXPATH = "string[@name='${comparison.key}']"
            val node = rootElement.selectSingleNode(stringElementXPATH)
            if (node != null) {
                node.text = AndroidValueSanitizer.sanitizeInput(comparison.iosValue)
            }
        }

        return document
    }
}