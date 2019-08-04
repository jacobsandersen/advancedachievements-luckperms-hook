package com.algorithmjunkie.mc.aalphook.conf

import com.algorithmjunkie.mc.aalphook.AALPPlugin
import com.algorithmjunkie.mc.konfig.system.bukkit.BukkitKonfig

class SettingsConfig(private val plugin: AALPPlugin) : BukkitKonfig(plugin, "settings.yml", plugin.dataFolder) {
    fun syncEveryTicks(): Long {
        return getInteger("sync-every-mins") * 60L * 20L
    }

    fun isDebug(): Boolean {
        return getBoolean("debug")
    }

    fun setDebug(debug: Boolean) {
        set("debug", debug)
        saveFile()
    }

    override fun reload() {
        super.reload()
    }
}