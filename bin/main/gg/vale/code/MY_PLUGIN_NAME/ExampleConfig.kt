package gg.vale.code.MY_PLUGIN_NAME

import com.algorithmjunkie.mc.konfig.system.bukkit.BukkitKonfig
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin

class ExampleConfig(plugin: JavaPlugin) : BukkitKonfig(plugin, "config.yml", plugin.getDataFolder()) {
    fun getSomeValue(): String {
        return getString("some-value")
    }
}