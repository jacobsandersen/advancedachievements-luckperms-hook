package com.algorithmjunkie.mc.aalphook.hook

import java.util.*

class Action(
    val actionType: HookActionType,
    val target: String,
    val server: String?,
    val world: String?
)