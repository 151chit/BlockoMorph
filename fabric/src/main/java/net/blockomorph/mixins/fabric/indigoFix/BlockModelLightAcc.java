package net.blockomorph.mixins.fabric.indigoFix;

import net.minecraft.client.renderer.block.BlockModelLighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockModelLighter.class)
public interface BlockModelLightAcc {

	@Accessor("CACHE")
	static ThreadLocal<BlockModelLighter.Cache> cache$bm() {
		throw new AssertionError();
	}
}
