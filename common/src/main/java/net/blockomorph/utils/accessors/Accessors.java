package net.blockomorph.utils.accessors;

import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.shapes.CollisionContext;

public enum Accessors {;

	public interface BlockAccessor {
		CompoundTag getTag$bm();
		static BlockAccessor of(BlockInput input) {
			return (BlockAccessor) input;
		}
	}

	public interface CategoryTabAccessor {
		CreativeModeTab.DisplayItemsGenerator getItemsFormer$bm();
	}

	public interface EntityAccessor {
		void unmarkRemoved$bm();
		void positionRider$bm(Entity passenger, Entity.MoveFunction function);
		static EntityAccessor of(Entity ent) {
			return (EntityAccessor) ent;
		}
	}

	public interface LivingEntityAccessor {
		void dropAllLoot$bm(DamageSource damage);
		static LivingEntityAccessor of(LivingEntity entity) {
			return (LivingEntityAccessor) entity;
		}
	}

	public interface ParticleAccessor {
		double getCoords$bm(Direction.Axis axis);
		static ParticleAccessor of(Particle particle) {
			return (ParticleAccessor) particle;
		}
	}

	public interface GuiGraphicsAccessor {
		GuiRenderState getGuiRenderState$bm();
		static GuiGraphicsAccessor of(GuiGraphics guiGraphics) {
			return (GuiGraphicsAccessor) guiGraphics;
		}
	}

	public interface FallingBlockAccessor {
		void prepareEntity$bm(FallingBlockEntity block);
		static FallingBlockAccessor of(FallingBlock fl) {
			return (FallingBlockAccessor) fl;
		}
	}

	public interface ClipContextAccessor {
		CollisionContext getCollisionCtx$bm();
		static ClipContextAccessor of(ClipContext ctx) {
			return (ClipContextAccessor) ctx;
		}
	}

	public interface BlockRendererDispatcherAccessor {
		LiquidBlockRenderer getFluidRenderer$bm();
		static LiquidBlockRenderer getFluidRenderer() {
			return ((BlockRendererDispatcherAccessor) GuiUtils.MC.getBlockRenderer()).getFluidRenderer$bm();
		}
	}

	public interface GameRendererAccessor {
		boolean shouldRenderOutline$bm();
		static boolean shouldRenderOutLine() {
			return ((GameRendererAccessor) GuiUtils.MC.gameRenderer).shouldRenderOutline$bm();
		}
	}
}
