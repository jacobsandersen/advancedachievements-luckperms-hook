package com.algorithmjunkie.mc.aalphook

import co.aikar.commands.BukkitCommandManager
import com.algorithmjunkie.mc.aalphook.conf.HookConfig
import com.algorithmjunkie.mc.aalphook.conf.SettingsConfig
import com.algorithmjunkie.mc.aalphook.man.ApiManager
import com.algorithmjunkie.mc.aalphook.man.HookManager
import com.algorithmjunkie.mc.aalphook.man.LogManager
import com.algorithmjunkie.mc.aalphook.sync.SyncManager
import com.algorithmjunkie.mc.aalphook.sync.SyncTask
import com.hm.achievement.api.AdvancedAchievementsAPI
import com.hm.achievement.api.AdvancedAchievementsAPIFetcher
import net.luckperms.api.LuckPerms
import net.luckperms.api.context.DefaultContextKeys
import net.luckperms.api.node.Node
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.lang.RuntimeException

class AALPPlugin : JavaPlugin() {
    lateinit var log: LogManager
    lateinit var config: HookConfig
    lateinit var settings: SettingsConfig
    lateinit var apis: ApiManager
    lateinit var hooks: HookManager
    lateinit var sync: SyncManager

    override fun onEnable() {
        instance = this

        log = LogManager(this)

        log.inf("Preparing configuration...")
        config = HookConfig(this)
        settings = SettingsConfig(this)

        log.inf("Checking required plugins are installed and enabled, please wait...")
        apis = loadApis()
        
        log.inf("Loading hooks...")
        hooks = HookManager(this)

        log.inf("Creating synchronization manager...")
        sync = SyncManager(this)

        log.inf("Launching synchronization task...")
        server.scheduler.scheduleSyncRepeatingTask(this, SyncTask(this), 0L, settings.syncEveryTicks())

        log.inf("OK! Registering interactivity...")
        server.pluginManager.registerEvents(AchievementListener(this), this)

        log.inf("OK! Creating administrative command...")
        val manager = BukkitCommandManager(this)
        manager.enableUnstableAPI("help")
        manager.registerCommand(AALPCommand(this))

        log.inf("Fire up those engines, AALPHook is ready.")
    }

    private fun loadApis(): ApiManager {
        var achievements: AdvancedAchievementsAPI? = null
        checkPlugin("AdvancedAchievements") {
            val apiInst = AdvancedAchievementsAPIFetcher.fetchInstance()
            achievements = apiInst.orElseThrow {
                die("Failed to receive AdvancedAchievements API")
                throw RuntimeException("AdvancedAchievements is installed, enabled, and of the right version; however, the API could not be loaded.")
            }
        }

        var permissions: LuckPerms? = null
        checkPlugin("LuckPerms") {
            val provider = server.servicesManager.getRegistration(LuckPerms::class.java)
            if (provider == null) {
                die("Failed to receive LuckPerms API")
                throw RuntimeException("LuckPerms is installed and enabled; however, the API could not be loaded.")
            } else {
                permissions = provider.provider
            }
        }

        return ApiManager(achievements!!, permissions!!)
    }

    private fun checkPlugin(name: String, then: (Plugin) -> Unit) {
        log.inf("Checking Plugin $name")
        val tempPlugin = server.pluginManager.getPlugin(name)
        if (tempPlugin != null && tempPlugin.isEnabled) {
            log.inf("Plugin $name is ok!")
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

    companion object {
        lateinit var instance: AALPPlugin

        fun getLpGroupNode(groupName: String, server: String? = null, world: String? = null ): Node {
            return getLpNode("group.${groupName}", server, world)
        }

        fun getLpNode(node: String, server: String? = null, world: String? = null): Node {
            val builder = Node.builder(node)
            server?.let { builder.withContext(DefaultContextKeys.SERVER_KEY, server) }
            world?.let { builder.withContext(DefaultContextKeys.WORLD_KEY, world) }
            return builder.build();
        }
    }
}