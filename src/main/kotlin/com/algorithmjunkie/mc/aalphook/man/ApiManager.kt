package com.algorithmjunkie.mc.aalphook.man

import com.hm.achievement.api.AdvancedAchievementsAPI
import me.lucko.luckperms.api.*

data class ApiManager(
        val achievements: AdvancedAchievementsAPI,
        val permissions: LuckPermsApi
) {
    fun getLpGroup(name: String?): Group? {
        if (name == null) return null
        return permissions.groupManager.getGroup(name)
    }

    fun getLpGroupNode(group: Group): Node  {
        return permissions.nodeFactory.makeGroupNode(group).build()
    }

    fun getLpTrack(track: String): Track? {
        return permissions.trackManager.getTrack(track)
    }

    fun playerHasLpGroup(lpUser: User, group: Group): Boolean {
        return lpUser.hasPermission(getLpGroupNode(group)).asBoolean()
    }

    fun playerMinimallyHasLpGroupInTrack(lpUser: User, group: Group, trackName: String): Boolean {
        val track = getLpTrack(trackName) ?: throw IllegalArgumentException("Track cannot be null")
        if (!track.containsGroup(group)) throw java.lang.IllegalArgumentException("Track does not contain group ${group.displayName}")

        if (!playerHasLpGroup(lpUser, group)) return false
        while (true) {
            val nextGroup = getLpGroup(track.getNext(group)) ?: break
            if (playerHasLpGroup(lpUser, nextGroup)) {
                return true
            }
        }

        return false
    }
}