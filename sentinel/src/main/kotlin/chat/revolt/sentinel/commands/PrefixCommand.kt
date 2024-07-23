package chat.revolt.sentinel.commands

import chat.revolt.sentinel.database.utils.ServerService
import chat.revolt.sentinel.extensions.reply
import chat.revolt.sentinel.extensions.server
import chat.revolt.sentinel.framework.Category
import chat.revolt.sentinel.framework.Command
import chat.revolt.sentinel.framework.CommandContext
import chat.revolt.sentinel.lookout.internals.PermissionBit

class PrefixCommand : Command {
    override val name: String
        get() = "prefix"
    override val description: String
        get() = "Display or change this server's prefix."
    override val category: Category
        get() = Category.GENERAL
    override val usage: String
        get() = "(<new_prefix>?|reset)"
    override val aliases: List<String>
        get() = emptyList()
    override val permissions: List<PermissionBit>
        get() = listOf(PermissionBit.ManageServer)

    override suspend fun execute(context: CommandContext) {
        when {
            context.args.isNotEmpty() && context.args[0] == "reset" -> {
                ServerService.setCustomPrefix(context.message.server ?: return, null)
                context.message.reply("Prefix reset to **`${ServerService.defaultPrefixFor(context.message.server ?: return)}`**.")
            }

            context.args.isNotEmpty() -> {
                val newPrefix = context.args[0]
                ServerService.setCustomPrefix(context.message.server ?: return, newPrefix)
                context.message.reply("Prefix set to `$newPrefix`. To unset it, type `${newPrefix}prefix reset`.")
            }

            else -> {
                val prefix = ServerService.prefixConfig(context.message.server ?: return)
                val usedPrefix = prefix.customPrefix ?: prefix.defaultPrefix
                context.message.reply(
                    """The **current prefix** in this is `${usedPrefix}`.
To **change** it, type `${usedPrefix}prefix <new prefix>`, or to **reset** it, type `${usedPrefix}prefix reset`.

The **default prefix** in this server is **`${prefix.defaultPrefix}`**.
${if (prefix.usingFallbackPrefix) "\n**Note:** AutoMod has been detected in this server. As a consequence, the fallback prefix **`${ServerService.DEFAULT_PREFIX_FALLBACK}`** is being used as the default prefix in place of **`${ServerService.DEFAULT_PREFIX}`**." else ""}"""
                )
            }
        }
    }
}