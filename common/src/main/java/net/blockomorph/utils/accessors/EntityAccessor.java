package net.blockomorph.utils.accessors;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface EntityAccessor {
	Vec3 getMorphedPos();

	Vec3 getSyncedPos();

	int getPermissionsLevel$blockomorph();

	static EntityAccessor of(Entity ent) {
		return (EntityAccessor) ent;
	}
}
