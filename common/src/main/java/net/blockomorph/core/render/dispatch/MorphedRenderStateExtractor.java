package net.blockomorph.core.render.dispatch;

import net.blockomorph.core.BlockInPlayer2;
import net.blockomorph.core.ClientInPlayerManager;
import net.blockomorph.core.coords.math.MorphMath;
import net.blockomorph.core.phys.hit.MorphedPlayerHitResult;
import net.blockomorph.core.render.renderers.async.AsyncRenderersStorage;
import net.blockomorph.screens.utils.GuiUtils;
import net.blockomorph.utils.accessors.Accessors;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.function.Supplier;

public class MorphedRenderStateExtractor {
	private final ClientInPlayerManager manager;
	private final SectionsStateExtractor sectionsStateExtractor;

	public MorphedRenderStateExtractor(ClientInPlayerManager manager, int heavyMode, Supplier<AsyncRenderersStorage> playerBaker) {
		this.manager = manager.assertOnInit();
		this.sectionsStateExtractor = new SectionsStateExtractor(manager, heavyMode, playerBaker);
	}

	public MorphedPlayerRenderState createRenderState() {
		if (this.manager.getFlags().serializingProcess.isTrue())
			return MorphedPlayerRenderState.SerializeProcessState.INSTANCE;
		if (this.manager.getBlocksStorage().size() > 0) {
			if (this.manager.getTntHandler().isActive()) {
				return new MorphedPlayerRenderState.TntMorph();
			}
			return new MorphedPlayerRenderState.BlockMorph();
		}
		return null;
	}

	public void extractRenderState(MorphedPlayerRenderState state, float deltaTick) {
		MorphedPlayerRenderState.BlockMorph bmMorph = state instanceof MorphedPlayerRenderState.BlockMorph morph ? morph : null;
		if (bmMorph != null) bmMorph.additionalBlockData.clear();
		this.sectionsStateExtractor.forEachBlockInPlayer(bmMorph, block ->
			this.fillAdditionalDataAndOldPosUpdate(block, bmMorph, deltaTick)
		);
		if (bmMorph != null) {
			bmMorph.handler = this.manager.getRenderer();
			bmMorph.matrixOffsetX = MorphMath.adjustMatrixForPlayer(this.manager.getOwner(), Direction.Axis.X);
			bmMorph.matrixOffsetZ =	MorphMath.adjustMatrixForPlayer(this.manager.getOwner(), Direction.Axis.Z);
			this.fillOutline(bmMorph);
		} else if (state instanceof MorphedPlayerRenderState.TntMorph tntMorph) {
			tntMorph.handler = this.manager.getRenderer();
			tntMorph.tntRenderState = ContextDependStateExtractor.extractRenderStateFor(this.manager.getTntHandler().getActiveTnt(), deltaTick, this.manager);
		}
	}

	private void fillAdditionalDataAndOldPosUpdate(BlockInPlayer2 block, MorphedPlayerRenderState.BlockMorph blockMorph, float deltaTick) {
		if (blockMorph == null) return;
		int brake = this.manager.getDestructionHandler().getBestProgress(block.getOffsetAsInt());
		if (block.getBlockEntity() != null || brake >= 0) {
			blockMorph.additionalBlockData.add(new MorphedPlayerRenderState.BlockMorph.AdditionalBlockData(block.getOffset(), block.getBlockState(), block.getPos(),
					ContextDependStateExtractor.extractRenderStateFor(block.getBlockEntity(), deltaTick, this.manager), brake));
		}
	}

	private void fillOutline(MorphedPlayerRenderState.BlockMorph morphState) {
		morphState.outLineShape = null;
		morphState.outLinePos = null;
		if (GuiUtils.MC.hitResult instanceof MorphedPlayerHitResult hit && GuiUtils.MC.player != null) {
			BlockInPlayer2 block = hit.getBlock();
			if (block.getOwner() == this.manager && Accessors.GameRendererAccessor.shouldRenderOutLine()) {
				morphState.outLinePos = block.getOffset();
				morphState.outLineShape = block.getBlockState().getShape(this.manager.level(), block.getPos(), CollisionContext.of(GuiUtils.MC.player));
			}
		}
	}
}
