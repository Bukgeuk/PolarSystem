package dev.bukgeuk.polarsystem.command

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.bukgeuk.polarsystem.util.Buffer
import dev.bukgeuk.polarsystem.util.Color
import dev.bukgeuk.polarsystem.util.ColoredChat
import dev.bukgeuk.polarsystem.util.RandomString
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.util.*
import kotlin.NumberFormatException
import kotlin.concurrent.timer

class LeaveExpirationTimer {
    companion object {
        var expirationTime: Long = 60
        var secondTable: MutableMap<String, Long> = mutableMapOf()
        @JvmField var Timer = timer(name = "LeaveExpirationTimer", period = 1000) {
            for (item in secondTable.keys) {
                if (secondTable[item] != 0.toLong()) {
                    secondTable[item] = secondTable[item]!! - 1
                } else {
                    LeaveExpirationTimer().removeTimer(item) // remove player in table
                    Bukkit.getPlayer(GuildLeave.LeaveID[item]!!)?.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드 탈퇴가 취소되었습니다")

                    GuildLeave.LeaveID.remove(item)
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

class KickExpirationTimer {
    companion object {
        var expirationTime: Long = 60
        var secondTable: MutableMap<String, Long> = mutableMapOf()
        @JvmField var Timer = timer(name = "KickExpirationTimer", period = 1000) {
            for (item in secondTable.keys) {
                if (secondTable[item] != 0.toLong()) {
                    secondTable[item] = secondTable[item]!! - 1
                } else {
                    KickExpirationTimer().removeTimer(item) // remove player in table
                    Bukkit.getPlayer(GuildKick.KickID[item]!!)?.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드 탈퇴가 취소되었습니다")

                    GuildKick.KickID.remove(item)
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

class GuildJoin(private val dataFolder: String) {
    fun onCommand(sender: Player, args: Array<out String>) {
        if (GuildBase.PlayerToGuildId[sender.uniqueId] == null) {
            if (args.isNotEmpty()) {
                val id: Int
                try {
                    id = args[0].toInt()
                } catch (e: NumberFormatException) {
                    sender.sendMessage("사용법: /guild join <guildID>")
                    return
                }

                if (GuildBase.Guilds[id] == null) {
                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드를 찾을 수 없습니다")
                    return
                }

                val data: MutableMap<Int, MutableList<UUID>>
                val mapper = jacksonObjectMapper()

                val file = File("$dataFolder/guild/joinRequests.json")

                data = if (file.exists()) {
                    mapper.readValue(file)
                } else {
                    mutableMapOf()
                }

                if (data[id] == null) {
                    data[id] = mutableListOf(sender.uniqueId)
                } else {
                    if (data[id]!!.contains(sender.uniqueId)) {
                        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}이미 해당 길드에 가입 신청을 보냈습니다")
                        return
                    }
                    data[id]!!.add(sender.uniqueId)
                }

                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드에 가입 신청을 보냈습니다")


                val p = Bukkit.getPlayer(GuildBase.Guilds[id]!!.leader)
                if (p != null) {
                    p.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${sender.name}${ChatColor.RESET}님이 길드 가입 신청을 보냈습니다")
                    val accept = TextComponent("${ChatColor.GREEN}${ChatColor.BOLD}[수락]")
                    accept.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild request accept ${sender.name}")
                    val deny = TextComponent("${ChatColor.RED}${ChatColor.BOLD}[거부]")
                    deny.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild request deny ${sender.name}")
                    val msg = TextComponent("        ")
                    msg.addExtra(accept)
                    msg.addExtra("      ")
                    msg.addExtra(deny)

                    p.spigot().sendMessage(msg)
                }

                mapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
                return
            }
            sender.sendMessage("사용법: /guild join <guildID>")
            return
        }
        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}이미 길드에 가입되어 있습니다")
    }
}

class _GuildLeaveAccept: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty() && sender is Player) {
            if (GuildLeave.LeaveID[args[0]] == null) return true

            val id = GuildBase.PlayerToGuildId[sender.uniqueId]

            LeaveExpirationTimer().removeTimer(args[0])
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드를 탈퇴했습니다")

            GuildBase.PlayerToGuildId.remove(sender.uniqueId)
            GuildBase.Guilds[id]!!.members.remove(sender.uniqueId)
            GuildLeave.LeaveID.remove(args[0])

            GuildBase.savePlayerToGuildId()
            GuildBase.saveGuilds()

            for (uuid in GuildBase.Guilds[id]!!.members) {
                val p = Bukkit.getPlayer(uuid)
                if (p != null) {
                    p.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.DARK_AQUA}${sender.name}${ChatColor.RESET}님이 길드를 탈퇴했습니다")
                } else {
                    Buffer().push(uuid, "${Color.GRAY}[PolarSystem] ${ChatColor.DARK_AQUA}${sender.name}${ChatColor.RESET}님이 길드를 탈퇴했습니다")
                }
            }
        }
        return true
    }
}

class _GuildLeaveDeny: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty() && sender is Player) {
            if (GuildLeave.LeaveID[args[0]] == null) return true

            LeaveExpirationTimer().removeTimer(args[0])
            GuildLeave.LeaveID.remove(args[0])

            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드 탈퇴를 취소했습니다")
        }
        return true
    }
}

class GuildLeave {
    companion object {
        @JvmField var LeaveID: MutableMap<String, UUID> = mutableMapOf()
    }

