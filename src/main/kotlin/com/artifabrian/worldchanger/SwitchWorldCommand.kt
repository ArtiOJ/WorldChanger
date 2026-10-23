package com.artifabrian.worldchanger

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class SwitchWorldCommand(private val plugin: WorldChanger) : BasicCommand {

    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val player = source.executor as? Player ?: source.sender as? Player
        if (player == null) {
            source.sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED))
            return
        }
        if (args.size != 1) {
            player.sendMessage(Component.text("Usage: /switchworld <dimension>", NamedTextColor.RED))
            return
        }
        val name = args[0].lowercase()
        if (name !in availableDimensions()) {
            player.sendMessage(Component.text("Unknown dimension: $name", NamedTextColor.RED))
            return
        }
        val world = loadWorld(NamespacedKey.minecraft(name))
        if (world == null) {
            player.sendMessage(Component.text("Could not load dimension: $name", NamedTextColor.RED))
            return
        }
        if (player.world == world) {
            player.sendMessage(Component.text("You are already in $name.", NamedTextColor.RED))
            return
        }
        val from = player.location
        player.teleportAsync(lastLocation(player, world) ?: world.spawnLocation).thenAccept { success ->
            if (success) {
                saveLocation(player, from)
                player.sendMessage(Component.text("Switched to $name.", NamedTextColor.GREEN))
            } else {
                player.sendMessage(Component.text("Could not switch to $name.", NamedTextColor.RED))
            }
        }
    }

    override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
        if (args.size > 1) return emptyList()
        val prefix = args.firstOrNull()?.lowercase().orEmpty()
        return availableDimensions().filter { it.startsWith(prefix) }
    }

    override fun permission(): String = "worldchanger.switch"

    private fun locationKey(world: World): NamespacedKey =
        NamespacedKey(plugin, "last_location/${world.key.namespace}/${world.key.key}")

    private fun saveLocation(player: Player, location: Location) {
        player.persistentDataContainer.set(
            locationKey(location.world),
            PersistentDataType.LIST.doubles(),
            listOf(location.x, location.y, location.z, location.yaw.toDouble(), location.pitch.toDouble())
        )
    }

    private fun lastLocation(player: Player, world: World): Location? {
        val values = player.persistentDataContainer.get(locationKey(world), PersistentDataType.LIST.doubles())
        if (values == null || values.size != 5) return null
        return Location(world, values[0], values[1], values[2], values[3].toFloat(), values[4].toFloat())
    }

    private fun loadWorld(key: NamespacedKey): World? =
        plugin.server.getWorld(key) ?: WorldCreator.ofKey(key).createWorld()

    private fun dimensionsFolder(): Path? = plugin.server.worlds.first().worldPath.parent

    private fun availableDimensions(): List<String> {
        val folder = dimensionsFolder()
        if (folder == null || !folder.isDirectory()) return emptyList()
        return Files.list(folder).use { paths ->
            paths.filter { it.isDirectory() && it.resolve("region").isDirectory() && NamespacedKey.fromString(it.name) != null }
                .map { it.name }
                .sorted()
                .toList()
        }
    }
}
