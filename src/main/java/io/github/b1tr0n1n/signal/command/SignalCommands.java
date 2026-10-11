package io.github.b1tr0n1n.signal.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.b1tr0n1n.signal.campaign.SignalCampaignData;
import io.github.b1tr0n1n.signal.domain.BellwetherBus;
import io.github.b1tr0n1n.signal.domain.BellwetherSite;
import io.github.b1tr0n1n.signal.domain.Service;
import io.github.b1tr0n1n.signal.registry.ModItems;
import io.github.b1tr0n1n.signal.scenario.BellwetherScenario;
import io.github.b1tr0n1n.signal.scenario.ScenarioGuard;
import io.github.b1tr0n1n.signal.scenario.ScenarioService;
import io.github.b1tr0n1n.signal.scenario.ServiceFacility;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;


/**
 * {@code /signal scenario create bellwether [<anchor>]}, {@code /signal scenario status},
 * {@code /signal scenario reset bellwether confirm}. Operator-only (permission level 2). Nothing here runs
 * automatically: an ordinary world is unchanged until an operator explicitly creates the scenario.
 */
public final class SignalCommands {
    private SignalCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("signal")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("scenario")
                        .then(Commands.literal("create")
                                .then(Commands.literal(BellwetherScenario.ID)
                                        .executes(c -> create(c.getSource(), null))
                                        .then(Commands.argument("anchor", BlockPosArgument.blockPos())
                                                .executes(c -> create(c.getSource(), BlockPosArgument.getSpawnablePos(c, "anchor"))))))
                        .then(Commands.literal("status").executes(c -> status(c.getSource())))
                        .then(Commands.literal("reset")
                                .then(Commands.literal(BellwetherScenario.ID)
                                        .executes(c -> resetWarning(c.getSource()))
                                        .then(Commands.literal("confirm").executes(c -> reset(c.getSource())))))));
    }

    private static int create(CommandSourceStack src, @Nullable BlockPos anchorArg) {
        ServerLevel level = src.getLevel();
        SignalCampaignData data = SignalCampaignData.get(src.getServer());
        BlockPos anchor = anchorArg != null ? anchorArg : BlockPos.containing(src.getPosition());
        ServerPlayer player = src.getEntity() instanceof ServerPlayer p ? p : null;
        src.sendSuccess(() -> Component.translatable("command.signal.scenario.surveying").withStyle(ChatFormatting.GRAY), false);
        ScenarioService.CreateResult r = ScenarioService.create(level, anchor, player == null ? null : player.getUUID());
        switch (r.status()) {
            case UNREADABLE_DATA -> {
                src.sendFailure(Component.literal(String.valueOf(data.loadError())));
                return 0;
            }
            case ALREADY_EXISTS -> {
                src.sendFailure(Component.translatable("command.signal.scenario.exists", fmt(data.anchor())));
                return 0;
            }
            case BLOCKED -> {
                BoundingBox box = r.bounds();
                ScenarioGuard.Report g = r.guard();
                src.sendFailure(Component.translatable(r.problemKey(), g == null ? 0 : g.occupied(),
                        g == null || g.first() == null ? "-" : fmt(g.first()) + " (" + g.firstWhat() + ")",
                        box == null ? "-" : fmt(new BlockPos(box.minX(), box.minY(), box.minZ())),
                        box == null ? "-" : fmt(new BlockPos(box.maxX(), box.maxY(), box.maxZ()))));
                return 0;
            }
            default -> {
            }
        }
        BoundingBox box = data.bounds();
        applyScenarioRules(src, level);
        if (player != null) {
            preparePlayer(player, level, data);
        }
        src.sendSuccess(() -> Component.translatable("command.signal.scenario.created", fmt(anchor),
                fmt(new BlockPos(box.minX(), box.minY(), box.minZ())), fmt(new BlockPos(box.maxX(), box.maxY(), box.maxZ())))
                .withStyle(ChatFormatting.GREEN), true);
        ScenarioService.begin(level, data);
        return 1;
    }

    private static int status(CommandSourceStack src) {
        SignalCampaignData data = SignalCampaignData.get(src.getServer());
        if (data.loadError() != null) {
            src.sendFailure(Component.literal(data.loadError()));
            return 0;
        }
        if (!data.isInitialized()) {
            src.sendSuccess(() -> Component.translatable("command.signal.scenario.none"), false);
            return 0;
        }
        BellwetherBus bus = data.site().bus();
        StringBuilder running = new StringBuilder();
        for (Service s : Service.values()) {
            if (bus.isRunning(s)) {
                running.append(running.isEmpty() ? "" : ",").append(s.id());
            }
        }
        String text = "SIGNAL campaign " + data.campaignId() + " (schema " + SignalCampaignData.SCHEMA_VERSION + ")\n"
                + " scenario=" + data.scenarioId() + " dimension=" + data.dimension().location() + " anchor=" + fmt(data.anchor()) + "\n"
                + " fuse=" + bus.fuse().id() + " isolated=" + bus.isolated() + " supply=" + bus.availableSupply() + "/" + BellwetherBus.GENERATOR_SUPPLY + "\n"
                + " configured=" + BellwetherSite.serviceList(bus.configured()) + " (" + bus.configuredDemand() + " units)"
                + " running=" + (running.isEmpty() ? "none" : running) + "\n"
                + " relayAligned=" + bus.relayAligned() + " relayReachable=" + bus.relayReachable() + " revision=" + bus.revision() + "\n"
                + " radio=" + data.site().radio().transcript() + "\n"
                + " historyEvents=" + data.site().history().events().size() + " pumpPressure=" + data.pumpPressure() + "\n"
                + " facility=" + fmt(data.frame().offset(27, 0, 15)) + " tower=" + towerBase(data);
        src.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    /** Tower base (walking level at its centre), from the highest relay-bound lamp: the beacon. */
    private static String towerBase(SignalCampaignData data) {
        return data.bindings().entrySet().stream()
                .filter(e -> e.getValue().equals(BellwetherSite.NODE_RELAY))
                .map(java.util.Map.Entry::getKey)
                .max(java.util.Comparator.comparingInt(BlockPos::getY))
                .map(p -> fmt(p.below(38))).orElse("-");
    }

    private static int resetWarning(CommandSourceStack src) {
        SignalCampaignData data = SignalCampaignData.get(src.getServer());
        if (!data.isActive()) {
            src.sendFailure(Component.translatable("command.signal.scenario.none"));
            return 0;
        }
        BoundingBox b = ServiceFacility.box(data.frame());
        src.sendSuccess(() -> Component.translatable("command.signal.scenario.reset_warning",
                fmt(new BlockPos(b.minX(), b.minY(), b.minZ())), fmt(new BlockPos(b.maxX(), b.maxY(), b.maxZ())))
                .withStyle(ChatFormatting.YELLOW), false);
        return 0;
    }

    /** Rebuilds strictly inside the stored bounds and starts a fresh timeline. Never touches anything else. */
    private static int reset(CommandSourceStack src) {
        SignalCampaignData data = SignalCampaignData.get(src.getServer());
        if (!data.isActive()) {
            src.sendFailure(Component.translatable("command.signal.scenario.none"));
            return 0;
        }
        ServerLevel level = data.level(src.getServer());
        if (level == null) {
            src.sendFailure(Component.literal("Campaign dimension " + data.dimension().location() + " is not available."));
            return 0;
        }
        BoundingBox box = ServiceFacility.box(data.frame());
        if (!level.hasChunksAt(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            src.sendFailure(Component.translatable("command.signal.scenario.not_loaded", 0, "-",
                    fmt(new BlockPos(box.minX(), box.minY(), box.minZ())), fmt(new BlockPos(box.maxX(), box.maxY(), box.maxZ()))));
            return 0;
        }
        ServerPlayer player = src.getEntity() instanceof ServerPlayer p ? p : null;
        ScenarioService.reset(level, data, player == null ? null : player.getUUID());
        if (player != null) {
            preparePlayer(player, level, data);
        }
        src.sendSuccess(() -> Component.translatable("command.signal.scenario.reset_done").withStyle(ChatFormatting.GREEN), true);
        ScenarioService.begin(level, data);
        return 1;
    }

    private static void applyScenarioRules(CommandSourceStack src, ServerLevel level) {
        // Weather is per dimension (Overworld only). Hostile mobs are kept out of the site by
        // CampaignEvents#onSpawnPosition; the world's own spawning (wildlife included) is left alone.
        boolean weather = level.dimension() == Level.OVERWORLD;
        if (weather) {
            level.setWeatherParameters(0, 6000, true, false);
        }
        src.sendSuccess(() -> Component.translatable(weather ? "command.signal.scenario.rules_weather" : "command.signal.scenario.rules")
                .withStyle(ChatFormatting.GRAY), false);
    }

    private static void preparePlayer(ServerPlayer player, ServerLevel level, SignalCampaignData data) {
        BlockPos start = data.start();
        player.teleportTo(level, start.getX() + 0.5, start.getY(), start.getZ() + 0.5, data.startYaw(), 0.0F);
        player.setRespawnPosition(level.dimension(), start, data.startYaw(), true, false);
        player.setGameMode(GameType.ADVENTURE);
        boolean hasRadio = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.HANDHELD_RADIO.get())) {
                hasRadio = true;
                break;
            }
        }
        if (!hasRadio) {
            player.getInventory().add(new ItemStack(ModItems.HANDHELD_RADIO.get()));
        }
        player.sendSystemMessage(Component.translatable("command.signal.scenario.player_ready").withStyle(ChatFormatting.GRAY));
    }

    private static String fmt(BlockPos p) {
        return p.getX() + " " + p.getY() + " " + p.getZ();
    }
}
