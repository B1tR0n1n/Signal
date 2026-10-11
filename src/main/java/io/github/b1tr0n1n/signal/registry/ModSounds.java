package io.github.b1tr0n1n.signal.registry;

import io.github.b1tr0n1n.signal.SignalMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Original synthesized sounds (tools/gen_sounds.py). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, SignalMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_SHOT = sound("revolver_shot");
    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_DRY = sound("revolver_dry");
    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_LOAD = sound("revolver_load");
    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_COCK = sound("revolver_cock");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRAWN_AMBIENT = sound("drawn_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRAWN_ALERT = sound("drawn_alert");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRAWN_HURT = sound("drawn_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRAWN_DEATH = sound("drawn_death");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(SignalMod.MOD_ID, name)));
    }

    private ModSounds() {
    }
}
