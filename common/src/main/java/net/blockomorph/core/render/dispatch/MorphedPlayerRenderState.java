package net.blockomorph.core.render.dispatch;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.render.blockGetter.BlocksRenderState;
import net.blockomorph.core.render.renderers.MorphedMainRenderer;
import net.blockomorph.core.render.renderers.MorphedRenderer;
import net.blockomorph.core.storage.BlocksInPlayerStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface MorphedPlayerRenderState {
	MorphedRenderer renderer();

	class BlockMorph implements MorphedPlayerRenderState {
		public MorphedMainRenderer handler;
		public final List<AdditionalBlockData> additionalBlockData = new ObjectArrayList<>(BlocksInPlayerStorage.ONE_AXIS);
		public double matrixOffsetX, matrixOffsetZ;
		@Nullable public BlocksRenderState immediateBlocksData;
		@Nullable public InPlayerBlockPos outLinePos;
		@Nullable public VoxelShape outLineShape;

		@Override
		public MorphedMainRenderer renderer() {
			return this.handler;
		}

		public record AdditionalBlockData(InPlayerBlockPos pos, BlockState blockState, BlockPos blockPos, Object blockEntity, int brakeProgress) {}
	}

	class TntMorph implements MorphedPlayerRenderState {
		public MorphedMainRenderer handler;
		public Object tntRenderState;

		@Override
		public MorphedMainRenderer renderer() {
			return this.handler;
		}
	}

	class SerializeProcessState implements MorphedPlayerRenderState {
		static SerializeProcessState INSTANCE = new SerializeProcessState();
		private SerializeProcessState() {}

		@Override
		public MorphedRenderer renderer() {
			return MorphedRenderer.DUMMY;
		}
	}

	interface Holder {
		void setRenderState(MorphedPlayerRenderState state);
		MorphedPlayerRenderState getState();
	}
}
