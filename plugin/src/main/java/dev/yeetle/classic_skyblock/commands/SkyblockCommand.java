package dev.yeetle.classic_skyblock.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.yeetle.classic_skyblock.IslandService;
import dev.yeetle.classic_skyblock.SaveDataManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.logging.Logger;

/**
 * Builds the command tree of <code>/skyblock</code>.
 * The tree is registered by the plugin in the <code>COMMANDS</code> lifecycle event.
 */
public final class SkyblockCommand {
	private static final String OWNER_ARGUMENT = "owner";

	private final Logger log;
	private final SaveDataManager saveDataManager;
	private final IslandService islandService;

	public SkyblockCommand(Logger logger, SaveDataManager saveDataManager, IslandService islandService) {
		this.log = logger;
		this.saveDataManager = saveDataManager;
		this.islandService = islandService;
	}

	public LiteralCommandNode<CommandSourceStack> build() {
		return Commands.literal("skyblock")
			// Hides the command from everyone who is not a player, including the console.
			.requires(source -> source.getExecutor() instanceof Player)
			.then(Commands.literal("regenerate")
				.executes(context -> {
					regenerate((Player) context.getSource().getExecutor());

					return Command.SINGLE_SUCCESS;
				}))
			.then(Commands.literal("join")
				.then(Commands.argument(OWNER_ARGUMENT, StringArgumentType.word())
					.suggests((context, builder) -> {
						String typed = builder.getRemaining().toLowerCase();

						saveDataManager.getIslandOwnerNames().stream()
							.filter(name -> name.toLowerCase().startsWith(typed))
							.forEach(builder::suggest);

						return builder.buildFuture();
					})
					.executes(context -> {
						String owner = StringArgumentType.getString(context, OWNER_ARGUMENT);
						islandService.joinIsland((Player) context.getSource().getExecutor(), owner);

						return Command.SINGLE_SUCCESS;
					})))
			.build();
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