    fun onCommand(sender: Player, args: Array<out String>) {
        val id = GuildBase.PlayerToGuildId[sender.uniqueId]
        if (id != null) {
            if (GuildBase.Guilds[id]!!.leader == sender.uniqueId) {
                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드장은 길드를 탈퇴할 수 없습니다")
                return
            }
            if (LeaveID.isNotEmpty()) {
                for (item in LeaveID.keys) {
                    if (LeaveID[item]!! == sender.uniqueId) {
                        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}탈퇴 요청이 대기중입니다")
                        return
                    }
                }
            }
            val rid = RandomString().getRandomString(6)
            LeaveID[rid] = sender.uniqueId

            LeaveExpirationTimer().startTimer(rid)

            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}더이상 해당 길드의 인벤토리와 홈에 접근할 수 없습니다")
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RED}${ChatColor.BOLD}정말 탈퇴하시겠습니까?")
            sender.sendMessage("")
            val accept = TextComponent("${ChatColor.RED}${ChatColor.BOLD}[탈퇴]")
            accept.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/PolarSystemGuildLeaveAccept $rid")
            val deny = TextComponent("${ChatColor.GRAY}${ChatColor.BOLD}[취소]")
            deny.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/PolarSystemGuildLeaveDeny $rid")
            val msg = TextComponent("        ")
            msg.addExtra(accept)
            msg.addExtra("      ")
            msg.addExtra(deny)

            sender.spigot().sendMessage(msg)

            return
        }
        sender.sendMessage("${Color.GRAY}[PolarSystem] ${net.md_5.bungee.api.ChatColor.RESET}길드에 가입되어 있지 않습니다")
    }
}

class GuildKick {
    companion object {
        @JvmField var KickID: MutableMap<String, UUID> = mutableMapOf()
    }

    fun onCommand(sender: Player, args: Array<out String>) {
        val id = GuildBase.PlayerToGuildId[sender.uniqueId]
        if (id != null) {
            if (GuildBase.Guilds[id]!!.leader == sender.uniqueId) {
                if (args.isEmpty()) {
                    sender.sendMessage("사용법: /guild kick <player>")
                    return
                }
                var uuid = Bukkit.getPlayer(args[0])?.uniqueId
                if (uuid == null) {
                    for (ofp in Bukkit.getOfflinePlayers()) {
                        if (ofp.name == args[0]) {
                            uuid = ofp.uniqueId
                            break
                        }
                    }
                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}유저를 찾을 수 없습니다")
                    return
                }
                
                if (!(GuildBase.Guilds[id]!!.members.contains(uuid))) {
                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}유저를 찾을 수 없습니다")
                    return
                }

                if (uuid == sender.uniqueId) {
                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}자기 자신을 추방할 수 없습니다")
                    return
                }
                
                val rid = RandomString().getRandomString(6)
                KickID[rid] = uuid

                KickExpirationTimer().startTimer(rid)

                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RED}${ChatColor.BOLD}정말 길드에서 추방하시겠습니까?")
                sender.sendMessage("")
                val accept = TextComponent("${ChatColor.RED}${ChatColor.BOLD}[추방]")
                accept.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/PolarSystemGuildKickAccept $rid")
                val deny = TextComponent("${ChatColor.GRAY}${ChatColor.BOLD}[취소]")
                deny.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/PolarSystemGuildKickDeny $rid")
                val msg = TextComponent("        ")
                msg.addExtra(accept)
                msg.addExtra("      ")
                msg.addExtra(deny)

                sender.spigot().sendMessage(msg)

                return
            }
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드장만 멤버를 추방할 수 있습니다")
            return
        }
        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드에 가입되어 있지 않습니다")
    }
}

