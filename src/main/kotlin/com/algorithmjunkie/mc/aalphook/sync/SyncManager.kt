package com.algorithmjunkie.mc.aalphook.sync

import com.algorithmjunkie.mc.aalphook.AALPPlugin
import com.algorithmjunkie.mc.aalphook.hook.HookActionType
import me.lucko.luckperms.api.DataMutateResult
import me.lucko.luckperms.api.Node
import me.lucko.luckperms.api.User
import org.bukkit.entity.Player

class SyncManager(private val plugin: AALPPlugin) {
    fun synchronize(player: Player) {
        val playerDisplayName = player.displayName

        val lpUser = plugin.apis.permissions.userManager.getUser(player.uniqueId)
        if (lpUser == null) {
            plugin.log.dbg("Failed to load LP User Object for player $playerDisplayName")
            return
        }

        plugin.log.dbg("Running hooks for player $playerDisplayName")
        for (hook in plugin.hooks.loadedHooks) {
            plugin.log.dbg("Running hook $hook for $playerDisplayName")

            val requiredGroup = hook.requiredGroup
            if (requiredGroup.isApplicable()) {
                val lpGroup = plugin.apis.permissions.getGroup(requiredGroup.name!!)
                if (lpGroup == null) {
                    plugin.log.dbg("Failed to load LP Group Object for group ${requiredGroup.name}; hook will be skipped!")
                    continue
                }

                if (requiredGroup.isTrackBased()) {
                    plugin.log.dbg("Hook $hook minimally requires LP group ${requiredGroup.name} and is based on track ${requiredGroup.track}")

                    plugin.log.dbg("Checking if player $playerDisplayName minimally has group or higher group in track")

                    if (!plugin.apis.playerMinimallyHasLpGroupInTrack(lpUser, lpGroup, requiredGroup.track!!)) {
                        plugin.log.dbg("Player $playerDisplayName did not have minimally required group or any higher group in track")
                        continue
                    }

                    plugin.log.dbg("Player $playerDisplayName either had minimally required group or higher group in track; hook processing will proceed")
                } else {
                    plugin.log.dbg("Hook $hook strictly requires LP group ${requiredGroup.name} and is not track based")

                    plugin.log.dbg("Checking if player $playerDisplayName has required group for hook $hook")

                    if (!plugin.apis.playerHasLpGroup(lpUser, lpGroup)) {
                        plugin.log.dbg("Player $playerDisplayName did not have the required group for $hook")
                        continue
                    }

                    plugin.log.dbg("Player $playerDisplayName did have the required group for $hook; hook processing will proceed.")
                }

            }

            plugin.log.dbg("Checking required achievement criteria for hook $hook")
            val meetsRequiredAchievementCriteria = hook.requiredAchievements.all {
                plugin.log.dbg("Checking $playerDisplayName has completed achievement $it")
                val completed = plugin.apis.achievements.hasPlayerReceivedAchievement(player.uniqueId, it)
                plugin.log.dbg("User has ${if (completed) "" else "not"} completed achievement $it")
                completed
            }

            if (meetsRequiredAchievementCriteria) {
                plugin.log.dbg("Hook $hook achievement criteria was met by $playerDisplayName; hook processing will proceed.")

                for (action in hook.thenApplyActions) {
                    plugin.log.dbg("Running $hook action $action for $playerDisplayName")

                    val target = action.value

                    val result = when (action.key) {
                        HookActionType.ADDGROUP -> doAddGroup(lpUser, target)
                        HookActionType.DELGROUP -> doDelGroup(lpUser, target)
                        HookActionType.ADDPERM -> doAddPerm(lpUser, target)
                        HookActionType.DELPERM -> doDelPerm(lpUser, target)
                    }

                    plugin.log.dbg("Processing result for hook $hook action $action")
                    when (result.first) {
                        true -> {
                            when (result.second) {
                                "added" -> plugin.log.log("${player.displayName} was added to group or permission $target")
                                "removed" -> plugin.log.log("${player.displayName} was removed from group or permission $target")
                            }
                        }

                        false -> {
                            when (result.second) {
                                "in_group" -> plugin.log.wrn("LuckPerms reported user ${player.displayName} was already in group $target")
                                "not_in_group" -> plugin.log.wrn("LuckPerms reported user ${player.displayName} was not already in group $target")
                                "unknown_err" -> plugin.log.sev("LuckPerms reported an unknown error with the previously requested action.")
                                "not_found" -> plugin.log.sev("The target group $target could not be found.")
                            }
                        }
                    }
                }

                plugin.log.dbg("Synchronizing changes with LuckPerms...")
                plugin.apis.permissions.userManager.saveUser(lpUser)
                plugin.log.dbg("Done!")
            }
        }
    }

    private fun resolveGroup(group: String): Node? {
        val toApply = plugin.apis.permissions.getGroup(group)

        if (toApply == null) {
            plugin.log.dbg("Could not resolve LP Group Object referenced by hook!")
            return null
        }

        return plugin.apis.permissions.nodeFactory.makeGroupNode(toApply).build()
    }

    private fun doAddGroup(user: User, group: String): Pair<Boolean, String> {
        val resolved = resolveGroup(group) ?: return false to "not_found"

        if (user.hasPermission(resolved).asBoolean()) {
            return false to "in_group"
        }

        return doAddPerm(user, resolved)
    }

    private fun doAddPerm(user: User, node: String): Pair<Boolean, String> {
        return doAddPerm(user, plugin.apis.permissions.buildNode(node).build())
    }

    private fun doAddPerm(user: User, node: Node): Pair<Boolean, String> {
        val result: DataMutateResult = user.setPermission(node)
        return if (result.wasSuccess()) {
            true to "added"
        } else {
            false to "unknown_err"
        }
    }

    private fun doDelGroup(user: User, group: String): Pair<Boolean, String> {
        val resolved = resolveGroup(group) ?: return false to "not_found"

        if (!user.hasPermission(resolved).asBoolean()) {
            return false to "not_in_group"
        }

        return doDelPerm(user, resolved)
    }

    private fun doDelPerm(user: User, node: String): Pair<Boolean, String> {
        return doDelPerm(user, plugin.apis.permissions.buildNode(node).build())
    }

    private fun doDelPerm(user: User, node: Node): Pair<Boolean, String> {
        val result = user.unsetPermission(node)
        return if (result.wasSuccess()) {
            true to "removed"
        } else {
            false to "unknown_err"
        }
    }
}