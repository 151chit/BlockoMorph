package net.blockomorph.mixins.main.system.inPlayerManager.tnt;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.TntHandler;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.utils.mixin.FastInject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(value = ServerLevel.class, priority = 990)
public abstract class ServerLevelMixin {

	@FastInject(method = "addEntity", at = @At("HEAD"))
	private byte redirectTnt(Entity entity) {
		if (entity instanceof PrimedTnt tnt) {
			UUID thisTickTnt = TntHandler.getTickingThisTick();
			if (thisTickTnt != null && thisTickTnt.equals(tnt.getUUID()))
				return -1;
			PlayerAccessor pl = InPlayerBlockPos.findPlayer(tnt.blockPosition());
			if (pl != null && pl.getManager().getTntHandler().putTntFromLevel(tnt))
				return -1;
		}
		return 0;
	}

}