package io.github.b1tr0n1n.signal.survival;

import io.github.b1tr0n1n.signal.domain.Magazine;
import io.github.b1tr0n1n.signal.item.RevolverItem;
import io.github.b1tr0n1n.signal.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A reload in progress, started by the reload key: one round goes in every {@code reloadTicksPerRound}
 * until the gun is full or the pockets are empty. Firing, switching away from the gun or logging out stops
 * it. Server only; the state is per player and never saved.
 */
public final class Reloading {
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    /** Begin reloading the gun in the main hand; false (with a reason on screen) when there is nothing to do. */
    public static boolean start(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof RevolverItem gun)) {
            return false;
        }
        int loaded = RevolverItem.loaded(stack), carried = RevolverItem.carried(player);
        if (loaded >= gun.spec().capacity()) {
            player.displayClientMessage(Component.translatable("message.signal.reload.full").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        if (carried == 0) {
            player.displayClientMessage(Component.translatable("message.signal.reload.none").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (ACTIVE.putIfAbsent(player.getUUID(), 0) == null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.REVOLVER_COCK.get(), SoundSource.PLAYERS, 0.6F, 0.8F);
            progress(player, loaded, gun.spec().capacity());
        }
        return true;
    }

    public static boolean isReloading(Player player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void cancel(Player player) {
        ACTIVE.remove(player.getUUID());
    }

    /** Advance one tick; public so tests can drive it without a real tick loop. */
    public static void tick(ServerPlayer player) {
        Integer ticks = ACTIVE.get(player.getUUID());
        if (ticks == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof RevolverItem gun)) {
            cancel(player); // switched away
            return;
        }
        ticks++;
        ACTIVE.put(player.getUUID(), ticks);
        if (ticks % gun.spec().reloadTicksPerRound() != 0) {
            return;
        }
        Magazine.Load load = gun.loadOneRound(player, stack);
        int capacity = gun.spec().capacity();
        if (!load.loadedOne() || load.loaded() >= capacity || RevolverItem.carried(player) == 0) {
            cancel(player);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.REVOLVER_COCK.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        }
        progress(player, load.loaded(), capacity);
    }

    private static void progress(ServerPlayer player, int loaded, int capacity) {
        player.displayClientMessage(Component.translatable(isReloading(player) ? "message.signal.reload.progress" : "message.signal.reload.done",
                loaded, capacity, RevolverItem.carried(player)).withStyle(ChatFormatting.GRAY), true);
    }

    @SubscribeEvent
    public void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            tick(player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        cancel(event.getEntity());
    }
}
