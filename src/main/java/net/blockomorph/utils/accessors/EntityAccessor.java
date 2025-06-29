package net.blockomorph.utils.accessors;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface EntityAccessor {
	void forceLevelChange(Level lv);
	Vec3 getMorphedPos();
	Vec3 getSyncedPos();

	static EntityAccessor of(Entity ent) {
		return (EntityAccessor) ent;
	}
}
