package net.blockomorph.utils.config.list;

import net.blockomorph.screens.config.renderers.BlockIdsSetConfigRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class BlockIdsSetConfig extends RegistryIdsSetConfig<Block> {
	private static BlockIdsSetConfigRenderer RENDERER;
	private final Identifier frameTexture;

	public BlockIdsSetConfig(String name, Set<Identifier> initialValue, boolean canOperatorModify, @Nullable Component tip, IdsSetOptionContext context, Identifier frame) {
		super(name, initialValue, canOperatorModify, tip, context, Registries.BLOCK);
		this.frameTexture = frame;
	}

	public Identifier getFrameTexture() {
		return this.frameTexture;
	}

	@Override
	public BlockIdsSetConfigRenderer getRenderer() {
		if (RENDERER == null) {
			RENDERER = new BlockIdsSetConfigRenderer();
		}
		return RENDERER;
	}
}

