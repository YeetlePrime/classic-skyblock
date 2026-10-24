package com.yeetle.skyblock;

import com.yeetle.skyblock.commands.SkyblockCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyblockPlugin extends JavaPlugin implements Listener {
	private final SaveDataManager saveDataManager = new SaveDataManager(this);
	private final IslandService islandService = new IslandService(getLogger(), getSaveDataManager());

	@Override
	public void onEnable() {
		try {
			saveDataManager.load();
		} catch (RuntimeException e) {
			getLogger().severe("Could not load save data, disabling plugin to avoid overwriting it: " + e.getMessage());
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		getServer().getPluginManager().registerEvents(new PlayerLocationHandler(getLogger(), getPlayerConfig(), getIslandLoader()), this);
		getServer().getPluginManager().registerEvents(new IslandCreationHandler(getLogger(), getPlayerConfig(), getIslandLoader()), this);
		getServer().getPluginManager().registerEvents(new IslandRespawnHandler(getPlayerConfig(), getIslandLoader()), this);
		getServer().getPluginManager().registerEvents(this, this);

		SkyblockCommand command = new SkyblockCommand(getLogger(), saveDataManager, islandService);
		getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
			event.registrar().register(command.build(), "Skyblock commands"));
	}

	@Override
	public void onDisable() {
		saveDataManager.save();
	}

	@Override
	public String namespace() {
		return Globals.NAMESPACE;
	}

	public SaveDataManager getPlayerConfig() {
		return saveDataManager;
	}

	public IslandService getIslandLoader() {
		return islandService;
	}

	public SaveDataManager getSaveDataManager() {
		return saveDataManager;
	}
}
