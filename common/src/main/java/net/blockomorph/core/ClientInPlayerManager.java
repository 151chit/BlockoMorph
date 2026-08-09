package net.blockomorph.core;

import net.blockomorph.core.render.dispatch.SectionsDirtyMarker;
import net.blockomorph.core.render.renderers.MorphedMainRenderer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ClientInPlayerManager extends InPlayerManager {
	private final DestructionProgressHandler destructionHandler;
	private final SectionsDirtyMarker sectionsDirtyMarker;
	private final MorphedMainRenderer renderer;

	protected ClientInPlayerManager(PlayerAccessor player) {
		super(player);
		this.getFlags().managerInit.setDirect(true);
		this.destructionHandler = new DestructionProgressHandler(this);
		this.sectionsDirtyMarker = new SectionsDirtyMarker(this);
		this.renderer = new MorphedMainRenderer(this, HEAVY_MODE);
		this.getFlags().managerInit.setDirect(false);
	}

	@Override
	public ClientInPlayerManager assertOnInit() {
		super.assertOnInit();
		return this;
	}

	@Override
	public boolean setBlock(int inPlayerBlockPos, BlockState state, int flags, int suppressHitboxChange) {
		BlockInPlayer2 block = this.getBlocksStorage().get(inPlayerBlockPos);
		BlockState one = block != null ? block.getBlockState() : Blocks.AIR.defaultBlockState();
		boolean result = super.setBlock(inPlayerBlockPos, state, flags, suppressHitboxChange);
		BlockInPlayer2 newBlock = this.getBlocksStorage().get(inPlayerBlockPos);
		BlockState two = newBlock != null ? newBlock.getBlockState() : Blocks.AIR.defaultBlockState();
		if (one != two) {
			this.sectionsDirtyMarker.markSectionDirty(inPlayerBlockPos);
			return result;
		}
		return result;
	}

	@Override
	protected void onBlockAdded(BlockInPlayer2 block) {
		super.onBlockAdded(block);
		this.sectionsDirtyMarker.markSectionDirty(block.getOffset());
	}

	@Override
	protected void onBlockRemoved(BlockInPlayer2 block) {
		super.onBlockRemoved(block);
		this.destructionHandler.removeBlock(block.getOffsetAsInt());
		this.sectionsDirtyMarker.markSectionDirty(block.getOffset());
	}

	@Override
	protected void tick() {
		super.tick();
		this.destructionHandler.tick();
		this.sectionsDirtyMarker.tick();
	}

	public MorphedMainRenderer getRenderer() {
		return this.renderer;
	}

	public SectionsDirtyMarker getSectionsDirtyMarker() {
		return this.sectionsDirtyMarker;
	}

	public DestructionProgressHandler getDestructionHandler() {
		return this.destructionHandler;
	}
}
