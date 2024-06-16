package chat.revolt.sentinel.lookout.auth

interface LoginStrategy {
    suspend fun receiveToken(): String
    fun loginHeaderName(): String
}