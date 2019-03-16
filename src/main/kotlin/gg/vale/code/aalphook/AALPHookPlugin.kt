package gg.vale.code.aalphook

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandManager
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import com.hm.achievement.api.AdvancedAchievementsAPI
import com.hm.achievement.api.AdvancedAchievementsAPIFetcher
import com.hm.achievement.utils.PlayerAdvancedAchievementEvent
import me.lucko.luckperms.api.DataMutateResult
import me.lucko.luckperms.api.LuckPermsApi
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.lang.RuntimeException

class AALPHookPlugin : JavaPlugin() {
    lateinit var config: AALPHookConfig
    private lateinit var loadedHooks: Set<AALPHookConfig.Hook>
    private lateinit var advancedAchievements: AdvancedAchievementsAPI
    private lateinit var luckPerms: LuckPermsApi

    override fun onEnable() {
        logger.info("Preparing configuration...")
        config = AALPHookConfig(this)
        applyHooks(config)

        logger.info("Checking required plugins are installed and enabled, please wait...")
        checkPlugin("AdvancedAchievements", { it.description.version[0].toInt() == 5 }, "Please ensure AdvancedAchievements is at least version 5 or above.") {
            val apiInst = AdvancedAchievementsAPIFetcher.fetchInstance()
            advancedAchievements = apiInst.orElseThrow {
                suicide("Failed to receive AdvancedAchievements API")
                throw RuntimeException("AdvancedAchievements is installed, enabled, and of the right version; however, the API could not be loaded.")
            }
        }

        checkPlugin("LuckPerms") {
            val provider = server.servicesManager.getRegistration(LuckPermsApi::class.java)
            if (provider == null) {
                suicide("Failed to receive LuckPerms API")
                throw RuntimeException("LuckPerms is installed and enabled; however, the API could not be loaded.")
            } else {
                luckPerms = provider.provider
            }
        }

        logger.info("OK! Registering interactivity...")
        server.pluginManager.registerEvents(AALPHookListener(this), this)

        logger.info("OK! Creating administrative command...")
        val manager = BukkitCommandManager(this)
        manager.registerCommand(AALPHookCommand(this))

        logger.info("Fire up those engines, AALPHook is ready.")
    }

    internal fun applyHooks(config: AALPHookConfig) {
        loadedHooks = config.getHooks()
    }

    private fun checkPlugin(name: String, verifier: ((Plugin) -> Boolean)? = null, messageIfVerifyFailed: String? = null, then: (Plugin) -> Unit) {
        logger.info("Checking Plugin $name")
        val tempPlugin = server.pluginManager.getPlugin("AdvancedAchievements")
        if (tempPlugin != null && tempPlugin.isEnabled) {
            if (verifier != null) {
                val verified = verifier(tempPlugin)
                if (!verified) {
                    suicide("$name could not be properly verified")
                    if (!messageIfVerifyFailed.isNullOrEmpty()) logger.severe(messageIfVerifyFailed)
                    return
                }
            }

            logger.info("Plugin ok!")
            then(tempPlugin)
        } else {
            suicide("$name could not be found")
            return
        }
    }

    private fun suicide(message: String) {
        logger.severe("$message. AALPHook will be disabled.")
        server.pluginManager.disablePlugin(this)
    }

    fun synchronize(player: Player) {
        logger.info("${player.displayName} earned an achievement, checking if LP promotion is required...")
        val user = luckPerms.userManager.getUser(player.uniqueId) ?: return

        loadedHooks.forEach loadedHooks@{ hook ->
            val requiredGroup = hook.requiredGroup
            if (requiredGroup != null) {
                val loaded = luckPerms.getGroup(requiredGroup) ?: return@loadedHooks
                if (!user.inheritsGroup(loaded)) return@loadedHooks
            }

            val meetsRequiredAchievementCriteria = hook.requiredAchievements.all {
                advancedAchievements.hasPlayerReceivedAchievement(player.uniqueId, it)
            }

            if (meetsRequiredAchievementCriteria) {
                logger.info("${player.displayName} meets the criteria for Hook \"$hook\".")
                hook.thenApplyGroups.forEach applyGroups@{ group ->
                    val isPrimary = group.startsWith("primary:")
                    val toApplyStr = if (isPrimary) group.substring(6) else group // remove "primary:" if primary
                    val toApply = luckPerms.getGroup(toApplyStr) ?: return@applyGroups

                    val result: DataMutateResult
                    if (isPrimary && user.primaryGroup != toApplyStr) {
                        user.primaryGroup = toApplyStr
                        result = user.setPrimaryGroup(toApplyStr)
                    } else {
                        result = user.setPermission(luckPerms.nodeFactory.makeGroupNode(toApply).build())
                    }

                    if (result.wasSuccess()) {
                        logger.info("${player.displayName} was promoted to group $toApply (primary? $isPrimary)")
                    } else {
                        logger.severe("LuckPerms reported it was unable to assign group $toApply to player ${player.displayName}!")
                    }
                }
            }
        }
    }
}