package com.harryskingdom.bloodlines.race;

import com.harryskingdom.bloodlines.BloodlinesMod;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = BloodlinesMod.MODID)
public class RaceCommands
{
    private static final SimpleCommandExceptionType ALREADY_CHOSEN =
            new SimpleCommandExceptionType(Component.literal("You have already chosen your bloodline."));
    private static final SimpleCommandExceptionType UNKNOWN_RACE =
            new SimpleCommandExceptionType(Component.literal("Unknown race."));
    private static final SimpleCommandExceptionType LOCKED_RACE =
            new SimpleCommandExceptionType(Component.literal("That bloodline has not been unlocked yet."));

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("race")
                .then(Commands.literal("select")
                        .then(Commands.argument("race", StringArgumentType.word())
                                .suggests(RaceCommands::suggestRaces)
                                .executes(RaceCommands::selectRace)))
                .then(Commands.literal("info")
                        .executes(RaceCommands::showInfo))
                .then(Commands.literal("admin")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("race", StringArgumentType.word())
                                                .suggests(RaceCommands::suggestRaces)
                                                .executes(RaceCommands::adminUnlock))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(RaceCommands::adminReset)))));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestRaces(
            CommandContext<CommandSourceStack> ctx, com.mojang.brigadier.suggestion.SuggestionsBuilder builder)
    {
        return SharedSuggestionProvider.suggest(Arrays.stream(Race.values()).map(r -> r.name().toLowerCase(Locale.ROOT)), builder);
    }

    private static Race parseRace(String input) throws CommandSyntaxException
    {
        try
        {
            return Race.valueOf(input.toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e)
        {
            throw UNKNOWN_RACE.create();
        }
    }

    private static int selectRace(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Race race = parseRace(StringArgumentType.getString(ctx, "race"));

        RaceSelection.Result result = RaceSelection.select(player, race);
        if (result == RaceSelection.Result.ALREADY_CHOSEN)
            throw ALREADY_CHOSEN.create();
        if (result == RaceSelection.Result.LOCKED)
            throw LOCKED_RACE.create();

        ctx.getSource().sendSuccess(() -> Component.literal("You have chosen the bloodline of ")
                .append(Component.literal(race.getDisplayName()).withStyle(ChatFormatting.GOLD))
                .append(Component.literal(".")), true);
        return 1;
    }

    private static int showInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        IPlayerRace data = PlayerRaceCapability.get(player).orElseThrow(() -> new IllegalStateException("Player race capability missing"));

        String current = data.getRace().map(Race::getDisplayName).orElse("None chosen yet");
        ctx.getSource().sendSuccess(() -> Component.literal("Bloodline: " + current), false);
        return 1;
    }

    private static int adminUnlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        Race race = parseRace(StringArgumentType.getString(ctx, "race"));

        PlayerRaceCapability.get(target).ifPresent(data -> data.unlockRace(race));
        ctx.getSource().sendSuccess(() -> Component.literal("Unlocked " + race.getDisplayName() + " for " + target.getName().getString()), true);
        return 1;
    }

    private static int adminReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        PlayerRaceCapability.get(target).ifPresent(data -> data.setRace(null));
        RaceEffects.clear(target);
        ctx.getSource().sendSuccess(() -> Component.literal("Reset bloodline for " + target.getName().getString()), true);
        return 1;
    }
}
