package gg.vale.code.MY_PLUGIN_NAME

import co.aikar.commands.BukkitCommandManager
import org.bukkit.plugin.java.JavaPlugin

class MyPluginNamePlugin : JavaPlugin() {
    override fun onEnable() {
        println("Enabled")
        val commands = BukkitCommandManager(this)
        commands.enableUnstableAPI("help")
        commands.registerCommand(MyPluginNameCommand())
    }

    override fun onDisable() {
        println("Disabled")
    }
}