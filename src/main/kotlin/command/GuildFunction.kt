package dev.bukgeuk.polarsystem.command

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.bukgeuk.polarsystem.util.Color
import dev.bukgeuk.polarsystem.util.ColoredChat
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.block.Skull
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.http.WebSocket
import java.util.*
import kotlin.math.ceil
import kotlin.properties.Delegates
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

class GuildChat: CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            if (GuildBase.PlayerToGuildId[sender.uniqueId] != null) {
                if (args.isNotEmpty()) {
                    val id = GuildBase.PlayerToGuildId[sender.uniqueId]
                    val text = args.joinToString(" ")
                    sender.sendMessage("${Color.BLUE}[${Color.RED}me${Color.BLUE} -> ${ChatColor.GOLD}${GuildBase.Guilds[id]!!.name}${Color.BLUE}]${ChatColor.RESET} ${ColoredChat().hexToColor(text)}")
                    for (uuid in GuildBase.Guilds[id]!!.members) {
                        if (uuid == sender.uniqueId) continue
                        Bukkit.getPlayer(uuid)?.sendMessage("${Color.BLUE}[${Color.RED}${sender.name}${Color.BLUE} -> ${ChatColor.GOLD}${GuildBase.Guilds[id]!!.name}${Color.BLUE}]${ChatColor.RESET} ${ColoredChat().hexToColor(text)}")
                    }
                    return true
                }
                return false
            }
            sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드에 가입되어 있지 않습니다")
            return true
        }
        sender.sendMessage("${ChatColor.RED}${ChatColor.BOLD}Error: ${ChatColor.RESET}This command isn't available on the console")
        return true
    }
}

class GuildMember: Listener {
    fun onCommand(sender: Player, args: Array<out String>) {
        val id = GuildBase.PlayerToGuildId[sender.uniqueId]
        if (id != null) {
            val inv = Bukkit.createInventory(null, 27, "길드: ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}" +
                    " ${ChatColor.RESET}(1/${ceil(GuildBase.Guilds[id]!!.members.size.toFloat() / 9).toInt()})")
            initialize(id, inv, 0)
            sender.openInventory(inv)
            return
        }
        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드에 가입되어 있지 않습니다")
    }

    private fun initialize(id: Int, inv: Inventory, page: Int) {
        val members = GuildBase.Guilds[id]!!.members

        for (idx in 0 until 9) {
            val i = page * 9 + idx
            if (members.size <= i) break

            val head = ItemStack(Material.PLAYER_HEAD, 1)
            val meta: SkullMeta = head.itemMeta as SkullMeta
            val ofp = Bukkit.getOfflinePlayer(members[i])
            meta.owningPlayer = ofp

            if (GuildBase.Guilds[id]!!.leader == members[i]) meta.setDisplayName("${ChatColor.GOLD}${ofp.name}")
            else meta.setDisplayName("${ChatColor.DARK_AQUA}${ofp.name}")

            head.itemMeta = meta
            inv.setItem(idx, head)

            val p = Bukkit.getPlayer(members[i])
            val status: ItemStack
            if (p != null) {
                if (p.isDead) {
                    status = ItemStack(Material.RED_CONCRETE, 1)
                    val smeta = status.itemMeta
                    smeta?.setDisplayName("${Color.STATUSRED}${ChatColor.BOLD}사망함")
                    status.itemMeta = smeta
                }
                else {
                    status = ItemStack(Material.GREEN_CONCRETE, 1)
                    val smeta = status.itemMeta
                    smeta?.setDisplayName("${Color.STATUSGREEN}${ChatColor.BOLD}온라인")
                    status.itemMeta = smeta
                }
            } else {
                status = ItemStack(Material.GRAY_CONCRETE, 1)
                val smeta = status.itemMeta
                smeta?.setDisplayName("${Color.STATUSGRAY}${ChatColor.BOLD}오프라인")
                status.itemMeta = smeta
            }

            inv.setItem(9 + idx, status)

            // ui
            if (page > 0) {
                val item = ItemStack(Material.ARROW, 1)
                val imeta = item.itemMeta
                imeta?.setDisplayName("${ChatColor.GREEN}이전 페이지")
                imeta?.lore = mutableListOf("${ChatColor.YELLOW}${page} 페이지")
                item.itemMeta = imeta

                inv.setItem(21, item)
            }
            if (page < ceil(GuildBase.Guilds[id]!!.members.size.toFloat() / 9).toInt() - 1) {
                val item = ItemStack(Material.ARROW, 1)
                val imeta = item.itemMeta
                imeta?.setDisplayName("${ChatColor.GREEN}다음 페이지")
                imeta?.lore = mutableListOf("${ChatColor.YELLOW}${page + 2} 페이지")
                item.itemMeta = imeta

                inv.setItem(23, item)
            }
        }
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        val id = GuildBase.PlayerToGuildId[e.whoClicked.uniqueId] ?: return
        val pattern = Regex("길드: ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)} ${ChatColor.RESET}\\((\\d)\\/\\d\\)")
        val res = pattern.matchEntire(e.view.title)
        if (res != null) {
            e.isCancelled = true

            if (e.currentItem?.type == Material.ARROW) {
                val page = when (e.currentItem?.itemMeta?.displayName) {
                    "${ChatColor.GREEN}이전 페이지" -> {
                        res.groupValues[1].toInt() - 2
                    }
                    "${ChatColor.GREEN}다음 페이지" -> {
                        res.groupValues[1].toInt()
                    }
                    else -> return
                }
                val inv = Bukkit.createInventory(null, 27, "길드: ${ChatColor.GOLD}${ColoredChat().hexToColor(GuildBase.Guilds[id]!!.name)}" +
                        " ${ChatColor.RESET}(${page + 1}/${ceil(GuildBase.Guilds[id]!!.members.size.toFloat() / 9).toInt()})")
                initialize(id, inv, page)
                e.whoClicked.openInventory(inv)
            }
        }
    }
}

