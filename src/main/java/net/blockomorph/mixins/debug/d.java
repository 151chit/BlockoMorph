package net.blockomorph.mixins.debug;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;

@Debug(export = true)
@Mixin(ClientPacketListener.class)
public class d {
}
