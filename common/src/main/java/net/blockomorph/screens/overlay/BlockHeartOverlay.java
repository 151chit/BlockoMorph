package net.blockomorph.screens.overlay;

import net.blockomorph.core.render.RenderingPlatformService;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.PlayerAccessor;
import net.blockomorph.utils.config.Config;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public class BlockHeartOverlay implements Overlay {
	private static final Identifier BAR_IMAGE = GuiUtils.res("textures/screens/icons.png");
	private static final Identifier HEART_INNER = GuiUtils.res("textures/screens/empty_heart_inner.png");
	private static final Identifier HEART_OUT = GuiUtils.res("textures/screens/empty_heart_out.png");
	private static final Identifier HEART_OUT_ABS = GuiUtils.res("textures/screens/empty_heart_abs.png");

	@Override
	public void render(GuiUtils gui, int screenWidth, int screenHeight) {
		if (GuiUtils.MC.player instanceof PlayerAccessor player && player.isBlockomorphActive() &&
				GuiUtils.MC.gameMode != null && GuiUtils.MC.gameMode.canHurtPlayer()) {
			switch (Config.get().hitReaction.getValue()) {
				case MELEE, PROJECTILES, FULL_PVP -> this.renderBlockHearts(player, gui, screenWidth, screenHeight);
				case BRAKING -> this.renderBlockSharps(player, gui, screenWidth, screenHeight);
				default -> {}
			}
		}
	}

	private void renderBlockSharps(PlayerAccessor pl, GuiUtils gui, int screenWidth, int screenHeight) {
		int progress = pl.getBiggestDestroyProgress();
		int x = screenWidth / 2 - 90;
		int y = screenHeight - 38;
		gui.blit(BAR_IMAGE, x - 1, y - 1, 0, progress == 9 ? 9 : 0, 81, 9, 81, 18);
		TextureData data = TextureData.get(pl);
		for (int i = 0; i < 10; i++) {
			if (i < 9 - progress) {
				gui.renderFromSpriteClass(data.sprite, x + i * 8, y, 7, 7, data.tint);
			}
		}
	}

	private void renderBlockHearts(PlayerAccessor player, GuiUtils gui, int screenWidth, int screenHeight) {
		int x = screenWidth / 2 - 91;
		int y = screenHeight - 39;

		TextureData data = TextureData.get(player);
		Player realPlayer = player.player();
		int hpCount = Mth.ceil(realPlayer.getHealth());
		int maxHpCount = Mth.ceil(realPlayer.getMaxHealth());
		int absHp = Mth.ceil(realPlayer.getAbsorptionAmount());

		int maxHearts = Mth.ceil((float) maxHpCount /2) + Mth.ceil((float) absHp /2);
		int yCount = 0;
		int xCount = 0;
		int currentHp = 0;
		boolean absHearts = false;
		for (int i = 0; i < maxHearts; i++) {
			hpCount = hpCount - 2;
			currentHp = currentHp + 2;
			Boolean bool;
			if (hpCount >= 0) {
				bool = false;
			} else if (hpCount == -1) {
				bool = true;
			} else {
				bool = null;
			}
			this.renderOneHeart(gui, x + xCount * 8, y - yCount * 10, data.sprite, bool, data.tint, absHearts);
			if (!absHearts && currentHp >= maxHpCount) {
				hpCount = absHp;
				absHearts = true;
			}
			xCount++;
			if (xCount == 10) {
				xCount = 0;
				yCount++;
			}
		}

	}

	private void renderOneHeart(GuiUtils gui, int x, int y, TextureAtlasSprite sprite, @Nullable Boolean half, int alphaOverlay, boolean absHeart) {
		gui.blitMonoImage(HEART_INNER, x, y, 9, 9);
		if (half != null) {
			gui.enableScissors(x + 1, y + 1, x + 8 + (half ? -3 : 0), y + 6);
			gui.renderFromSpriteClass(sprite, x + 1, y + 1, 7, 7, alphaOverlay);
			gui.disableScissors();

			gui.enableScissors(x + 3, y + 6, x + 6 + (half ? -1 : 0), y + 8);
			gui.renderFromSpriteClass(sprite, x + 1, y + 1, 7, 7, alphaOverlay);
			gui.disableScissors();
		}
		gui.blitMonoImage(absHeart ? HEART_OUT_ABS : HEART_OUT, x, y, 9, 9);
	}

	private record TextureData(TextureAtlasSprite sprite, int tint) {
		static TextureData get(PlayerAccessor pl) {
			BlockInPlayer2 block = getBlock(pl);
			var service = RenderingPlatformService.INSTANCE;
			var level = pl.player().level() instanceof BlockAndTintGetter getter ? getter : BlockAndTintGetter.EMPTY;
			TextureAtlasSprite particle = service.particleIcon(level, block.getPos(), block.getBlockState());
			Integer tint = service.tintForBlock(level, block.getPos(), block.getBlockState());
			return new TextureData(particle, tint != null ? (0xFF000000 | tint) : -1);
		}

		private static BlockInPlayer2 getBlock(PlayerAccessor pl) {
			var randomBlock = pl.getBlocksStorage().randomSortedBlockOrThrow();
			var block = pl.getBlock(InPlayerBlockPos.ZERO);
			if (block == null) {
				return randomBlock;
			}
			return block;
		}
	}
}
