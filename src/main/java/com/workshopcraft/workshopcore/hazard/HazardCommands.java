package com.workshopcraft.workshopcore.hazard;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.workshopcraft.workshopcore.WorkshopCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class HazardCommands {
    private static final DynamicCommandExceptionType BAD_COLOR = new DynamicCommandExceptionType(
            value -> Component.literal("Invalid colour '" + value + "', expected #RRGGBB or #AARRGGBB")
    );

    private HazardCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(WorkshopCore.MOD_ID)
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("haze")
                        .then(Commands.literal("clear")
                                .executes(context -> clear(context.getSource(), 20))
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, Haze.MAX_FADE_TICKS))
                                        .executes(context -> clear(context.getSource(), IntegerArgumentType.getInteger(context, "ticks")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("color", StringArgumentType.word())
                                        .then(Commands.argument("density", FloatArgumentType.floatArg(0.0F, 1.0F))
                                                .executes(context -> set(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "color"),
                                                        FloatArgumentType.getFloat(context, "density"),
                                                        20))
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, Haze.MAX_FADE_TICKS))
                                                        .executes(context -> set(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "color"),
                                                                FloatArgumentType.getFloat(context, "density"),
                                                                IntegerArgumentType.getInteger(context, "ticks"))))))))
                .then(Commands.literal("hazard")
                        .then(Commands.literal("protected")
                                .then(Commands.argument("hazard", IdentifierArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(HazardProtection.hazards(), builder))
                                        .executes(context -> checkProtected(context.getSource(), IdentifierArgument.getId(context, "hazard")))))
                        .then(Commands.literal("tick")
                                .then(Commands.argument("hazard", IdentifierArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(HazardProtection.hazards(), builder))
                                        .executes(context -> tick(context.getSource(), IdentifierArgument.getId(context, "hazard")))))
                        .then(Commands.literal("hurt")
                                .then(Commands.argument("hazard", IdentifierArgument.id())
                                        .executes(context -> hurt(context.getSource(), IdentifierArgument.getId(context, "hazard"), 1.0F))
                                        .then(Commands.argument("amount", FloatArgumentType.floatArg(0.0F))
                                                .executes(context -> hurt(
                                                        context.getSource(),
                                                        IdentifierArgument.getId(context, "hazard"),
                                                        FloatArgumentType.getFloat(context, "amount"))))))));
    }

    private static int set(CommandSourceStack source, String color, float density, int ticks) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int argb = parseColor(color);
        Haze.set(player, argb, density, ticks);
        source.sendSuccess(() -> Component.literal("Set haze to " + formatColor(argb) + " density " + density + " over " + ticks + " ticks"), true);
        return 1;
    }

    private static int clear(CommandSourceStack source, int ticks) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Haze.clear(player, ticks);
        source.sendSuccess(() -> Component.literal("Cleared haze over " + ticks + " ticks"), true);
        return 1;
    }

    private static int checkProtected(CommandSourceStack source, Identifier hazard) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean protectedNow = HazardProtection.isProtected(player, hazard);
        source.sendSuccess(() -> Component.literal(
                (protectedNow ? "Protected" : "Unprotected") + " against " + hazard + helmetSummary(player)), false);
        return protectedNow ? 1 : 0;
    }

    private static int tick(CommandSourceStack source, Identifier hazard) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack before = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        boolean protectedNow = HazardProtection.tick(player, hazard);
        ItemStack after = player.getItemBySlot(EquipmentSlot.HEAD);
        String wear = describeWear(before, after);
        source.sendSuccess(() -> Component.literal(
                (protectedNow ? "Tick protected" : "Tick unprotected") + " against " + hazard + wear), false);
        return protectedNow ? 1 : 0;
    }

    private static int hurt(CommandSourceStack source, Identifier hazard, float amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        float health = player.getHealth();
        boolean applied = HazardDamage.hurt(player, HazardDamage.key(hazard), amount);
        float after = player.getHealth();
        source.sendSuccess(() -> Component.literal(
                (applied ? "Applied " : "Did not apply ") + amount + " " + hazard + " damage (health " + health + " -> " + after + ")"), true);
        return applied ? 1 : 0;
    }

    private static String helmetSummary(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) {
            return "; head is empty";
        }
        return "; head is " + helmet.getItem().getName(helmet).getString() + " damage " + helmet.getDamageValue() + "/" + helmet.getMaxDamage();
    }

    private static String describeWear(ItemStack before, ItemStack after) {
        if (before.isEmpty() && after.isEmpty()) {
            return "; head is empty";
        }
        if (!before.isEmpty() && after.isEmpty()) {
            return "; head item broke";
        }
        if (before.isEmpty()) {
            return helmetSummaryFrom(after);
        }
        int delta = after.getDamageValue() - before.getDamageValue();
        return "; head damage " + before.getDamageValue() + " -> " + after.getDamageValue()
                + (delta != 0 ? " (" + (delta > 0 ? "+" : "") + delta + ")" : " (no wear this tick)");
    }

    private static String helmetSummaryFrom(ItemStack helmet) {
        return "; head is " + helmet.getItem().getName(helmet).getString() + " damage " + helmet.getDamageValue() + "/" + helmet.getMaxDamage();
    }

    private static int parseColor(String raw) throws CommandSyntaxException {
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }
        if (hex.length() != 6 && hex.length() != 8) {
            throw BAD_COLOR.create(raw);
        }
        try {
            long value = Long.parseLong(hex, 16);
            if (hex.length() == 6) {
                return (int) (0xFF000000L | value);
            }
            return (int) value;
        } catch (NumberFormatException exception) {
            throw BAD_COLOR.create(raw);
        }
    }

    private static String formatColor(int argb) {
        return String.format("#%08X", argb);
    }
}
