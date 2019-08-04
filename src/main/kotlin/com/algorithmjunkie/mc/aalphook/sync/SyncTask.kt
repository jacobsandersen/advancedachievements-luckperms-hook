package com.algorithmjunkie.mc.aalphook.sync

import com.algorithmjunkie.mc.aalphook.AALPPlugin

class SyncTask(private val plugin: AALPPlugin) : Runnable {
    override fun run() {
        val start = System.currentTimeMillis()
        plugin.log.dbg("Running automatic synchronization task...")
        for (player in plugin.server.onlinePlayers) {
            plugin.sync.synchronize(player)
        }
        val elapsed = System.currentTimeMillis() - start
        plugin.log.dbg("Synchronization task has finished in $elapsed milliseconds.")
    }
}