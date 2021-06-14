package dev.bukgeuk.polarsystem

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.bukgeuk.polarsystem.util.Color
import net.md_5.bungee.api.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLevelChangeEvent
import java.io.File
import java.util.*

class PlayerLevelChangeEvent(private val dataFolder: String, private val requireLevel: Int): Listener {
    companion object {
        @JvmField var canUseGuild: MutableMap<UUID, Boolean> = mutableMapOf()

        fun getCanUseGuild(uuid: UUID): Boolean {
            return canUseGuild[uuid] ?: false
        }
    }

    @EventHandler
    fun onPlayerLevelChangeEvent(e: PlayerLevelChangeEvent) {
        if (e.newLevel != requireLevel) return

        val data: MutableMap<UUID, Boolean>
        val mapper = jacksonObjectMapper()

        val file = File("$dataFolder/require/level.json")

        data = if (file.exists()) {
            mapper.readValue(file)
        } else {
            File(dataFolder, "require").mkdir()
            mutableMapOf()
        }

        if (data[e.player.uniqueId] != true) {
            data[e.player.uniqueId] = true
            canUseGuild[e.player.uniqueId] = true
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, data)

        e.player.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ChatColor.BOLD}길드 기능${ChatColor.RESET}이 해금되었습니다!")
        e.player.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.DARK_AQUA}/guild help ${ChatColor.RESET}명령어로 도움말을 확인하세요")
    }
}