class GuildInventory: Listener {
    companion object {
        lateinit var dataFolder: String
        var size by Delegates.notNull<Int>()
        @JvmField var Inventories: MutableMap<Int, Inventory> = mutableMapOf()

        private fun load(id: Int, inv: Inventory) {
            val serializedData: MutableList<String>

            val file = File("$dataFolder/inventory/$id.json")

            if (file.exists()) {
                serializedData = jacksonObjectMapper().readValue(file)
                var i = 0

                if (serializedData.isNotEmpty()) {
                    for (it in serializedData) {
                        val byte = Base64.getDecoder().decode(it)
                        val byteArrayInputStream = ByteArrayInputStream(byte)
                        val bukkitObjectInputStream = BukkitObjectInputStream(byteArrayInputStream)
                        val map = bukkitObjectInputStream.readObject() as MutableMap<String, Any>
                        val item = ItemStack.deserialize(map)
                        inv.setItem(i, item)
                        i++
                    }
                }
            }
        }

        fun loadAll() {
            for (id in GuildBase.Guilds.keys) {
                val inv = createInventory()
                load(id, inv)
                Inventories[id] = inv
            }
        }

        private fun save(id: Int, inv: Inventory) {
            val data: List<ItemStack?> = inv.storageContents.toList()
            val serializedData: MutableList<String> = mutableListOf()

            val file = File("$dataFolder/inventory/$id.json")

            if (data.isNotEmpty()) {
                for (it in data) {
                    val item: ItemStack = it ?: ItemStack(Material.AIR)
                    val map: MutableMap<String, Any> = item.serialize()

                    val byteArrayOutputStream = ByteArrayOutputStream()
                    val bukkitObjectOutputStream = BukkitObjectOutputStream(byteArrayOutputStream)
                    bukkitObjectOutputStream.writeObject(map)
                    bukkitObjectOutputStream.flush()
                    val byte = byteArrayOutputStream.toByteArray()
                    serializedData.add(String(Base64.getEncoder().encode(byte)))
                }
            }
            jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, serializedData)
        }

        fun saveAll() {
            for (id in GuildBase.Guilds.keys) {
                save(id, Inventories[id] ?: createInventory())
            }
        }

        private fun createInventory(): Inventory {
            return Bukkit.createInventory(null, size, "길드 보관함")
        }
    }

    fun onCommand(sender: Player, args: Array<out String>) {
        val id = GuildBase.PlayerToGuildId[sender.uniqueId]
        if (id != null) {
            if (Inventories[id] == null)
                sender.openInventory(createInventory())
            else
                sender.openInventory(Inventories[id]!!)

            return
        }
        sender.sendMessage("${Color.GRAY}[PolarSystem] ${ChatColor.RESET}길드에 가입되어 있지 않습니다")
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        if (e.view.title != "길드 보관함") return

        val id = GuildBase.PlayerToGuildId[e.whoClicked.uniqueId]!!

        Inventories[id] = e.inventory
    }

    @EventHandler
    fun onInventoryClose(e: InventoryCloseEvent) {
        if (e.view.title != "길드 보관함") return

        save(GuildBase.PlayerToGuildId[e.player.uniqueId]!!, e.inventory)
    }
}