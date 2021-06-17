package dev.bukgeuk.polarsystem.duplication

import org.bukkit.Material
import org.bukkit.block.Furnace
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryType

class RemoveDuplicationTimer: Listener {
    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) {
        if (e.clickedInventory?.type == InventoryType.FURNACE) {
            if (e.clickedInventory?.location != null) {
                if (e.slotType == InventoryType.SlotType.FUEL && e.currentItem?.itemMeta?.displayName == moduleItemDisplayName) {
                    DuplicationTimer().removeTimer(e.clickedInventory?.location!!)
                }
            }
        }
    }

    @EventHandler
    fun onBlockBreak(e: BlockBreakEvent) {
        if (e.block.type == Material.FURNACE) {
            val f = e.block.state as Furnace
            if (f.inventory.fuel?.type == Material.CLOCK && f.inventory.fuel?.itemMeta?.displayName == moduleItemDisplayName) {
                DuplicationTimer().removeTimer(f.location)
            }
        }
    }

}