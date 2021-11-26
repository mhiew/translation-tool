package com.minhiew.translation

import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File
import java.io.Reader

object AndroidTranslationGenerator {

    fun generateFixedAndroidXML(originalStringsXmlFile: File, report: LocalizationReport) =
        generateFixedAndroidXML(originalStringsXmlFile.bufferedReader(), report.differences)

    // Standardizes the input android xml file with the ios localizations
    // This creates an in memory copy of the original android strings.xml document
    // It will then find any keys that have mismatched text copy and replace it with the ios version
    fun generateFixedAndroidXML(reader: Reader, differences: List<StringComparison>): Document {
        val document = SAXReader().read(reader)

        //modify the document replacing the text copy with the ios value for all differences
        val rootElement = document.rootElement
        differences.forEach { comparison: StringComparison ->
            val stringElementXPATH = "string[@name='${comparison.key}']"
            val node = rootElement.selectSingleNode(stringElementXPATH)
            if (node != null) {
                node.text = comparison.iosValue
            }
        }

        return document
    }
}