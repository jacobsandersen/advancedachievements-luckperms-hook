package gg.vale.code.MY_PLUGIN_NAME

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.CatchUnknown
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default

@CommandAlias("MyPluginNameCommand")
class MyPluginNameCommand : BaseCommand() {
    @Default
    @CatchUnknown
    fun onDefault(help: CommandHelp) {
        help.showHelp()
    }
}