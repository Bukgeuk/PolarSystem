package dev.bukgeuk.polarsystem.util

import java.io.File
import java.nio.charset.Charset
import java.util.*

class Buffer {
    companion object {
        lateinit var dataFolder: String
    }

    fun push(uuid: UUID, text: String) {
        val file = File("$dataFolder/buffer/${uuid}.buf")
        if (file.exists()) {
            var str = file.readText()
            str += "\n"
            str += text
            file.writeText(str)
        } else {
            file.writeText(text)
        }
    }

    fun get(uuid: UUID): String? {
        val file = File("$dataFolder/buffer/${uuid}.buf")
        if (!file.exists()) return null
        val text = file.readText()
        file.writeText("")
        return text
    }
}