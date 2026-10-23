package com.artifabrian.worldchanger

import org.bukkit.plugin.java.JavaPlugin

class WorldChanger : JavaPlugin() {

    override fun onEnable() {
        logger.info("WorldChanger enabled")
    }

    override fun onDisable() {
        logger.info("WorldChanger disabled")
    }
}
