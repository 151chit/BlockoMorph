package net.blockomorph.utils.hit;

import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.PlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MorphedPlayerHitResult extends BlockHitResult {
	private final PlayerAccessor player;
	private final InPlayerBlockPos offset;
	private final Vec3 realLocation;

	private MorphedPlayerHitResult(PlayerAccessor player, InPlayerBlockPos offset, BlockPos offsetIn, Vec3 inBlockOffset, Direction direction, boolean inside, Vec3 realLoc) {
		super(inBlockOffset.add(offsetIn.getX(), offsetIn.getY(), offsetIn.getZ()), direction, offsetIn, inside);
		this.offset = offset;
		this.player = player;
		this.realLocation = realLoc;
	}

	@Nullable
	protected static MorphedPlayerHitResult of(PlayerAccessor player, InPlayerBlockPos offset, Direction direction, boolean inside, Vec3 inBlockOffset, Vec3 realLoc) {
		BlockPos offsetIn = offset.boundedBlockPos(player.player());
		if (offsetIn == null)
			return null;
		return new MorphedPlayerHitResult(player, offset, offsetIn, inBlockOffset, direction, inside, realLoc);
	}

	@Override
	public Type getType() {
		return Type.BLOCK;
	}

	public PlayerAccessor getPlayer() {
		return this.player;
	}

	public InPlayerBlockPos getOffset() {
		return this.offset;
	}

	public Vec3 getRealLocation() {
		return this.realLocation;
	}
}
