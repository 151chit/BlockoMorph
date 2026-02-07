package net.blockomorph.mixins.main.client.graphic;

import com.llamalad7.mixinextras.sugar.Local;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.debug.DebugEntryLookingAtBlock;
import net.minecraft.client.gui.components.debug.DebugEntryLookingAtFluid;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(value = {DebugEntryLookingAtBlock.class, DebugEntryLookingAtFluid.class})
public class BlockInfoMixin {

	@Redirect(method = "display", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 0))
	private boolean add(List<String> instance, Object e, @Local HitResult hitResult) {
		String out = GuiUtils.redirectBlockInfo((Object)this instanceof DebugEntryLookingAtFluid, hitResult);
		if (out != null) {
			return instance.add(ChatFormatting.UNDERLINE + out);
		}
		return instance.add((String) e);
	}
}
