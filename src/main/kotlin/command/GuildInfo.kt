package dev.bukgeuk.polarsystem.command

import dev.bukgeuk.polarsystem.util.Color
import dev.bukgeuk.polarsystem.util.ColoredChat
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.NumberFormatException

class GuildInfo {
    fun onCommand(sender: Player, args: Array<out String>) {
        val id = if (args.isEmpty()) {
            if (GuildBase.PlayerToGuildId[sender.uniqueId] == null) {
                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드에 가입되어 있지 않습니다")
                return
            }
            GuildBase.PlayerToGuildId[sender.uniqueId]!!
        } else {
            try {
                args[0].toInt()
            } catch (e: NumberFormatException) {
                sender.sendMessage("사용법: /guild info <guildID>")
                return
            }
        }

        val guild = GuildBase.Guilds[id]!!
        val members: MutableList<String?> = mutableListOf()
        for (uuid in guild.members) {
            val p = Bukkit.getPlayer(uuid)
            if (p == null) {
                members.add(Bukkit.getOfflinePlayer(uuid).name)
            } else members.add(p.name)
        }

        val lp = Bukkit.getPlayer(guild.leader)
        sender.sendMessage("")
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}======== 길드: ${ColoredChat().hexToColor(guild.name)} ${ChatColor.GOLD}${ChatColor.BOLD}==========")
        sender.sendMessage("")
        sender.sendMessage("  - ID: ${guild.id}")
        sender.sendMessage("  - 길드장: ${(lp ?: Bukkit.getOfflinePlayer(guild.leader)).name}")
        sender.sendMessage("  - 멤버: ${members.joinToString(", ")}")
        sender.sendMessage("  - 설명: ${ColoredChat().hexToColor(guild.description)}")
    }
}

class GuildDescription {
    fun onCommand(sender: Player, args: Array<out String>) {
        val id = GuildBase.PlayerToGuildId[sender.uniqueId]
        if (id != null) {
            if (GuildBase.Guilds[id]!!.leader == sender.uniqueId) {
                GuildBase.Guilds[id]!!.description = args.joinToString(" ")
                GuildBase.saveGuilds()
                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드 설명을 변경했습니다")
                return
            }
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드장만 길드 설명을 변경할 수 있습니다")
            return
        }
        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드에 가입되어 있지 않습니다")
    }
}

class GuildRename {
    fun onCommand(sender: Player, args: Array<out String>) {
        val id = GuildBase.PlayerToGuildId[sender.uniqueId]
        if (id != null) {
            if (GuildBase.Guilds[id]!!.leader == sender.uniqueId) {
                GuildBase.Guilds[id]!!.name = args.joinToString(" ")
                GuildBase.saveGuilds()
                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드 이름을 변경했습니다")
                return
            }
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드장만 길드 이름을 변경할 수 있습니다")
            return
        }
        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드에 가입되어 있지 않습니다")
    }
}