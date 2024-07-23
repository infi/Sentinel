package chat.revolt.sentinel.commands

import chat.revolt.sentinel.extensions.reply
import chat.revolt.sentinel.framework.Category
import chat.revolt.sentinel.framework.Command
import chat.revolt.sentinel.framework.CommandContext
import chat.revolt.sentinel.framework.SentinelFramework
import chat.revolt.sentinel.lookout.internals.PermissionBit

class HelpCommand : Command {
    override val name: String
        get() = "help"
    override val description: String
        get() = "Displays help."
    override val category: Category
        get() = Category.GENERAL
    override val usage: String
        get() = ""
    override val aliases: List<String>
        get() = listOf("?")
    override val permissions: List<PermissionBit>
        get() = emptyList()

    override suspend fun execute(context: CommandContext) {
        val commands = SentinelFramework.commands

        val helpMessage = commands.joinToString("\n") {
            getCommandUsageEntry(context.prefix, it)
        }

        context.message.reply("This is an alpha version of Sentinel reporting for duty! Here are the available commands:\n$helpMessage")
    }

    private fun getCommandUsageEntry(prefix: String, command: Command): String {
        val usage = if (command.usage.isEmpty()) "" else " ${command.usage}"
        return "**${prefix}${command.name}${usage}** - ${command.description}"
    }
}