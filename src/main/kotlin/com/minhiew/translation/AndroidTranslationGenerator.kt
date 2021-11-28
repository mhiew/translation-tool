package com.minhiew.translation

import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File
import java.io.StringReader

object AndroidTranslationGenerator {
    //region generating an android file with synchronized translations from ios
    fun generateFixedAndroidXML(originalStringsXmlFile: File, report: LocalizationReport, blockReplacementOnPlaceholderCountMismatch: Boolean): Document {
        val reader = originalStringsXmlFile.bufferedReader()
        val document = SAXReader().read(reader)
        return generateFixedAndroidXML(document = document, differences = report.differences, blockReplacementOnPlaceholderCountMismatch = blockReplacementOnPlaceholderCountMismatch)
    }

    fun generateFixedAndroidXML(document: Document, report: LocalizationReport, blockReplacementOnPlaceholderCountMismatch: Boolean): Document {
        return generateFixedAndroidXML(document = document, differences = report.differences, blockReplacementOnPlaceholderCountMismatch = blockReplacementOnPlaceholderCountMismatch)
    }

    fun generateFixedAndroidXML(xmlString: String, differences: List<StringComparison>, blockReplacementOnPlaceholderCountMismatch: Boolean): Document {
        val document = SAXReader().read(StringReader(xmlString))
        return generateFixedAndroidXML(document = document, differences = differences, blockReplacementOnPlaceholderCountMismatch = blockReplacementOnPlaceholderCountMismatch)
    }

    // Standardizes the input android xml file with the ios localizations
    // This creates an in memory copy of the original android strings.xml document
    // It will then find any keys that have mismatched text copy and replace it with the ios version
    fun generateFixedAndroidXML(document: Document, differences: List<StringComparison>, blockReplacementOnPlaceholderCountMismatch: Boolean): Document {
        //modify the document replacing the text copy with the ios value for all differences
        val rootElement = document.rootElement
        differences.forEach { comparison: StringComparison ->
            if (comparison.hasMismatchedPlaceholders && blockReplacementOnPlaceholderCountMismatch) {
                println("Block replacing ${comparison.key} due to placeholder mismatch:")
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
    //endregion

    //region Merging Android XML Files from other languages
    fun mergeAndroidTranslation(mainTemplateXML: String, otherAndroidStrings: Map<String, String>): Document {
        val document = SAXReader().read(StringReader(mainTemplateXML))
        return mergeAndroidTranslation(mainTemplate = document, otherLocale = otherAndroidStrings)
    }

    // Uses one android strings.xml file as the main template and fills in values from another android strings.xml
    // This allows us to use the main values/strings.xml file as a template and fill it in from another locale
    //
    // Returns the updated main template with:
    //  - untranslatable strings within the main template are removed
    //  - the value from the other locale when keys are present in both files
    //  - empty string if the key is missing from the other locale
    fun mergeAndroidTranslation(mainTemplate: Document, otherLocale: Map<String, String>): Document {
        val stringsElements = mainTemplate.rootElement.elements("string")
        stringsElements.forEach {
            //filter out any strings that are not translatable
            val translatable = it.attributeValue("translatable", "true")
            if (translatable.trim().lowercase() == "false") {
                it.detach()
            }

            val name = it.attributeValue("name")
            if (otherLocale.containsKey(name)) {
                //replace the template value with other locales value
                it.text = otherLocale[name]
            } else {
                //other locale doesn't contain this key so set the value to empty
                it.text = ""
            }
        }

        return mainTemplate
    }
    //endregion
}