package net.blockomorph.mixins.main.client.graphic;

import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

import java.util.List;
import java.util.Locale;

@Mixin(DebugScreenOverlay.class)
public class DebugScreenMixin {

	@Shadow
	private HitResult block;

	@Shadow @Final private Minecraft minecraft;

	@Shadow
	private HitResult liquid;

	@Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1))
	public boolean redirectAddBlock(List<String> instance, Object e) {
		return this.redirectBlockInfo(instance, e, false);
	}

	@Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 5))
	public boolean redirectAddFluid(List<String> instance, Object e) {
		return this.redirectBlockInfo(instance, e, true);
	}

	private boolean redirectBlockInfo(List<String> instance, Object e, boolean fluid) {
		if ((fluid ? this.liquid : block) instanceof MorphedPlayerHitResult hit) {
			if (this.minecraft.player.getAbilities().instabuild) {
				return instance.add((String) e);
			}
			Vec3 position = MorphUtils.getRealBlockPos(hit.getPlayer(), hit.getOffset());
			String x = this.formatCoordinate(position.x);
			String y = this.formatCoordinate(position.y);
			String z = this.formatCoordinate(position.z);
			String st = String.format(Locale.ROOT, "Targeted " + (fluid ? "Fluid" : "Block") + ": %s, %s, %s", x, y, z);
			return instance.add(ChatFormatting.UNDERLINE + st);
		}
		return instance.add((String) e);
	}

	private String formatCoordinate(double coordinate) {
		if (coordinate == (int) coordinate) {
			return String.valueOf((int) coordinate);
		} else {
			return String.format(Locale.ROOT, "%.3f", coordinate);
		}
	}
}
