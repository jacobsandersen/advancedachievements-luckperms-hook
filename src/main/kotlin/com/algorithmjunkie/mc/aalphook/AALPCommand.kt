package com.algorithmjunkie.mc.aalphook

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.command.CommandSender

@CommandAlias("aalp")
@CommandPermission("aalp.admin")
class AALPCommand(private val plugin: AALPPlugin) : BaseCommand() {
    @Default
    @CatchUnknown
    fun onDefault(help: CommandHelp) = help.showHelp()

    @Subcommand("reload")
    fun reload(sender: CommandSender) {
        plugin.config.reload()
        sender.spigot().sendMessage(
                *ComponentBuilder("AALP was reloaded.")
                        .color(ChatColor.GREEN).create()
        )
    }

    @Subcommand("debug")
    fun debug(sender: CommandSender) {
        val settings = plugin.settings
        val new = !settings.isDebug()
        settings.setDebug(new)
        settings.reload()
        sender.spigot().sendMessage(
                *ComponentBuilder("AALP Debug Mode was toggled. It is now: $new")
                        .color(ChatColor.YELLOW)
                        .create()
        )
    }
}