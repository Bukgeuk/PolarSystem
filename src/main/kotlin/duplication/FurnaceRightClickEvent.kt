package dev.bukgeuk.polarsystem.duplication

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Furnace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import javax.naming.Name

class FurnaceRightClickEvent(private val plugin: JavaPlugin): Listener {
    @EventHandler
    fun onRightClick(e: PlayerInteractEvent) {
        val p = e.player
        if (e.action == Action.RIGHT_CLICK_BLOCK && e.item?.type == Material.CLOCK && e.item?.itemMeta?.displayName == moduleItemDisplayName) {
            val b = e.clickedBlock
            if (b?.type == Material.FURNACE) {
                val periodKey = NamespacedKey(plugin, "period")
                val amountKey = NamespacedKey(plugin, "amount")
                val container = e.item?.itemMeta?.persistentDataContainer
                if (container?.has(periodKey, PersistentDataType.LONG) == true && container.has(amountKey, PersistentDataType.INTEGER)) {
                    val nf = b.state as Furnace
                    val item = p.inventory.itemInMainHand
                    if (item.type != nf.inventory.fuel?.type && nf.inventory.fuel?.type != Material.AIR && nf.inventory.fuel != null) return
                    if (item.amount == 1) {
                        nf.inventory.fuel = item
                        p.inventory.setItemInMainHand(ItemStack(Material.AIR))
                    } else {
                        item.amount = item.amount - 1
                        p.inventory.setItemInMainHand(item)
                        item.amount = 1
                        nf.inventory.fuel = item
                    }

                    e.isCancelled = true

                    DuplicationTimer().startTimer(b.location, container.get(periodKey, PersistentDataType.LONG)!!, container.get(amountKey, PersistentDataType.INTEGER)!!)
                }
            }
        }
    }
}