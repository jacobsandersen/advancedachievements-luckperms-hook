package com.algorithmjunkie.mc.aalphook.hook

import java.util.*

data class Hook(
        val name: String,
        val requiredGroup: RequiredGroupInfo,
        val requiredAchievements: List<String>,
        val thenApplyActions: Map<HookActionType, String>,
        val thenSendMessages: LinkedList<String>
)