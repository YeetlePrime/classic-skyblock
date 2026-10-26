package dev.yeetle.classicskyblock.locations.api;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.yeetle.classicskyblock.locations.domain.TeleportationService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;

public final class JoinCommandHandler {
	private static final String OWNER_ARGUMENT = "owner";

	private final TeleportationService teleportationService;

	public JoinCommandHandler(TeleportationService teleportationService) {
		this.teleportationService = teleportationService;
	}

	public LiteralArgumentBuilder<CommandSourceStack> build() {
		return Commands.literal("join")
			.then(Commands.argument(OWNER_ARGUMENT, StringArgumentType.word())
				.suggests((context, builder) -> {
					String typed = builder.getRemaining().toLowerCase();

					teleportationService.getJoinableIslandOwners().stream()
						.filter(name -> name.toLowerCase().startsWith(typed))
						.forEach(builder::suggest);

					return builder.buildFuture();
				})
				.executes(context -> {
					String owner = StringArgumentType.getString(context, OWNER_ARGUMENT);
					teleportationService.joinIsland((Player) context.getSource().getExecutor(), owner);

					return Command.SINGLE_SUCCESS;
				}));
	}
}
