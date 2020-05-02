package com.algorithmjunkie.mc.aalphook.conf

import com.algorithmjunkie.mc.aalphook.AALPPlugin
import com.algorithmjunkie.mc.aalphook.hook.*
import com.algorithmjunkie.mc.konfig.system.bukkit.BukkitKonfig
import org.bukkit.ChatColor
import java.lang.RuntimeException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class HookConfig(private val plugin: AALPPlugin) : BukkitKonfig(plugin, "hooks.yml", plugin.dataFolder) {
    fun splitPackedPerm(string: String?): Triple<String, String?, String?>? {
        if(string == null) return null

        val unpack = string.split(":")

        if(unpack.isEmpty()) return null

        val target = unpack[0]
        val server = if (unpack.size > 1) unpack[1] else null
        val world = if (unpack.size > 2) unpack[2] else null

        return Triple(target, server, world)
    }

    fun getHooks(): Set<Hook> {
        val out = HashSet<Hook>()

        backend.getKeys(false).forEach { key ->
            val sect = backend.getConfigurationSection(key)
            if (sect != null) {
                val exactGroupInfo = sect.getString("if-luckperms-group-exact")
                val minimumGroupInfo = sect.getConfigurationSection("if-luckperms-group-minimal")

                val groupInfo = when {
                    exactGroupInfo != null -> {
                        val triple = splitPackedPerm(exactGroupInfo)
                        RequiredGroupInfo(triple?.first?.let { AALPPlugin.getLpGroupNode(it, triple.second, triple.third) }, null)
                    }

                    minimumGroupInfo != null -> {
                        val triple = splitPackedPerm(minimumGroupInfo.getString("name"))
                        RequiredGroupInfo(
                                triple?.first?.let { AALPPlugin.getLpGroupNode(it, triple.second, triple.third) },
                                minimumGroupInfo.getString("track")
                        )
                    }

                    else -> RequiredGroupInfo(null, null)
                }

                val luckPermsActions = ArrayList<Action>()
                sect.getStringList("then-luckperms-actions").forEach { action ->
                    val actionToGroup = action.split(":")
                    val actionType = when (actionToGroup[0].toLowerCase().trim()) {
                        "addgroup" -> HookActionType.ADDGROUP
                        "delgroup" -> HookActionType.DELGROUP
                        "addperm" -> HookActionType.ADDPERM
                        "delperm" -> HookActionType.DELPERM
                        else -> throw RuntimeException("Unknown Action Type was found and could not be decoded.")
                    }
                    val value = actionToGroup[1].trim();

                    val server = if (actionToGroup.size > 2) actionToGroup[2].trim() else null
                    val world = if (actionToGroup.size > 3) actionToGroup[3].trim() else null

                    val node = when (actionType) {
                        HookActionType.ADDGROUP, HookActionType.DELGROUP -> AALPPlugin.getLpGroupNode(value, server, world)
                        HookActionType.ADDPERM, HookActionType.DELPERM -> AALPPlugin.getLpNode(value, server, world)
                    }

                    luckPermsActions.add(Action(actionType, node))
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