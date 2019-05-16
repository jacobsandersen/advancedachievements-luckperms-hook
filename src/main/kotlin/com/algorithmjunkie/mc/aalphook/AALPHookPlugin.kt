package com.algorithmjunkie.mc.aalphook

import co.aikar.commands.BukkitCommandManager
import com.hm.achievement.api.AdvancedAchievementsAPI
import com.hm.achievement.api.AdvancedAchievementsAPIFetcher
import com.algorithmjunkie.mc.aalphook.AALPHookConfig.LuckPermsActionType.*
import me.lucko.luckperms.api.*
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.lang.RuntimeException
import java.util.logging.Level

class AALPHookPlugin : JavaPlugin() {
    lateinit var config: AALPHookConfig
    lateinit var settings: AALPHookSettings
    internal lateinit var loadedHooks: Set<AALPHookConfig.Hook>
    internal var debug: Boolean = false
    private lateinit var advancedAchievements: AdvancedAchievementsAPI
    private lateinit var luckPerms: LuckPermsApi

    override fun onEnable() {
        logger.info("Preparing configuration...")
        config = AALPHookConfig(this)
        loadedHooks = config.getHooks()
        settings = AALPHookSettings(this)
        debug = settings.isDebug()

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

    private fun debug(message: String, level: Level = Level.INFO) {
        if (debug) logger.log(level, message)
    }

    fun synchronize(player: Player) {
        val pdn = player.displayName
        val user = luckPerms.userManager.getUser(player.uniqueId)
        if (user == null) {
            debug("Failed to load LP User Object for player $pdn")
            return
        }

        debug("Running hooks for player $pdn")
        for (hook in loadedHooks) {
            debug("Running hook $hook for $pdn")
            val requiredGroup = hook.requiredGroup
            if (requiredGroup != null) {
                debug("Hook $hook requires LP group $requiredGroup")
                val loaded = luckPerms.getGroup(requiredGroup)
                if (loaded == null) {
                    debug("Failed to load LP Group Object for group $requiredGroup")
                    continue
                }

                debug("Checking if player $pdn has required group for hook $hook")
                val node = luckPerms.nodeFactory.makeGroupNode(loaded).build()
                if (!user.hasPermission(node).asBoolean()) {
                    debug("Player $pdn did not have the required group for $hook")
                    continue
                }

                debug("Player $pdn did have the required group for $hook; hook processing will proceed.")
            }

            debug("Checking required achievement criteria for hook $hook")
            val meetsRequiredAchievementCriteria = hook.requiredAchievements.all {
                debug("Checking $pdn has completed achievement $it")
                val completed = advancedAchievements.hasPlayerReceivedAchievement(player.uniqueId, it)
                debug("User has ${if (completed) "" else "not"} completed achievement $it")
                completed
            }

            if (meetsRequiredAchievementCriteria) {
                debug("Hook $hook achievement criteria was met by $pdn; hook processing will proceed.")

                for (action in hook.thenApplyActions) {
                    debug("Running $hook action $action for $pdn")

                    val target = action.value

                    val result = when (action.key) {
                        ADDGROUP -> doAddGroup(user, target)
                        DELGROUP -> doDelGroup(user, target)
                        ADDPERM -> doAddPerm(user, target)
                        DELPERM -> doDelPerm(user, target)
                    }

                    debug("Processing result for hook $hook action $action")
                    when (result.first) {
                        true -> {
                            when (result.second) {
                                "added" -> logger.info("${player.displayName} was added to group or permission $target")
                                "removed" -> logger.info("${player.displayName} was removed from group or permission $target")
                            }
                        }

                        false -> {
                            when (result.second) {
                                "in_group" -> logger.warning("LuckPerms reported user ${player.displayName} was already in group $target")
                                "not_in_group" -> logger.warning("LuckPerms reported user ${player.displayName} was not already in group $target")
                                "unknown_err" -> logger.severe("LuckPerms reported an unknown error with the previously requested action.")
                                "not_found" -> logger.severe("The target group $target could not be found.")
                            }
                        }
                    }
                }

                debug("Synchronizing changes with LuckPerms...")
                luckPerms.userManager.saveUser(user)
                debug("Done!")
            }
        }
    }

    private fun resolveGroup(group: String): Node? {
        val toApply = luckPerms.getGroup(group)

        if (toApply == null) {
            debug("Could not resolve LP Group Object referenced by hook!")
            return null
        }

        return luckPerms.nodeFactory.makeGroupNode(toApply).build()
    }

    private fun doAddGroup(user: User, group: String): Pair<Boolean, String> {
        val resolved = resolveGroup(group) ?: return false to "not_found"

        if (user.hasPermission(resolved).asBoolean()) {
            return false to "in_group"
        }

        return doAddPerm(user, resolved)
    }

    private fun doAddPerm(user: User, node: String): Pair<Boolean, String> {
        return doAddPerm(user, luckPerms.buildNode(node).build())
    }

    private fun doAddPerm(user: User, node: Node): Pair<Boolean, String> {
        val result: DataMutateResult = user.setPermission(node)
        return if (result.wasSuccess()) {
            true to "added"
        } else {
            false to "unknown_err"
        }
    }

    private fun doDelGroup(user: User, group: String): Pair<Boolean, String> {
        val resolved = resolveGroup(group) ?: return false to "not_found"

        if (!user.hasPermission(resolved).asBoolean()) {
            return false to "not_in_group"
        }

        return doDelPerm(user, resolved)
    }

    private fun doDelPerm(user: User, node: String): Pair<Boolean, String> {
        return doDelPerm(user, luckPerms.buildNode(node).build())
    }

    private fun doDelPerm(user: User, node: Node): Pair<Boolean, String> {
        val result = user.unsetPermission(node)
        return if (result.wasSuccess()) {
            true to "removed"
        } else {
            false to "unknown_err"
        }
    }
}