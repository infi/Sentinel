package chat.revolt.sentinel.lookout.internals

import chat.revolt.sentinel.lookout.routes.server.fetchMembers
import chat.revolt.sentinel.lookout.schemas.Member

class Members {
    // memberCache (mapping of serverId to userId to member)
    private val memberCache = mutableMapOf<String, MutableMap<String, Member>>()

    suspend fun getMembersForServer(serverId: String): List<Member> {
        val hasMembers = memberCache.containsKey(serverId)
        if (!hasMembers) {
            val response = fetchMembers(serverId, includeOffline = true, pure = false)
            // We now also have them cached for future use (pure = false)
            // So its fine to only return them here
            return response.members
        }
        return memberCache[serverId]?.values?.toList() ?: emptyList()
    }

    fun getMember(serverId: String, userId: String): Member? {
        return memberCache[serverId]?.get(userId)
    }

    fun hasMember(serverId: String, userId: String): Boolean {
        return memberCache[serverId]?.containsKey(userId) ?: false
    }

    fun setMember(serverId: String, member: Member) {
        if (!memberCache.containsKey(serverId)) {
            memberCache[serverId] = mutableMapOf()
        }

        memberCache[serverId]?.set(member.id!!.user, member)
    }

    fun removeMember(serverId: String, userId: String) {
        memberCache[serverId]?.remove(userId)
    }

    fun clear() {
        memberCache.clear()
    }

    /**
     * Returns a Map of userId to server-nickname for the given serverId.
     */
    fun markdownMemberMapFor(serverId: String): Map<String, String> {
        return memberCache[serverId]?.mapNotNull { (userId, member) ->
            member.nickname?.let { userId to member.nickname }
        }?.toMap() ?: emptyMap()
    }

    fun filterNamesFor(serverId: String, query: String): List<Member> {
        return memberCache[serverId]?.values?.filter { member ->
            member.nickname?.contains(query, ignoreCase = true) ?: false
        } ?: emptyList()
    }
}