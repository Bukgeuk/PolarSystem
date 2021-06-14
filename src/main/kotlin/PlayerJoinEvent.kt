package dev.bukgeuk.polarsystem

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.bukgeuk.polarsystem.command.GuildBase
import dev.bukgeuk.polarsystem.util.Buffer
import net.md_5.bungee.api.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.io.File
import java.util.*

class PlayerJoinEvent(private val dataFolder: String): Listener {
    @EventHandler
    fun onPlayerJoinEvent(e: PlayerJoinEvent) {
        val str: String? = Buffer().get(e.player.uniqueId)
        if (str != null) {
           e.player.sendMessage(str)
        }

        val id = GuildBase.PlayerToGuildId[e.player.uniqueId]
        if (id != null && GuildBase.Guilds.isNotEmpty()) {
            if (GuildBase.Guilds[id]!!.leader == e.player.uniqueId) {
                val file = File("$dataFolder/guild/joinRequests.json")

                if (file.exists()) {
                    val data: MutableMap<Int, MutableList<UUID>> = jacksonObjectMapper().readValue(file)
                    if (data[id]?.isNotEmpty() == true) {
                        e.player.sendMessage("${ChatColor.RED}${ChatColor.BOLD}${data[id]!!.size}개${ChatColor.RESET}의 길드 가입 신청이 대기중입니다")
                    }
                }
            }
        }
    }
}