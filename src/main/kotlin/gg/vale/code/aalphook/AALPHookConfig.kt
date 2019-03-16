package gg.vale.code.aalphook

import com.algorithmjunkie.mc.konfig.system.bukkit.BukkitKonfig

class AALPHookConfig(private val plugin: AALPHookPlugin) : BukkitKonfig(plugin, "config.yml", plugin.getDataFolder()) {
    fun getHooks(): Set<Hook> {
        val out = HashSet<Hook>()

        backend.getKeys(false).forEach {
            val sect = backend.getConfigurationSection(it)
            if (sect != null) {
                out.add(Hook(
                        sect.getString("if-already-in-luckperms-group"),
                        sect.getStringList("if-achievements"),
                        sect.getStringList("then-luckperms-groups")
                ))
            } else {
                plugin.logger.severe("One of the hooks ($it) could not be loaded. Please check the configuration! I loaded what I could.")
            }
        }

        return out
    }

    override fun reload() {
        super.reload()
        plugin.applyHooks(this)
    }

    data class Hook(
            val requiredGroup: String?,
            val requiredAchievements: List<String>,
            val thenApplyGroups: List<String>
    )
}