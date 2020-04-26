package com.algorithmjunkie.mc.aalphook

import com.hm.achievement.utils.PlayerAdvancedAchievementEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class AchievementListener(private val plugin: AALPPlugin) : Listener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onAdvancedAchievementAward(event: PlayerAdvancedAchievementEvent) {
        plugin.server.scheduler.runTaskLater(
                plugin,
                Runnable { plugin.sync.synchronize(event.player) },
                20L
        )
    }
}