package dev.bukgeuk.polarsystem.util

import net.md_5.bungee.api.ChatColor

class Color {
    companion object {
        val GRAY: ChatColor = ChatColor.of("#696969")
        val RED: ChatColor = ChatColor.of("#FF4040")
        val BLUE: ChatColor = ChatColor.of("#1E90FF")
        val STATUSGREEN: ChatColor = ChatColor.of("#00FA9A")
        val STATUSRED: ChatColor = ChatColor.of("#DC143C")
        val STATUSGRAY: ChatColor = ChatColor.of("#778899")
    }
}

class ColoredChat {
    private val pattern = Regex("""\$\{(#[a-zA-Z0-9]{6})}""")

    fun hexToColor(text: String): String {
        var str = text
        var res = pattern.find(str)
        while (res != null) {
            str = str.replace(res.value, "${ChatColor.of(res.groupValues[1])}")
            res = pattern.find(str)
        }

        return ChatColor.translateAlternateColorCodes('&', str)
    }
}