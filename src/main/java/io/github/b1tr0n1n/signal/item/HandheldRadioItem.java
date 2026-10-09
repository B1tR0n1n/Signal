package io.github.b1tr0n1n.signal.item;

import io.github.b1tr0n1n.signal.campaign.RadioPresenter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Basic handheld radio: right-click replays the received transcript. Replay is read-only; it never
 * re-delivers a message or records an event. Channels and tuning are M2.
 */
public class HandheldRadioItem extends Item {
    public HandheldRadioItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            RadioPresenter.showTranscript(sp);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
