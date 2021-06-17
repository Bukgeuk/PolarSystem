package dev.bukgeuk.polarsystem.duplication

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Furnace
import org.bukkit.inventory.ItemStack
import kotlin.concurrent.timer

class DuplicationTimer {
    companion object {
        var leftTimeTable: MutableMap<Location, Triple<Long, Long, Int>> = mutableMapOf() // Location, leftTime, Term, Amount
        @JvmField val task = Runnable {
            for (item in leftTimeTable.keys) {
                val it = leftTimeTable[item]
                if (it != null && item.block.type != Material.AIR) {
                    val nf = item.block.state as Furnace
                    if (nf.inventory.smelting == null) break

                    nf.burnTime = 1200
                    nf.update()

                    if (it.first == 1.toLong()) {
                        leftTimeTable[item] = it.copy(first = it.second * 2, second = it.second, third = it.third)

                        val target = nf.inventory.smelting?.clone()
                        if (target != null) {
                            if (nf.inventory.result != null) {
                                if (nf.inventory.result?.type == target.type) {
                                    if (nf.inventory.result!!.amount + it.third > 64)
                                        target.amount = 64
                                    else
                                        target.amount = nf.inventory.result!!.amount + it.third
                                }
                            } else target.amount = it.third

                            nf.inventory.result = target
                        }
                    } else {
                        leftTimeTable[item] = it.copy(first = it.first - 1, second = it.second, third = it.third)
                    }
                }
            }
        }
    }

    fun startTimer(id: Location, term: Long, amount: Int) {
        val f = id.block.state as Furnace
        f.customName = furnaceCustomName
        f.update()
        leftTimeTable[id] = Triple(term * 2, term, amount)
    }

    fun removeTimer(id: Location) {
        val f = id.block.state as Furnace
        f.burnTime = 0
        f.customName = ItemStack(Material.FURNACE).itemMeta?.localizedName
        f.update()
        leftTimeTable.remove(id)
    }
}