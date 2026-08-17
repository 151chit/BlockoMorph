package net.blockomorph.network;

import net.blockomorph.core.coords.InPlayerBlockPos;
import net.blockomorph.core.misc.chairController.EntityChairController;
import net.blockomorph.screens.utils.GuiUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.UUID;

public final class ClientBoundChairConnectPacket implements BlockMorphPacket {
	public static final String ID = "client_bound_chair_connect_packet";
	private final int ownerId;
	private final UUID targetUUID;
	private final Vec3 pos;
	private final EntityChairController.BlockCondition block;

	private ClientBoundChairConnectPacket(int ownerId, UUID target, Vec3 pos, EntityChairController.BlockCondition block) {
		this.ownerId = ownerId;
		this.targetUUID = target;
		this.pos = pos;
		this.block = block;
	}

	ClientBoundChairConnectPacket(FriendlyByteBuf buffer) {
		this.ownerId = buffer.readVarInt();
		this.targetUUID = buffer.readNullable(buf -> buf.readUUID());
		if (this.targetUUID != null) {
			this.pos = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		} else {
			this.pos = null;
		}
		this.block = buffer.readNullable(buf -> new EntityChairController.BlockCondition(
				InPlayerBlockPos.get(buf.readByte(), buf.readByte(), buf.readByte()),
				BuiltInRegistries.BLOCK.byId(buf.readVarInt())
		));
	}

	public static ClientBoundChairConnectPacket bound(LivingEntity owner, UUID target, Vec3 pos, EntityChairController.BlockCondition block) {
		return new ClientBoundChairConnectPacket(owner.getId(), Objects.requireNonNull(target), Objects.requireNonNull(pos), block);
	}

	public static ClientBoundChairConnectPacket unbound(LivingEntity owner) {
		return new ClientBoundChairConnectPacket(owner.getId(), null, null, null);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(this.ownerId);
		buffer.writeNullable(this.targetUUID, (buf, v) -> buf.writeUUID(v));
		if (this.targetUUID != null) {
			buffer.writeDouble(this.pos.x);
			buffer.writeDouble(this.pos.y);
			buffer.writeDouble(this.pos.z);
		}
		buffer.writeNullable(this.block, (buf, block) -> {
			buf.writeByte(block.blockPos().x);
			buf.writeByte(block.blockPos().y);
			buf.writeByte(block.blockPos().z);
			buf.writeVarInt(BuiltInRegistries.BLOCK.getId(block.block()));
		});
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void handle(Player player) {
		if (GuiUtils.MC.level != null && GuiUtils.MC.level.getEntity(this.ownerId) instanceof EntityChairController.ChairedEntity ent) {
			if (this.targetUUID != null) {
				ent.getController().boundForPlayer(this.targetUUID, this.pos, this.block, null);
			} else ent.getController().unbound();
		}
	}
}
