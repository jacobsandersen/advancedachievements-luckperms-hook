package gg.vale.code.aalphook

import com.algorithmjunkie.mc.konfig.system.bukkit.BukkitKonfig
import java.lang.RuntimeException

class AALPHookConfig(private val plugin: AALPHookPlugin) : BukkitKonfig(plugin, "config.yml", plugin.dataFolder) {
    fun getHooks(): Set<Hook> {
        val out = HashSet<Hook>()

        backend.getKeys(false).forEach { key ->
            val sect = backend.getConfigurationSection(key)
            if (sect != null) {
                val luckPermsActions = LinkedHashMap<LuckPermsActionType, String>()
                sect.getStringList("then-luckperms-actions").forEach { action ->
                    val actionToGroup = action.split(":")
                    val actionType= when (actionToGroup[0].toLowerCase().trim()) {
                        "addgroup" -> LuckPermsActionType.ADDGROUP
                        "delgroup" -> LuckPermsActionType.DELGROUP
                        "addperm" -> LuckPermsActionType.ADDPERM
                        "delperm" -> LuckPermsActionType.DELPERM
                        else -> throw RuntimeException("Unknown Action Type was found and could not be decoded.")
                    }

                    luckPermsActions[actionType] = actionToGroup[1].trim()
                }

                out.add(Hook(
                        sect.getString("if-already-in-luckperms-group"),
                        sect.getStringList("if-achievements"),
                        luckPermsActions
                ))
            } else {
                plugin.logger.severe("One of the hooks ($key) could not be loaded. Please check the configuration! I loaded what I could.")
            }
        }

        return out
    }

    override fun reload() {
        super.reload()
        plugin.loadedHooks = getHooks()
    }

    data class Hook(
            val requiredGroup: String?,
            val requiredAchievements: List<String>,
            val thenApplyActions: Map<LuckPermsActionType, String>
    )

    enum class LuckPermsActionType {
        ADDGROUP,
        DELGROUP,
        ADDPERM,
        DELPERM;
    }
}