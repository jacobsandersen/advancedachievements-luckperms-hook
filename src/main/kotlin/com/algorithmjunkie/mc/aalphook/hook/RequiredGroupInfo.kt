package com.algorithmjunkie.mc.aalphook.hook

data class RequiredGroupInfo(val name: String?, val track: String?) {
    fun isApplicable(): Boolean {
        return name != null
    }

    fun isTrackBased(): Boolean {
        return track != null
    }
}