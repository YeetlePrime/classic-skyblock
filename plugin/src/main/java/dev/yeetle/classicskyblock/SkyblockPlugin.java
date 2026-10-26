package dev.yeetle.classicskyblock;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.yeetle.classicskyblock.common.persistence.SaveDataFileManager;
import dev.yeetle.classicskyblock.common.valueobjects.Globals;
import dev.yeetle.classicskyblock.islandmanagement.api.IslandCreationCommandHandler;
import dev.yeetle.classicskyblock.islandmanagement.api.IslandCreationEventHandler;
import dev.yeetle.classicskyblock.islandmanagement.domain.IslandService;
import dev.yeetle.classicskyblock.islandmanagement.persistence.IslandRepository;
import dev.yeetle.classicskyblock.locations.api.IslandRespawnEventHandler;
import dev.yeetle.classicskyblock.locations.api.JoinCommandHandler;
import dev.yeetle.classicskyblock.locations.api.NetherPortalEventHandler;
import dev.yeetle.classicskyblock.locations.api.PlayerLocationEventHandler;
import dev.yeetle.classicskyblock.locations.domain.TeleportationService;
import dev.yeetle.classicskyblock.player.domain.PlayerService;
import dev.yeetle.classicskyblock.player.persistence.PlayerRepository;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The entry point of the plugin.
 * It wires all parts together and may therefore access everything.
 */
public final class SkyblockPlugin extends JavaPlugin {
	private final SaveDataFileManager saveDataFileManager = new SaveDataFileManager(getDataFolder().toPath());
	private final PlayerRepository playerRepository = new PlayerRepository(saveDataFileManager);
	private final IslandRepository islandRepository = new IslandRepository(saveDataFileManager, playerRepository);
	private final PlayerService playerService = new PlayerService(playerRepository);
	private final IslandService islandService = new IslandService(getLogger(), islandRepository);
	private final TeleportationService teleportationService = new TeleportationService(getLogger(), playerService, islandService);

	@Override
	public void onEnable() {
		try {
			saveDataFileManager.load();
		} catch (RuntimeException e) {
			getLogger().severe("Could not load save data, disabling plugin to avoid overwriting it: " + e.getMessage());
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		getServer().getPluginManager().registerEvents(new NetherPortalEventHandler(teleportationService), this);
		getServer().getPluginManager().registerEvents(new PlayerLocationEventHandler(teleportationService), this);
		getServer().getPluginManager().registerEvents(new IslandCreationEventHandler(getLogger(), islandService), this);
		getServer().getPluginManager().registerEvents(new IslandRespawnEventHandler(teleportationService), this);

		IslandCreationCommandHandler islandCreationCommandHandler = new IslandCreationCommandHandler(getLogger(), islandService);
		JoinCommandHandler joinCommandHandler = new JoinCommandHandler(teleportationService);

		LiteralCommandNode<CommandSourceStack> command = Commands.literal("skyblock")
			// Hides the command from everyone who is not a player, including the console.
			.requires(source -> source.getExecutor() instanceof Player)
			.then(islandCreationCommandHandler.build())
			.then(joinCommandHandler.build())
			.build();

		getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
			event.registrar().register(command, "Skyblock commands"));
	}

	@Override
	public void onDisable() {
		saveDataFileManager.save();
	}

	@Override
	public String namespace() {
		return Globals.NAMESPACE;
	}
}
