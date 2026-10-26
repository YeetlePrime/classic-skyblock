package dev.yeetle.classicskyblock.islandmanagement.api;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.yeetle.classicskyblock.islandmanagement.domain.IslandService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

public final class IslandCreationCommandHandler {
	private final Logger log;
	private final IslandService islandService;

	public IslandCreationCommandHandler(Logger logger, IslandService islandService) {
		this.log = logger;
		this.islandService = islandService;
	}

	public LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("regenerate")
			.executes(context -> {
				regenerate((Player) context.getSource().getExecutor());

				return Command.SINGLE_SUCCESS;
			});
	}

	private void regenerate(Player player) {
		World world = islandService.regenerateIslandForPlayer(player);

		if (world != null) {
			player.teleport(world.getSpawnLocation());
			player.sendMessage("Your island was regenerated.");
			log.info("The island of player %s was regenerated.".formatted(player.getName()));
		}
	}
}
