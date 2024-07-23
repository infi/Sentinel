package chat.revolt.sentinel.framework

import chat.revolt.sentinel.lookout.internals.PermissionBit
import chat.revolt.sentinel.lookout.schemas.Message

enum class Category(val friendlyName: String) {
    GENERAL("General"),
    MODERATION("Moderation"),
}

data class CommandContext(
    val args: List<String>,
    val prefix: String,
    val message: Message,
)

interface Command {
    val name: String
    val description: String
    val usage: String
    val aliases: List<String>
    val permissions: List<PermissionBit>
    val category: Category
    suspend fun execute(context: CommandContext)
}