class _GuildKickAccept: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty() && sender is Player) {
            if (GuildKick.KickID[args[0]] == null) return true

            val id = GuildBase.PlayerToGuildId[sender.uniqueId]
            val uuid = GuildKick.KickID[args[0]]!!
            val p = Bukkit.getPlayer(uuid)
            val off = Bukkit.getOfflinePlayer(uuid)

            KickExpirationTimer().removeTimer(args[0])
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.DARK_AQUA}${(p ?: off).name}${ChatColor.RESET}님을 ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드에서 추방했습니다")

            if (p != null) {
                p.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드에서 추방되었습니다")
            } else {
                Buffer().push(uuid, "${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드에서 추방되었습니다")
            }

            GuildBase.PlayerToGuildId.remove(uuid)
            GuildBase.Guilds[id]!!.members.remove(uuid)
            GuildKick.KickID.remove(args[0])

            GuildBase.savePlayerToGuildId()
            GuildBase.saveGuilds()

            for (mu in GuildBase.Guilds[id]!!.members) {
                if (mu == sender.uniqueId) continue
                val m = Bukkit.getPlayer(mu)
                if (m != null) {
                    m.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.DARK_AQUA}${(p ?: off).name}${ChatColor.RESET}님이 길드에서 추방되었습니다")
                } else {
                    Buffer().push(mu, "${Color.GRAY}[PolarSystem] ${ChatColor.DARK_AQUA}${(p ?: off).name}${ChatColor.RESET}님이 길드에서 추방되었습니다")
                }
            }
        }
        return true
    }
}

class _GuildKickDeny: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isNotEmpty() && sender is Player) {
            if (GuildKick.KickID[args[0]] == null) return true

            KickExpirationTimer().removeTimer(args[0])
            GuildKick.KickID.remove(args[0])

            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}멤버 추방을 취소했습니다")
        }
        return true
    }
}

