package net.blockomorph.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.BlockInPlayer2;
import net.blockomorph.utils.MorphUtils;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.accessors.ClientLevelAccessor;
import net.blockomorph.utils.accessors.LevelRendererAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.utils.config.ConfigEnums;
import net.blockomorph.utils.coords.InPlayerBlockPos;
import net.blockomorph.utils.hit.MorphedPlayerHitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;

import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

public class MorphedRenderStateExtractor {

	public static MorphedPlayerRenderState extractRenderState(PlayerAccessor pl, MorphedPlayerRenderState source, float deltaTick) {
		Optional<BlockPos> pos = pl.player().getSleepingPos();
		pos.ifPresent(blockPos -> source.sleepingPos = blockPos);
		if (pl.isFullActive() && pl.player().level() instanceof BlockAndTintGetter getter) {
			source.morphedState = extractBlocks(pl, deltaTick, getter);
		} else if (pl.getTnt() != null) {
			TntRenderer renderer = (TntRenderer) GuiUtils.MC.getEntityRenderDispatcher().getRenderer(pl.getTnt());
			MorphedPlayerRenderState.TntMorphedState tntMorphedState = new MorphedPlayerRenderState.TntMorphedState();
			tntMorphedState.tntRenderState = renderer.createRenderState();
			try {
				renderer.extractRenderState(pl.getTnt(), tntMorphedState.tntRenderState, deltaTick);
			} catch (Exception ignored) {}
			source.morphedState = tntMorphedState;
		}
		return source;
	}

	public static MorphedPlayerRenderState.BlockMorphedState extractBlocks(PlayerAccessor pl, float deltaTick, BlockAndTintGetter getter) {
		MorphedPlayerRenderState.BlockMorphedState blockMorphedState = new MorphedPlayerRenderState.BlockMorphedState();
		Player player = pl.player();

		blockMorphedState.level = getter;
		blockMorphedState.matrixOffset = adjustMatrixForPlayer(pl, player);
		InPlayerBlockPos frame = getFramedBlock(pl);
		if (frame != null) {
			blockMorphedState.framedBlock = pl.getRenderShape(frame, GuiUtils.MC.player);
		}

		for (Map.Entry<InPlayerBlockPos, BlockInPlayer2> entry : pl.getBlocksData2().entrySet()) {
			MorphedPlayerRenderState.BlockInfo info = new MorphedPlayerRenderState.BlockInfo();
			BlockInPlayer2 block = entry.getValue();
			info.blockState = block.getBlockState();
			info.keyPos = block.getPos();
			info.brakeProgress = getBrakeProgress(player, info.keyPos);
			info.offset = block.getOffset();
			fillBlockEntity(player.oldPosition().lerp(player.position(), deltaTick), block.getBlockEntity(), info, blockMorphedState.matrixOffset, deltaTick, getter);
			info.needFluidAction = block.shouldDoFluidAction();
			blockMorphedState.blocks.add(info);
		}

		return blockMorphedState;
	}

	private static void fillBlockEntity(Vec3 playerPos, BlockEntity blockEntity, MorphedPlayerRenderState.BlockInfo info, Vector2d matrixOffset, float deltaTick, BlockAndTintGetter getter) {
		if (blockEntity != null) {
			BlockEntityRenderer<BlockEntity, BlockEntityRenderState> renderer = GuiUtils.MC.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
			if (renderer != null) {
				BlockEntityRenderState rd = renderer.createRenderState();
				ModelFeatureRenderer.CrumblingOverlay overlay = null;
				int k = info.brakeProgress;
				PoseStack poseStack = new PoseStack();
				Vec3 camPos = GuiUtils.MC.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState.pos;
				poseStack.translate(playerPos.subtract(camPos));
				poseStack.translate(matrixOffset.x, 0, matrixOffset.y);
				poseStack.translate(info.offset.x, info.offset.y, info.offset.z);
				if (k > -1 && k < 10) overlay = new ModelFeatureRenderer.CrumblingOverlay(k, poseStack.last());
				ClientLevelAccessor acc = ClientLevelAccessor.of(getter);
				acc.setSpecialRenderingMode(true);
				try {
					renderer.extractRenderState(blockEntity, rd, deltaTick, camPos, overlay);
				} catch (Exception ignored) {} finally {
					acc.setSpecialRenderingMode(false);
				}
				info.blockEntity = rd;
			}
		}
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
