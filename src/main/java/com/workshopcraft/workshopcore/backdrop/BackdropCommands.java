package com.workshopcraft.workshopcore.backdrop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.workshopcraft.workshopcore.WorkshopCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;

public final class BackdropCommands {
    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            id -> Component.literal("Unknown backdrop " + id)
    );

    private BackdropCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(WorkshopCore.MOD_ID)
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("backdrop")
                        .then(Commands.literal("get").executes(context -> get(context.getSource())))
                        .then(Commands.literal("clear")
                                .executes(context -> clear(context.getSource(), OptionalTicks.UNSET))
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, BackdropTransition.MAX_DURATION_TICKS))
                                        .executes(context -> clear(context.getSource(), IntegerArgumentType.getInteger(context, "ticks")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", IdentifierArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(Backdrops.ids(), builder))
                                        .executes(context -> set(context.getSource(), IdentifierArgument.getId(context, "id"), OptionalTicks.UNSET))
                                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, BackdropTransition.MAX_DURATION_TICKS))
                                                .executes(context -> set(context.getSource(), IdentifierArgument.getId(context, "id"), IntegerArgumentType.getInteger(context, "ticks"))))))));
    }

    private static int get(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        Backdrops.get(level).ifPresentOrElse(
                id -> source.sendSuccess(() -> Component.literal("Backdrop for " + level.dimension().identifier() + " is " + id), false),
                () -> source.sendSuccess(() -> Component.literal("Backdrop for " + level.dimension().identifier() + " is vanilla"), false)
        );
        return 1;
    }

    private static int clear(CommandSourceStack source, int ticks) {
        ServerLevel level = source.getLevel();
        if (ticks == OptionalTicks.UNSET) {
            Backdrops.clear(level);
        } else {
            Backdrops.clear(level, ticks);
        }
        source.sendSuccess(() -> Component.literal("Cleared backdrop for " + level.dimension().identifier()), true);
        return 1;
    }

    private static int set(CommandSourceStack source, Identifier id, int ticks) throws CommandSyntaxException {
        if (!Backdrops.exists(id)) {
            throw UNKNOWN.create(id);
        }
        ServerLevel level = source.getLevel();
        if (ticks == OptionalTicks.UNSET) {
            Backdrops.set(level, id);
        } else {
            Backdrops.set(level, id, ticks);
        }
        source.sendSuccess(() -> Component.literal("Set backdrop for " + level.dimension().identifier() + " to " + id), true);
        return 1;
    }

    private static final class OptionalTicks {
        private static final int UNSET = Integer.MIN_VALUE;

        private OptionalTicks() {
        }
    }
}
