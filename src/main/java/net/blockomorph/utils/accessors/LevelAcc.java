package net.blockomorph.utils.accessors;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class LevelAcc {

	public static Level of(Object lv) {
		return (Level) lv;
	}

	public static ServerLevel ofSv(Object lv) {
		return (ServerLevel) lv;
	}
}
