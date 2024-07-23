package chat.revolt.sentinel.framework

import chat.revolt.sentinel.commands.PrefixCommand
import chat.revolt.sentinel.commands.HelpCommand
import chat.revolt.sentinel.database.utils.ServerService
import chat.revolt.sentinel.extensions.respond
import chat.revolt.sentinel.lookout.RevoltAPI
import chat.revolt.sentinel.lookout.schemas.Message
import org.jetbrains.exposed.sql.Database
import java.util.regex.Pattern

private val WHITESPACE_MULTI_PATTERN: Pattern = Pattern.compile("\\s+")

object SentinelFramework {
    private lateinit var database: Database
    fun init(database: Database) {
        this.database = database
    }

    val commands = listOf(
        HelpCommand(),
        PrefixCommand()
    )

    suspend fun onMessage(message: Message) {
        val channel = message.channel ?: return
        val author = message.author ?: return

        if (author == RevoltAPI.selfId)
            return

        val server = RevoltAPI.channelCache[channel]?.server
        if (server == null) {
            message.respond(":wave: Hello! My name is Sentinel. I'm always here to assist you in all servers that I'm in. Type `/help` in a server to see what I can do!")
            return
        }

        message.content?.let {
            val prefix = ServerService.prefixFor(server)
            if (!it.startsWith(prefix)) return

            var args = it.substring(prefix.length).split(WHITESPACE_MULTI_PATTERN)
            val commandName = args.first()
            args = args.drop(1)

            val command = commands.find { cmd -> cmd.name == commandName }
                ?: commands.find { cmd -> cmd.aliases.contains(commandName) }
                ?: return
            command.execute(CommandContext(args, prefix, message))
        }
    }
}