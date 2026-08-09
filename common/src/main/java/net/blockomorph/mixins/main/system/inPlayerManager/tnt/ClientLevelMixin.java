package net.blockomorph.mixins.main.system.inPlayerManager.tnt;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ClientLevel.class, priority = 990)
public class ClientLevelMixin {

	@FastInject(method = "addEntity", at = @At("HEAD"))
	private boolean redirectTnt(Entity entity) {
		if (entity instanceof PrimedTnt tnt) {
			PlayerAccessor pl = InPlayerBlockPos.findPlayer(tnt.blockPosition());
			return pl == null || !pl.getManager().getTntHandler().putTntFromLevel(tnt);
		}
		return true;
	}
}