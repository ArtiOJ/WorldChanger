package com.artifabrian.worldchanger

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class WorldChanger : JavaPlugin() {

    override fun onEnable() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register("switchworld", "Switch to another dimension", SwitchWorldCommand(this))
        }
    }
}