class GuildRequest(private val dataFolder: String) {
    fun onCommand(sender: Player, args: Array<out String>) {
        val id = GuildBase.PlayerToGuildId[sender.uniqueId]
        if (id != null) {
            if (GuildBase.Guilds[id]!!.leader == sender.uniqueId) {
                if (args.isNotEmpty()) {
                    when (args[0]) {
                        "list" -> {
                            val data: MutableMap<Int, MutableList<UUID>>

                            val file = File("$dataFolder/guild/joinRequests.json")

                            if (file.exists()) {
                                data = jacksonObjectMapper().readValue(file)
                            } else {
                                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}대기중인 길드 가입 신청이 없습니다")
                                return
                            }
                            if (data[id] == null) {
                                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}대기중인 길드 가입 신청이 없습니다")
                                return
                            }

                            sender.sendMessage("${ChatColor.GOLD}${ChatColor.BOLD}[ 대기중인 길드 가입 신청 ]")
                            sender.sendMessage("")
                            for (uuid in data[id]!!) {
                                val name = (Bukkit.getPlayer(uuid) ?: Bukkit.getOfflinePlayer(uuid)).name
                                val msg = TextComponent("  - $name")
                                msg.addExtra("    ")
                                val accept = TextComponent("${ChatColor.GREEN}${ChatColor.BOLD}[수락]")
                                accept.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild request accept $name")
                                val deny = TextComponent("${ChatColor.RED}${ChatColor.BOLD}[거절]")
                                deny.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/guild request deny $name")
                                msg.addExtra(accept)
                                msg.addExtra("  ")
                                msg.addExtra(deny)

                                sender.spigot().sendMessage(msg)
                            }

                            return
                        }
                        "accept" -> {
                            if (args.size >= 2) {
                                val data: MutableMap<Int, MutableList<UUID>>
                                val mapper = jacksonObjectMapper()

                                val file = File("$dataFolder/guild/joinRequests.json")

                                if (file.exists()) {
                                    data = mapper.readValue(file)
                                } else {
                                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}해당 유저로부터의 길드 가입 신청이 없습니다")
                                    return
                                }
                                if (data[id] == null) {
                                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}해당 유저로부터의 길드 가입 신청이 없습니다")
                                    return
                                }
                                
                                val p = Bukkit.getPlayer(args[1])

                                if (p != null) {
                                    if (GuildBase.PlayerToGuildId[p.uniqueId] != null) {
                                        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}해당 유저는 이미 길드에 가입되어 있습니다")
                                        return
                                    }
                                    for (uuid in GuildBase.Guilds[id]!!.members) {
                                        val m = Bukkit.getPlayer(uuid)
                                        if (m != null) {
                                            m.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.DARK_AQUA}${p.name}${ChatColor.RESET}님이 ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드에 가입했습니다!")
                                        } else {
                                            Buffer().push(uuid, "${Color.GRAY}[PolarSystem] ${ChatColor.DARK_AQUA}${p.name}${ChatColor.RESET}님이 ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드에 가입했습니다!")
                                        }
                                    }
                                    GuildBase.Guilds[id]!!.members.add(p.uniqueId)
                                    GuildBase.PlayerToGuildId[p.uniqueId] = id
                                    GuildBase.saveGuilds()
                                    GuildBase.savePlayerToGuildId()
                                    p.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드에 가입되었습니다!")
                                    
                                    for (g in data.keys) {
                                        if (data[g]!!.contains(p.uniqueId)) data[g]!!.remove(p.uniqueId)
                                    }
                                } else {
                                    for (off in Bukkit.getOfflinePlayers()) {
                                        if (off.name == args[1]) {
                                            if (GuildBase.PlayerToGuildId[off.uniqueId] != null) {
                                                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}해당 유저는 이미 길드에 가입되어 있습니다")
                                                return
                                            }
                                            for (uuid in GuildBase.Guilds[id]!!.members) {
                                                val m = Bukkit.getPlayer(uuid)
                                                if (m != null) {
                                                    m.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.DARK_AQUA}${off.name}${ChatColor.RESET}님이 ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드에 가입했습니다!")
                                                } else {
                                                    Buffer().push(uuid, "${Color.GRAY}[PolarSystem] ${ChatColor.DARK_AQUA}${off.name}${ChatColor.RESET}님이 ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드에 가입했습니다!")
                                                }
                                            }
                                            GuildBase.Guilds[id]!!.members.add(off.uniqueId)
                                            GuildBase.PlayerToGuildId[off.uniqueId] = id
                                            GuildBase.saveGuilds()
                                            GuildBase.savePlayerToGuildId()
                                            Buffer().push(off.uniqueId, "${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드에 가입되었습니다!")

                                            for (g in data.keys) {
                                                if (data[g]!!.contains(off.uniqueId)) data[g]!!.remove(off.uniqueId)
                                            }
                                            break
                                        }
                                    }
                                }
                                mapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
                                return
                            }
                            sender.sendMessage("사용법: /guild request accept <player>")
                        }
                        "deny" -> {
                            if (args.size >= 2) {
                                val data: MutableMap<Int, MutableList<UUID>>
                                val mapper = jacksonObjectMapper()

                                val file = File("$dataFolder/guild/joinRequests.json")

                                if (file.exists()) {
                                    data = mapper.readValue(file)
                                } else {
                                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}해당 유저로부터의 길드 가입 신청이 없습니다")
                                    return
                                }
                                if (data[id] == null) {
                                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}해당 유저로부터의 길드 가입 신청이 없습니다")
                                    return
                                }

                                val p = Bukkit.getPlayer(args[1])

                                if (p != null) {
                                    if (!data[id]!!.contains(p.uniqueId)) {
                                        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}해당 유저로부터의 길드 가입 신청이 없습니다")
                                        return
                                    }
                                    data[id]!!.remove(p.uniqueId)
                                    sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드 가입 신청을 거절했습니다")
                                    p.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드 가입이 ${ChatColor.RED}${ChatColor.BOLD}거절${ChatColor.RESET}되었습니다")
                                } else {
                                    for (off in Bukkit.getOfflinePlayers()) {
                                        if (off.name == args[1]) {
                                            if (!(data[id]!!.contains(off.uniqueId))) {
                                                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}해당 유저로부터의 길드 가입 신청이 없습니다")
                                                return
                                            }
                                            data[id]!!.remove(off.uniqueId)
                                            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드 가입 신청을 거절했습니다")
                                            Buffer().push(off.uniqueId, "${Color.GRAY}[PolarSystem] ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}${ChatColor.RESET} 길드 가입이 ${ChatColor.RED}${ChatColor.BOLD}거절${ChatColor.RESET}되었습니다")
                                            break
                                        }
                                    }
                                }
                                mapper.writerWithDefaultPrettyPrinter().writeValue(file, data)
                                return
                            }
                            sender.sendMessage("사용법: /guild request deny <player>")
                        }
                        else -> sender.sendMessage("사용법: /guild request [accept/deny]")
                    }
                    return
                }
                sender.sendMessage("사용법: /guild request [accept/deny]")
            }
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드장만 길드 신청을 관리할 수 있습니다")
            return
        }
        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드에 가입되어 있지 않습니다")
    }
}