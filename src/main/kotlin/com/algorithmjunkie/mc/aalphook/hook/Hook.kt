package com.algorithmjunkie.mc.aalphook.hook

import com.algorithmjunkie.mc.aalphook.conf.HookConfig

data class Hook(
        val requiredGroup: RequiredGroupInfo,
        val requiredAchievements: List<String>,
        val thenApplyActions: Map<HookActionType, String>
)