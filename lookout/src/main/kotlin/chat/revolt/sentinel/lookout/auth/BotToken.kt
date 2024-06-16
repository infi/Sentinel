package chat.revolt.sentinel.lookout.auth

class BotToken(private val token: String) : LoginStrategy {
    override suspend fun receiveToken(): String {
        return token
    }

    override fun loginHeaderName(): String {
        return "x-bot-token"
    }
}