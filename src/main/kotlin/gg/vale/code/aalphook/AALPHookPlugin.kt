package gg.vale.code.aalphook

import co.aikar.commands.BukkitCommandManager
import com.hm.achievement.api.AdvancedAchievementsAPI
import com.hm.achievement.api.AdvancedAchievementsAPIFetcher
import me.lucko.luckperms.api.DataMutateResult
import me.lucko.luckperms.api.Group
import me.lucko.luckperms.api.LuckPermsApi
import me.lucko.luckperms.api.User
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.lang.RuntimeException

class AALPHookPlugin : JavaPlugin() {
    lateinit var config: AALPHookConfig
    internal lateinit var loadedHooks: Set<AALPHookConfig.Hook>
    private lateinit var advancedAchievements: AdvancedAchievementsAPI
    private lateinit var luckPerms: LuckPermsApi

    override fun onEnable() {
        logger.info("Preparing configuration...")
        config = AALPHookConfig(this)
        loadedHooks = config.getHooks()

        logger.info("Checking required plugins are installed and enabled, please wait...")
        checkPlugin("AdvancedAchievements") {
            val apiInst = AdvancedAchievementsAPIFetcher.fetchInstance()
            advancedAchievements = apiInst.orElseThrow {
                die("Failed to receive AdvancedAchievements API")
                throw RuntimeException("AdvancedAchievements is installed, enabled, and of the right version; however, the API could not be loaded.")
            }
        }

        checkPlugin("LuckPerms") {
            val provider = server.servicesManager.getRegistration(LuckPermsApi::class.java)
            if (provider == null) {
                die("Failed to receive LuckPerms API")
                throw RuntimeException("LuckPerms is installed and enabled; however, the API could not be loaded.")
            } else {
                luckPerms = provider.provider
            }
        }

        logger.info("OK! Registering interactivity...")
        server.pluginManager.registerEvents(AALPHookListener(this), this)

        logger.info("OK! Creating administrative command...")
        val manager = BukkitCommandManager(this)
        manager.enableUnstableAPI("help")
        manager.registerCommand(AALPHookCommand(this))

        logger.info("Fire up those engines, AALPHook is ready.")
    }

    private fun checkPlugin(name: String, then: (Plugin) -> Unit) {
        logger.info("Checking Plugin $name")
        val tempPlugin = server.pluginManager.getPlugin(name)
        if (tempPlugin != null && tempPlugin.isEnabled) {
            logger.info("Plugin $name is ok!")
            then(tempPlugin)
        } else {
            die("$name could not be found")
            return
        }
    }

    private fun die(message: String) {
        logger.severe("$message. AALPHook will be disabled.")
        server.pluginManager.disablePlugin(this)
    }

    fun synchronize(player: Player) {
        val user = luckPerms.userManager.getUser(player.uniqueId) ?: return

        loadedHooks.forEach loadedHooks@{ hook ->
            val requiredGroup = hook.requiredGroup
            if (requiredGroup != null) {
                val loaded = luckPerms.getGroup(requiredGroup)
                if (loaded == null || !user.inheritsGroup(loaded)) {
                    return@loadedHooks
                }
            }

            val meetsRequiredAchievementCriteria = hook.requiredAchievements.all {
                advancedAchievements.hasPlayerReceivedAchievement(player.uniqueId, it)
            }

            if (meetsRequiredAchievementCriteria) {
                hook.thenApplyActions.forEach applyActions@{ action ->

                    val toApply = luckPerms.getGroup(action.value) ?: return@applyActions

                    val result = when (action.key) {
                        AALPHookConfig.LuckPermsActionType.ADDGROUP -> doAddGroup(user, toApply)
                        AALPHookConfig.LuckPermsActionType.DELGROUP -> doDelGroup(user, toApply)
                    }

                    when (result.first) {
                        true -> {
                            when (result.second) {
                                "added" -> logger.info("${player.displayName} was added to group ${toApply.name}")
                                "removed" -> logger.info("${player.displayName} was removed from group ${toApply.name}")
                            }
                        }

                        false -> {
                            when (result.second) {
                                "in_group" -> logger.warning("LuckPerms reported user ${player.displayName} was already in group ${toApply.name}")
                                "unknown_err" -> logger.severe("LuckPerms reported an unknown error with the previously requested action.")
                            }
                        }
                    }
                }

                luckPerms.userManager.saveUser(user)
            }
        }
    }

    private fun doAddGroup(user: User, group: Group): Pair<Boolean, String> {
        if (user.inheritsGroup(group)) {
            return false to "in_group"
        }

        val result: DataMutateResult = user.setPermission(luckPerms.nodeFactory.makeGroupNode(group).build())
        return if (result.wasSuccess()) {
            true to "added"
        } else {
            false to "unknown_err"
        }
    }

    private fun doDelGroup(user: User, group: Group): Pair<Boolean, String> {
        if (!user.inheritsGroup(group)) {
            return false to "not_in_group"
        }

        val result = user.unsetPermission(luckPerms.nodeFactory.makeGroupNode(group).build())
        return if (result.wasSuccess()) {
            true to "removed"
        } else {
            false to "unknown_err"
        }
    }
}