package com.algorithmjunkie.mc.aalphook.conf

import com.algorithmjunkie.mc.aalphook.AALPPlugin
import com.algorithmjunkie.mc.aalphook.hook.Hook
import com.algorithmjunkie.mc.aalphook.hook.HookActionType
import com.algorithmjunkie.mc.aalphook.hook.RequiredGroupInfo
import com.algorithmjunkie.mc.konfig.system.bukkit.BukkitKonfig
import org.bukkit.ChatColor
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap

class HookConfig(private val plugin: AALPPlugin) : BukkitKonfig(plugin, "hooks.yml", plugin.dataFolder) {
    fun getHooks(): Set<Hook> {
        val out = HashSet<Hook>()

        backend.getKeys(false).forEach { key ->
            val sect = backend.getConfigurationSection(key)
            if (sect != null) {
                val exactGroupInfo = sect.getString("if-luckperms-group-exact")
                val minimumGroupInfo = sect.getConfigurationSection("if-luckperms-group-minimal")

                val groupInfo = when {
                    exactGroupInfo != null -> RequiredGroupInfo(exactGroupInfo, null)

                    minimumGroupInfo != null -> RequiredGroupInfo(
                            minimumGroupInfo.getString("name"),
                            minimumGroupInfo.getString("track")
                    )

                    else -> RequiredGroupInfo(null, null)
                }

                val luckPermsActions = LinkedHashMap<HookActionType, String>()
                sect.getStringList("then-luckperms-actions").forEach { action ->
                    val actionToGroup = action.split(":")
                    val actionType = when (actionToGroup[0].toLowerCase().trim()) {
                        "addgroup" -> HookActionType.ADDGROUP
                        "delgroup" -> HookActionType.DELGROUP
                        "addperm" -> HookActionType.ADDPERM
                        "delperm" -> HookActionType.DELPERM
                        else -> throw RuntimeException("Unknown Action Type was found and could not be decoded.")
                    }

                    luckPermsActions[actionType] = actionToGroup[1].trim()
                }

                val thenSendMessages = LinkedList<String>()
                sect.getStringList("on-success-then-send-messages").forEach { thenSendMessages.add(ChatColor.translateAlternateColorCodes('&', it)) }

                out.add(Hook(key, groupInfo, sect.getStringList("if-achievements"), luckPermsActions, thenSendMessages))
            } else {
                plugin.logger.severe("One of the hooks ($key) could not be loaded. Please check the configuration! I loaded what I could.")
            }
        }

        return out
    }

    override fun reload() {
        super.reload()
        plugin.hooks.loadedHooks = getHooks()
    }
}