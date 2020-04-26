package com.algorithmjunkie.mc.aalphook.man

import com.algorithmjunkie.mc.aalphook.AALPPlugin
import com.algorithmjunkie.mc.aalphook.conf.HookConfig
import com.algorithmjunkie.mc.aalphook.hook.Hook

class HookManager(private val plugin: AALPPlugin) {
    internal var loadedHooks: Set<Hook> = plugin.config.getHooks()
}