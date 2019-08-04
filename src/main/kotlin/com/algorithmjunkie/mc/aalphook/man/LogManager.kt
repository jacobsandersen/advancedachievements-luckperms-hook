package com.algorithmjunkie.mc.aalphook.man

import com.algorithmjunkie.mc.aalphook.AALPPlugin

class LogManager(private val plugin: AALPPlugin) {
    fun log(message: String) {
        plugin.logger.info(message)
    }

    fun wrn(message: String) {
        plugin.logger.warning(message)
    }

    fun sev(message: String) {
        plugin.logger.severe(message)
    }

    fun dbg(message: String) {
        if (!plugin.settings.isDebug()) return
        log(message)
    }
}