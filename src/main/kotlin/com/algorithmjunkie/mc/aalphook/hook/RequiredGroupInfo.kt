package com.algorithmjunkie.mc.aalphook.hook

import net.luckperms.api.node.Node

data class RequiredGroupInfo(val node: Node?, val track: String?) {
    public val name: String?
        get() {
            if (isApplicable()) {
                return node!!.key.split('.')[1]
            }
            return null
        }

    fun isApplicable(): Boolean {
        return node != null
    }

    fun isTrackBased(): Boolean {
        return track != null
    }
}