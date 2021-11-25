package com.minhiew.translation

import java.io.File

//returns system agnostic filepath
fun String.sanitizeFilePath(): String = if (contains("/")) {
    replace("/", File.separator)
} else if (contains("\\")) {
    replace("\\", File.separator)
} else {
    this
}
