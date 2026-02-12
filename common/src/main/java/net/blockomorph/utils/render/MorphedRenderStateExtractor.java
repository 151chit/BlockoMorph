package net.blockomorph.utils.render;

import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.LevelRendererAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;

import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

public class MorphedRenderStateExtractor {

	public static MorphedPlayerRenderState extractRenderState(PlayerAccessor pl, MorphedPlayerRenderState source, float deltaTick) {
		source.deltaTick = deltaTick;
		Optional<BlockPos> pos = pl.player().getSleepingPos();
		pos.ifPresent(blockPos -> source.sleepingPos = blockPos);
		if (pl.isFullActive()) {
			source.morphedState = extractBlocks(pl);
		} else if (pl.getTnt() != null) {
			TntRenderer renderer = (TntRenderer) GuiUtils.MC.getEntityRenderDispatcher().getRenderer(pl.getTnt());
			MorphedPlayerRenderState.TntMorphedState tntMorphedState = new MorphedPlayerRenderState.TntMorphedState();
			tntMorphedState.tntRenderState = renderer.createRenderState(pl.getTnt(), deltaTick);
			source.morphedState = tntMorphedState;
		}
		return source;
	}

	public static MorphedPlayerRenderState.BlockMorphedState extractBlocks(PlayerAccessor pl) {
		MorphedPlayerRenderState.BlockMorphedState blockMorphedState = new MorphedPlayerRenderState.BlockMorphedState();
		Player player = pl.player();

		blockMorphedState.level = player.level();
		blockMorphedState.matrixOffset = adjustMatrixForPlayer(pl, player);
		InPlayerBlockPos frame = getFramedBlock(pl);
		if (frame != null) {
			blockMorphedState.framedBlock = pl.getRenderShape(frame, GuiUtils.MC.player);
		}

		for (Map.Entry<InPlayerBlockPos, BlockInPlayer2> entry : pl.getBlocksData2().entrySet()) {
			MorphedPlayerRenderState.BlockInfo info = new MorphedPlayerRenderState.BlockInfo();
			BlockInPlayer2 block = entry.getValue();
			info.blockState = block.getBlockState();
			info.blockEntity = block.getBlockEntity();
			info.offset = block.getOffset();
			info.keyPos = block.getPos();
			info.needFluidAction = block.shouldDoFluidAction();
			info.renderLight = LightTexture.pack(
					blockMorphedState.level.getBrightness(LightLayer.BLOCK, info.keyPos),
					blockMorphedState.level.getBrightness(LightLayer.SKY, info.keyPos));
			info.brakeProgress = getBrakeProgress(player, info.keyPos);
			blockMorphedState.blocks.add(info);
		}

		return blockMorphedState;
	}

	private static Vector2d adjustMatrixForPlayer(PlayerAccessor pl, Player player) {
		InPlayerBlockPos minpos = pl.minPos();
		AABB hitbox = player.getBoundingBox();
		Vec3 playerCenter = player.position();

		double offsetX = hitbox.minX - (playerCenter.x + minpos.getX());
		double offsetZ = hitbox.minZ - (playerCenter.z + minpos.getZ());

		return new Vector2d(offsetX, offsetZ);
	}

	private static InPlayerBlockPos getFramedBlock(PlayerAccessor pl) {
		if (GuiUtils.MC.hitResult instanceof MorphedPlayerHitResult hit && !GuiUtils.MC.options.hideGui && hit.getPlayer() == pl) {
			InPlayerBlockPos offset = hit.getOffset();
			if (GuiUtils.MC.player.isSpectator()) {
				return MorphUtils.canOpenMenuIn(pl, offset) ? offset : null;
			} else if (GuiUtils.MC.gameMode.getPlayerMode() == GameType.ADVENTURE) {
				return MorphUtils.isAdventureCanBreak(pl, GuiUtils.MC.player, offset) ? offset : null;
			}
			return offset;
		}
		return null;
	}

	private static int getBrakeProgress(Player player, BlockPos bounded) {
		if (Config.get().hitReaction.getValue() != ConfigEnums.HitReaction.BRAKING) {
			float percentage = player.getHealth() / player.getMaxHealth();
			if (percentage < 0.03) return 9;
			int k = Mth.ceil(percentage * 10f);
			return 9 - k;
		}
		return getBrakeProgress(bounded);
	}

	public static int getBrakeProgress(BlockPos bounded) {
		LevelRendererAccessor acc = LevelRendererAccessor.of(Minecraft.getInstance().levelRenderer);
		SortedSet<BlockDestructionProgress> pos = acc.getBrakingBlocks().get(bounded.asLong());
		if (pos != null) {
			BlockDestructionProgress progress = pos.last();
			if (progress != null) {
				return progress.getProgress();
			}
		}
		return -1;
	}
}
