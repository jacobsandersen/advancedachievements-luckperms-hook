package gg.vale.code.aalphook

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.command.CommandSender

@CommandAlias("aalp")
@CommandPermission("aalp.admin")
class AALPHookCommand(private val plugin: AALPHookPlugin) : BaseCommand() {
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
}