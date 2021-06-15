package dev.bukgeuk.polarsystem

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.bukgeuk.polarsystem.command.*
import dev.bukgeuk.polarsystem.util.Buffer
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class PolarSystem: JavaPlugin() {
    override fun onEnable() {
        logger.info("Plugin Enabled")

        config.addDefault("require-level", 15)
        config.addDefault("remove-request-expiration", 60)
        config.addDefault("leave-request-expiration", 60)
        config.addDefault("kick-request-expiration", 60)
        config.addDefault("inventory-size", 36)
        config.addDefault("guild-member-limit", 5)
        config.options().copyDefaults(true)
        saveConfig()

        checkFolder()

        getCommand("guild")?.setExecutor(GuildBase(config.getInt("require-level"), config.getInt("guild-member-limit")))
        getCommand("gchat")?.setExecutor(GuildChat())
        getCommand("PolarSystemGuildRemoveAccept")?.setExecutor(_GuildRemoveAccept())
        getCommand("PolarSystemGuildRemoveDeny")?.setExecutor(_GuildRemoveDeny())
        getCommand("PolarSystemGuildLeaveAccept")?.setExecutor(_GuildLeaveAccept())
        getCommand("PolarSystemGuildLeaveDeny")?.setExecutor(_GuildLeaveDeny())
        getCommand("PolarSystemGuildKickAccept")?.setExecutor(_GuildKickAccept())
        getCommand("PolarSystemGuildKickDeny")?.setExecutor(_GuildKickDeny())

        val df = dataFolder.absolutePath

        server.pluginManager.registerEvents(PlayerLevelChangeEvent(df, config.getInt("require-level")), this)
        server.pluginManager.registerEvents(PlayerJoinEvent(df), this)
        server.pluginManager.registerEvents(GuildMember(), this)
        server.pluginManager.registerEvents(GuildInventory(), this)

        RequestExpirationTimer.expirationTime = config.getLong("remove-request-expiration")
        LeaveExpirationTimer.expirationTime = config.getLong("leave-request-expiration")
        KickExpirationTimer.expirationTime = config.getLong("kick-request-expiration")
        var isz = config.getInt("inventory-size")
        when {
            isz < 9 -> {
                isz = 9
                logger.warning("inventory size must be larger than 9")
            }
            isz > 54 -> {
                isz = 54
                logger.warning("inventory size must be smaller 54")
            }
            isz % 9 != 0 -> {
                isz -= (isz % 9)
                logger.warning("inventory size must be a multiple of 9")
            }
        }
        GuildInventory.size = isz

        GuildInventory.loadAll()
    }

    override fun onDisable() {
        logger.info("Plugin Disabled")

        GuildInventory.saveAll()
    }

    private fun checkFolder() {
        val df = dataFolder.absolutePath

        var file = File("$df/require/level.json")
        if (file.exists()) PlayerLevelChangeEvent.canUseGuild = jacksonObjectMapper().readValue(file)

        GuildBase.setDataFolder(df)

        file = File("$df/guild/table.json")
        if (file.exists()) GuildBase.PlayerToGuildId = GuildBase.readPlayerToGuildId()
        else File("$df/guild/").mkdir()
        file = File("$df/guild/guilds.json")
        if (file.exists()) GuildBase.Guilds = GuildBase.readGuilds()

        file = File("$df/buffer/")
        if (!file.exists()) File("$df/buffer/").mkdir()
        Buffer.dataFolder = df

        file = File("$df/inventory/")
        if (!file.exists()) File("$df/inventory/").mkdir()
        GuildInventory.dataFolder = df
    }
}