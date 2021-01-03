package dev.bukgeuk.polarsystem.util

class RandomString {
    private val sequence = "ABCDEF1234567890"

    fun getRandomString(length: Int): String {
        var str = ""
        for (i in 0 until length) str += sequence.random()

        return str
    }
}