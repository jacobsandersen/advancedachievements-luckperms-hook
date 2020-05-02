package com.algorithmjunkie.mc.aalphook.man

import com.algorithmjunkie.mc.aalphook.AALPPlugin
import com.hm.achievement.api.AdvancedAchievementsAPI
import net.luckperms.api.LuckPerms
import net.luckperms.api.context.DefaultContextKeys
import net.luckperms.api.model.group.Group
import net.luckperms.api.model.user.User
import net.luckperms.api.node.Node
import net.luckperms.api.node.NodeEqualityPredicate
import net.luckperms.api.track.Track
import org.bukkit.entity.Player

data class ApiManager(
        val achievements: AdvancedAchievementsAPI,
        val permissions: LuckPerms
) {
    fun getLpGroup(name: String?): Group? {
        if (name == null) return null
        return permissions.groupManager.getGroup(name)
    }

    fun getLpTrack(track: String): Track? {
        return permissions.trackManager.getTrack(track)
    }
}



fun Player.hasLpNode(node: String, server: String? = null, world: String? = null): Boolean {
    val predicate = if(server == null && world == null) NodeEqualityPredicate.EXACT else NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE
    return this.hasLpNode(AALPPlugin.getLpNode(node, server, world), predicate)
}

fun Player.hasLpNode(node: Node, predicate: NodeEqualityPredicate = NodeEqualityPredicate.EXACT): Boolean {
    return this.asLpUser().data().contains(node, predicate).asBoolean()
}

fun Player.hasLpGroup(group: Group, server: String? = null, world: String? = null): Boolean {
    return this.hasLpNode("group.${group.name}", server, world)
}

fun Player.minimallyHasLpGroup(group: Group, trackName: String): Boolean {
    val apis = AALPPlugin.instance.apis

    val track = apis.getLpTrack(trackName) ?: throw IllegalArgumentException("Track cannot be null")
    if (!track.containsGroup(group)) throw java.lang.IllegalArgumentException("Track does not contain group ${group.displayName}")

    if (this.hasLpGroup(group)) {
        return true
    } else {
        var position = track.getNext(group)

        while (true) {
            val nextGroup = apis.getLpGroup(position) ?: return false

            if (this.hasLpGroup(nextGroup)) {
                return true
            }

            position = track.getNext(nextGroup)
        }
    }
}

fun Player.asLpUser(): User {
    return AALPPlugin.instance.apis.permissions.userManager.getUser(this.uniqueId)!!
}