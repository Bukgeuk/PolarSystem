package dev.bukgeuk.polarsystem.command

import dev.bukgeuk.polarsystem.util.Color
import dev.bukgeuk.polarsystem.util.ColoredChat
import dev.bukgeuk.polarsystem.util.RandomString
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.lang.NumberFormatException
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.ceil

class RequestExpirationTimer {
    companion object {
        var expirationTime: Long = 60
        var secondTable: MutableMap<String, Long> = mutableMapOf()
        @JvmField var Timer = timer(name = "RequestExpirationTimer", period = 1000) {
            for (item in secondTable.keys) {
                if (secondTable[item] != 0.toLong()) {
                    secondTable[item] = secondTable[item]!! - 1
                } else {
                    RequestExpirationTimer().removeTimer(item) // remove player in table
                    Bukkit.getPlayer(GuildRemove.RemoveID[item]!!.second)?.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드 삭제가 취소되었습니다")

                    GuildRemove.RemoveID.remove(item)
                }
            }
        }
    }

    fun startTimer(id: String) {
        secondTable[id] = expirationTime
    }

    fun removeTimer(id: String) {
        secondTable.remove(id)
    }
}

class GuildList {
    fun onCommand(sender: Player, args: Array<out String>) {
        if (GuildBase.Guilds.isEmpty()) {
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}창설된 길드가 없습니다")
            return
        }

        val maxPage = ceil(GuildBase.Guilds.size.toDouble() / 5).toInt()
        val page = try {
            if (args.isEmpty()) 1 else args[0].toInt()
        } catch (e: NumberFormatException) {
            1
        }
        val list = GuildBase.Guilds.toList().subList((page - 1) * 5, if (page == maxPage) GuildBase.Guilds.size else page * 5)

        sender.sendMessage("")
        sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}========== 길드 목록 ${ChatColor.RESET}${ChatColor.GOLD}($page/$maxPage)${ChatColor.BOLD} ============")
        sender.sendMessage("")
        for (guild in list) {
            val msg = TextComponent("  - ")
            val item = TextComponent(ColoredChat().hexToColor(guild.second.name))
            item.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("자세히 보기"))
            item.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild info ${guild.second.id}")
            msg.addExtra(item)
            msg.addExtra("    ")
            if (guild.second.members.size >= 5) {
                val xjoin = TextComponent("${ChatColor.GRAY}${org.bukkit.ChatColor.BOLD}[가입 신청]")
                xjoin.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("길드가 가득 참"))
                msg.addExtra(xjoin)
            } else if (GuildBase.PlayerToGuildId[sender.uniqueId] != null) {
                val xjoin = TextComponent("${ChatColor.GRAY}${org.bukkit.ChatColor.BOLD}[가입 신청]")
                xjoin.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text("길드에 가입됨"))
                msg.addExtra(xjoin)
            } else {
                val join = TextComponent("${org.bukkit.ChatColor.GREEN}${org.bukkit.ChatColor.BOLD}[가입 신청]")
                join.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild join ${guild.second.id}")
                msg.addExtra(join)
            }
            sender.spigot().sendMessage(msg)
        }
    }
}

class GuildCreate {
    fun onCommand(sender: Player, args: Array<out String>) {
        if (GuildBase.PlayerToGuildId[sender.uniqueId] == null) {
            if (args.isNotEmpty()) {
                val guildData = GuildBase.Guilds

                val id: Int = (guildData.keys.maxOrNull() ?: 0) + 1
                val name = args.joinToString(" ")

                GuildBase.Guilds[id] = Guild(name, id, sender.uniqueId, mutableListOf(sender.uniqueId), "")
                GuildBase.PlayerToGuildId[sender.uniqueId] = id

                GuildBase.savePlayerToGuildId()
                GuildBase.saveGuilds()

                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ColoredChat().hexToColor(name)}${ChatColor.RESET} 길드를 창설했습니다")

                return
            }
            sender.sendMessage("사용법: /guild create <name>")
            return
        }
        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}이미 길드에 가입되어 있습니다")
    }
}

class GuildRemove {
    companion object {
        @JvmField var RemoveID: MutableMap<String, Pair<Int, UUID>> = mutableMapOf()
    }

    fun onCommand(sender: Player, args: Array<out String>) {
        val id = GuildBase.PlayerToGuildId[sender.uniqueId]
        if (id != null) {
            if (GuildBase.Guilds[id]!!.leader == sender.uniqueId) {
                if (GuildBase.Guilds[id]!!.members.size < 2) {
                    if (RemoveID.isNotEmpty()) {
                        for (item in RemoveID.keys) {
                            if (RemoveID[item]!!.second == sender.uniqueId) {
                                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}삭제 요청이 대기중입니다")
                                return
                            }
                        }
                    }
                    val rid = RandomString().getRandomString(6)
                    RemoveID[rid] = id to sender.uniqueId

                    RequestExpirationTimer().startTimer(rid)

                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드 인벤토리 내 모든 아이템과 길드 홈이 삭제됩니다")
                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RED}${ChatColor.BOLD}정말 삭제하시겠습니까?")
                    sender.sendMessage("")
                    val accept = TextComponent("${ChatColor.RED}${ChatColor.BOLD}[삭제]")
                    accept.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/PolarSystemGuildRemoveAccept $rid")
                    val deny = TextComponent("${ChatColor.GRAY}${ChatColor.BOLD}[취소]")
                    deny.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/PolarSystemGuildRemoveDeny $rid")
                    val msg = TextComponent("        ")
                    msg.addExtra(accept)
                    msg.addExtra("      ")
                    msg.addExtra(deny)

                    sender.spigot().sendMessage(msg)

                    return
                }
                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}다른 멤버가 없어야 길드를 삭제할 수 있습니다")
                return
            }
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드장만 길드를 삭제할 수 있습니다")
            return
        }
        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드에 가입되어 있지 않습니다")
    }
}

class _GuildRemoveAccept: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty() && sender is Player) {
            if (GuildRemove.RemoveID[args[0]] == null) return true

            val id = GuildRemove.RemoveID[args[0]]!!.first
            for (uuid in GuildBase.Guilds[id]!!.members) {
                GuildBase.PlayerToGuildId.remove(uuid)
            }

            RequestExpirationTimer().removeTimer(args[0])
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드를 삭제했습니다")

            GuildBase.Guilds.remove(id)
            GuildRemove.RemoveID.remove(args[0])

            GuildBase.savePlayerToGuildId()
            GuildBase.saveGuilds()
        }
        return true
    }
}

class _GuildRemoveDeny: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty() && sender is Player) {
            if (GuildRemove.RemoveID[args[0]] == null) return true

            RequestExpirationTimer().removeTimer(args[0])
            GuildRemove.RemoveID.remove(args[0])

            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드 삭제를 취소했습니다")
        }
        return true
    }
}