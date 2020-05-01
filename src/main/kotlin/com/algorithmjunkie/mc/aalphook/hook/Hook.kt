package com.algorithmjunkie.mc.aalphook.hook

import java.util.*

data class Hook(
        val name: String,
        val requiredGroup: RequiredGroupInfo,
        val requiredAchievements: List<String>,
        val thenApplyActions: List<Action>,
        val thenSendMessages: LinkedList<String>
)