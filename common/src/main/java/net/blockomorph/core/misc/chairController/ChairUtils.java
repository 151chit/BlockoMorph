package net.blockomorph.core.misc.chairController;

import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;

public class ChairUtils {

	public static boolean checkChairCandidateAndRun(Entity entity, PlayerAccessor sitter) {
		if (entity instanceof InPlayerSpawnedEntity plEnt &&
				entity != sitter &&
				sitter instanceof EntityChairController.ChairedEntity sitterChair &&
				!entity.isRemoved()) {
			PlayerAccessor chairPlayer = plEnt.getLastOwnerPlayer();
			if (chairPlayer != null && entity.getType().getCategory() == MobCategory.MISC && isProbablyChair(entity)) {
				Vec3 ownerZero = MorphMath.getRealBlockPos(chairPlayer, Vec3.ZERO);
				Vec3 offset = testSitPos(entity, sitter).subtract(ownerZero);
				int x = Mth.floor(offset.x);
				int y = Mth.floor(offset.y);
				int z = Mth.floor(offset.z);
				if (!InPlayerBlockPos.isByteBoundInvalid(x, y, z)) {
					Vec3 entityOffset = entity.position().subtract(ownerZero);
					InPlayerBlockPos chairPos = InPlayerBlockPos.get(Mth.floor(entityOffset.x), Mth.floor(entityOffset.y), Mth.floor(entityOffset.z));
					BlockState state = chairPlayer.getBlockState(chairPos);
					if (!state.isAir()) {
						entity.discard();
						entity.setLevelCallback(EntityInLevelCallback.NULL);
						sitterChair.getController().boundForPlayer(chairPlayer.getManager().getOwnerUUID(), offset,
								new EntityChairController.BlockCondition(chairPos, state.getBlock()),
								(chair, cntr) -> {
							try {
								entity.setPos(MorphMath.getRealBlockPos(chair, entityOffset));
								Vec3 pos = entity.getDismountLocationForPassenger(cntr.getOwner());
								if (pos.distanceToSqr(cntr.getOwner().position()) > 100) return null;
								return pos;
							} catch (Exception e) {
								MorphUtils.LOGGER.warn("Chair type: {} probably do not support dismount pos calculating on player: {}. Exception info: {}",
										state, cntr.getOwner(), e.getClass().getCanonicalName() + ": " + e.getMessage());
								return null;
							}
						});
						return true;
					}
				}
			}
		}
		return false;
	}

	private static Vec3 testSitPos(Entity chair, PlayerAccessor player) {
		MutableObject<Vec3> pos = new MutableObject<>();
		Accessors.EntityAccessor.of(chair).positionRider$bm(player.player(), (ignored, x, y, z) ->
				pos.setValue(new Vec3(x, y, z))
		);
		return pos.getValue() == null ? chair.position() : pos.getValue();
	}

	private static boolean isProbablyChair(Entity chairCandidate) {
		return chairCandidate.noPhysics ||
				chairCandidate.getType().getDimensions().width() < Mth.EPSILON ||
				chairCandidate instanceof Mob mob && mob.isNoAi() ||
				!chairCandidate.shouldRender(chairCandidate.getX() + 1, chairCandidate.getY() + 1, chairCandidate.getZ() + 1);
	}

	public interface InPlayerSpawnedEntity {
		PlayerAccessor getLastOwnerPlayer();
	}

}
