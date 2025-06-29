package net.blockomorph.utils;

import net.blockomorph.utils.accessors.EntityAccessor;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.atomic.AtomicReference;

public class ChairController {
	private final Entity owner;
	private SynchedEntityData entityData;
	private final EntityDataAccessor<CompoundTag> CHAIR_DATA;
	private BlockState chairBlockstate;
	private InPlayerBlockPos realPos;

	public ChairController(Entity owner, EntityDataAccessor<CompoundTag> data) {
		this.owner = owner;
		this.CHAIR_DATA = data;
	}

	public void setEntityData(SynchedEntityData data) {
		this.entityData = data;
	}

	public void tick() {
		if (!owner.level().isClientSide && owner.getVehicle() instanceof PlayerAccessor pl && !this.entityData.get(CHAIR_DATA).isEmpty()) {
			BlockState state = this.chairBlockstate;
			if (state != null) {
				if (!pl.getBlockState(this.realPos).getBlock().equals(state.getBlock())) {
					owner.stopRiding();
				}
			}
		}
	}

	public void onStopRiding() {
		if (!owner.level().isClientSide) {
			if (owner.getVehicle() instanceof PlayerAccessor pl && !this.entityData.get(CHAIR_DATA).isEmpty()) {
				BlockState state = this.chairBlockstate;
				if (state != null) {
					BlockState state2 = this.findOccupied(state);
					if (!state.equals(state2)) {
						pl.setBlockState(this.realPos, state2, false);
					}
				}
			}
			this.entityData.set(CHAIR_DATA, new CompoundTag());
		}
	}

	public Entity replaceEntityOnMorphedPlayer(Entity entity) {
		AtomicReference<Entity> chair = new AtomicReference<>(entity);
		Vec3 pos = EntityAccessor.of(entity).getMorphedPos();
		if (pos != null) {
			if (this.isChair(entity)) {
				InPlayerBlockPos.check(BlockPos.containing(pos), (pl, realPos) -> {
					chair.set(pl.player());
					CompoundTag tg = new CompoundTag();
					entity.setPos(0, pos.y, 0);
					Vec3 vec3 = entity.getPassengerRidingPosition(this.owner);
					Vec3 vec32 = this.owner.getVehicleAttachmentPoint(entity);
					tg.putDouble("x", pos.x);
					tg.putDouble("y", vec3.y - vec32.y);
					tg.putDouble("z", pos.z);
					tg.put("Chair", NbtUtils.writeBlockState(pl.getBlockState(realPos)));
					tg.putString("RealPos", realPos.string());
					this.entityData.set(CHAIR_DATA, tg);
					entity.discard();
				}, null, this.owner.level());
			}
		}
		return chair.get();
	}

	private boolean isChair(Entity ent) {
		return ent.noPhysics || (ent instanceof Mob mob && mob.isNoAi()) || !ent.shouldRender(ent.getX(), ent.getY(), ent.getZ());
	}

	private BlockState findOccupied(BlockState state) { //TODO
		for (Property<?> prop : state.getProperties()) {
			if (prop instanceof BooleanProperty bProp && bProp.getName().equalsIgnoreCase("occupied")) {
				return state.setValue(bProp, false);
			}
		}
		return state;
	}

	public void onDataReceived(EntityDataAccessor<?> data) {
		if (data.equals(CHAIR_DATA)) {
			CompoundTag tg = this.entityData.get(CHAIR_DATA);
			if (tg.isEmpty()) {
				this.chairBlockstate = null;
				this.realPos = null;
			} else {
				this.chairBlockstate = NbtUtils.readBlockState(owner.level().holderLookup(Registries.BLOCK), tg.getCompound("Chair"));
				this.realPos = InPlayerBlockPos.parseBlockPos(tg.getString("RealPos"));
			}
		}
	}
}
