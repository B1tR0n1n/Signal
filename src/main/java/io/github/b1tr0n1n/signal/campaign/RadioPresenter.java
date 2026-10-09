package io.github.b1tr0n1n.signal.campaign;

import io.github.b1tr0n1n.signal.domain.RadioCatalog;
import io.github.b1tr0n1n.signal.domain.RadioMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;
import java.util.Optional;

/**
 * Presents radio traffic as subtitled chat lines (text-first; no voice acting is claimed). Live
 * presentation happens once per message; the transcript can be replayed freely without side effects.
 */
public final class RadioPresenter {
    private RadioPresenter() {
    }

    /** Present newly delivered messages to the participant if online; otherwise they wait for login. */
    public static void presentDelivered(MinecraftServer server, SignalCampaignData data, List<RadioMessage> delivered) {
        if (delivered.isEmpty()) {
            return;
        }
        data.setDirty();
        presentPending(server, data);
    }

    /** Show every delivered-but-unseen message to the participant, in delivery order, exactly once. */
    public static void presentPending(MinecraftServer server, SignalCampaignData data) {
        if (!data.isActive() || data.participant() == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(data.participant());
        if (player == null) {
            return;
        }
        for (String id : data.site().radio().unpresented()) {
            Optional<RadioMessage> m = RadioCatalog.byId(id);
            m.ifPresent(msg -> player.sendSystemMessage(line(msg, false)));
            data.site().radio().markPresented(id);
            data.setDirty();
        }
        // A short, original-to-vanilla click cue; the text above carries all information.
        player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.35F, 0.6F);
    }

    /** Read-only transcript replay. Never delivers, never records history. */
    public static void showTranscript(ServerPlayer player) {
        SignalCampaignData data = SignalCampaignData.get(player.server);
        if (!data.isActive() || !data.isParticipant(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("radio.signal.no_carrier").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        List<String> transcript = data.site().radio().transcript();
        player.sendSystemMessage(Component.translatable("radio.signal.transcript.header", transcript.size())
                .withStyle(ChatFormatting.GRAY, ChatFormatting.UNDERLINE));
        for (String id : transcript) {
            RadioCatalog.byId(id).ifPresent(msg -> player.sendSystemMessage(line(msg, true)));
        }
    }

    public static MutableComponent line(RadioMessage msg, boolean replay) {
        MutableComponent prefix = Component.literal("[")
                .append(Component.translatable("radio.signal.channel." + msg.channel()))
                .append("] ")
                .withStyle(replay ? ChatFormatting.DARK_GRAY : ChatFormatting.DARK_AQUA);
        MutableComponent speaker = Component.translatable("character.signal." + msg.speaker())
                .append(": ")
                .withStyle(ChatFormatting.GOLD);
        MutableComponent text = Component.translatable(msg.textKey())
                .withStyle(replay ? ChatFormatting.GRAY : ChatFormatting.WHITE);
        return prefix.append(speaker).append(text);
    }
}
