package net.blockomorph.utils.tnt;

import net.blockomorph.utils.MultiBlockLevel;
import net.blockomorph.utils.platform.CommonPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public abstract class TntSpawnLevel extends MultiBlockLevel {
	private PrimedTnt tnt;
	private final BlockState need;

	protected TntSpawnLevel(Level orig, BlockState need) {
		super(orig, false);
		this.need = need;
	}

	public static TntSpawnLevel create(Level orig, BlockState need) {
		return CommonPlatformUtils.INSTANCE.createLevel(orig, need);
	}

	@Override
	public boolean setBlock(BlockPos blockPos, BlockState blockState, int i, int j) {
		return false;
	}

	public boolean removeBlock(BlockPos p, boolean y) {
		return false;
	}

	public void playSound(@Nullable Player pl, BlockPos bp, SoundEvent se, SoundSource ss, float sp1, float sp2) {
		this.realLevel.playSound(pl, bp, se, ss, sp1, sp2);
	}

	@Override
	public void gameEvent(GameEvent gameEvent, Vec3 vec3, GameEvent.Context context) {

	}

	@Override
	public boolean hasNeighborSignal(BlockPos blockPos) {
		return true;
	}

	public BlockState getBlockState(BlockPos p) {
		return this.need;
	}

	public FluidState getFluidState(BlockPos p) {
		return realLevel.getFluidState(p);
	}

	public void playSeededSound(@Nullable Player a1, double a2, double a3, double a4, Holder<SoundEvent> a5, SoundSource a6, float a7, float a8, long a9) {
		realLevel.playSeededSound(a1, a2, a3, a4, a5, a6, a7, a8, a9);
	}

	public boolean addFreshEntity(Entity tnt) {
		if (tnt instanceof PrimedTnt ent) {
			this.tnt = ent;
			return true;
		}
		return false;
	}

	public Explosion explode(
			@Nullable Entity a1,
			@Nullable DamageSource a2,
			@Nullable ExplosionDamageCalculator a3,
			double a4, double a5, double a6, float a7, boolean a8,
			ExplosionInteraction a9,
			boolean a10
	) {
		return null;
	}

	@Override
	public @Nullable MapItemSavedData getMapData(String string) {
		return null;
	}

	@Override
	public void setMapData(String string, MapItemSavedData mapItemSavedData) {

	}

	@Override
	public int getFreeMapId() {
		return 0;
	}

	@Override
	public Explosion explode(
			@Nullable Entity a1,
			@Nullable DamageSource a2,
			@Nullable ExplosionDamageCalculator a3,
			double a4, double a5, double a6, float a7, boolean a8,
			ExplosionInteraction a9
	) {
		return null;
	}

	public PrimedTnt extractTnt() {
		return this.tnt;
	}
}
