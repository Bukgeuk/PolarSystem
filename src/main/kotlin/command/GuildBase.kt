package dev.bukgeuk.polarsystem.command

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.bukgeuk.polarsystem.PlayerLevelChangeEvent
import dev.bukgeuk.polarsystem.util.Color
import net.md_5.bungee.api.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.util.*

data class Guild(
    var name: String,
    var id: Int,
    var leader: UUID,
    var members: MutableList<UUID>,
    var description: String
)

class GuildBase(private val requireLevel: Int, private val memberLimit: Int): CommandExecutor {
    companion object {
        @JvmField var PlayerToGuildId: MutableMap<UUID, Int> = mutableMapOf()
        @JvmField var Guilds: MutableMap<Int, Guild> = mutableMapOf()
        private lateinit var dataFolder: String

        fun setDataFolder(location: String) {
            dataFolder = location
        }

        fun readPlayerToGuildId(): MutableMap<UUID, Int> {
            val file = File("$dataFolder/guild/table.json")
            return if (file.exists()) jacksonObjectMapper().readValue(file) else mutableMapOf()
        }

        fun savePlayerToGuildId() {
            jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(File("$dataFolder/guild/table.json"), PlayerToGuildId)
        }

        fun readGuilds(): MutableMap<Int, Guild> {
            val file = File("$dataFolder/guild/guilds.json")
            return if (file.exists()) jacksonObjectMapper().readValue(file) else mutableMapOf()
        }

        fun saveGuilds() {
            jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(File("$dataFolder/guild/guilds.json"), Guilds)
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            if (args.isNotEmpty()) {
                if (PlayerLevelChangeEvent.getCanUseGuild(sender.uniqueId)) {
                    val _args: Array<out String> = if (args.size < 2) arrayOf() else args.copyOfRange(1, args.size)
                    when {
                        args[0] == "list" || args[0] == "l" -> GuildList().onCommand(sender, _args)
                        args[0] == "create" -> GuildCreate().onCommand(sender, _args)
                        args[0] == "remove" -> GuildRemove().onCommand(sender, _args)
                        args[0] == "info" || args[0] == "in" -> GuildInfo(memberLimit).onCommand(sender, _args)
                        args[0] == "description" || args[0] == "des" || args[0] == "d" -> GuildDescription().onCommand(sender, _args)
                        args[0] == "join" -> GuildJoin(dataFolder, memberLimit).onCommand(sender, _args)
                        args[0] == "leave" -> GuildLeave().onCommand(sender, _args)
                        args[0] == "kick" -> GuildKick().onCommand(sender, _args)
                        args[0] == "rename" || args[0] == "rn" -> GuildRename().onCommand(sender, _args)
                        args[0] == "request" || args[0] == "rq" -> GuildRequest(dataFolder).onCommand(sender, _args)
                        args[0] == "members" || args[0] == "member" || args[0] == "m" -> GuildMember().onCommand(sender, _args)
                        args[0] == "inventory" || args[0] == "i" -> GuildInventory().onCommand(sender, _args)
                        else -> return false
                    }
                    return true
                }
                sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}${ChatColor.GOLD}${ChatColor.BOLD}${requireLevel}레벨${ChatColor.RESET}을 달성해야 길드 기능이 해금됩니다")
                return true
            }
            return false
        }
        sender.sendMessage("${ChatColor.RED}${ChatColor.BOLD}Error: ${ChatColor.RESET}This command isn't available on the console")
        return true
    }
}