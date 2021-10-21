package me.kmatias.signesp

import com.lambda.client.plugin.api.Plugin

internal object Main: Plugin() {
    override fun onLoad() {
        modules.add(SignESP)
    }
}