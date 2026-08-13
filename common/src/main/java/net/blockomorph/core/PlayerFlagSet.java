package net.blockomorph.core;

import net.blockomorph.core.serialization.PlayerWorldSerializer;
import net.blockomorph.network.ClientBoundSerializeInfoPacket;

import java.util.function.BooleanSupplier;

public class PlayerFlagSet {
	public final Flag isBreaking;
	public final Flag killedByPlayerHand;
	public final Flag serializingProcess;
	public final Flag morphProcess;
	public final Flag managerInit;

	PlayerFlagSet(InPlayerManager manager) {
		this.isBreaking = new Flag(() -> true);
		this.killedByPlayerHand = new Flag(manager::isServer);
		this.serializingProcess = new Flag(() -> ClientBoundSerializeInfoPacket.isReady() || PlayerWorldSerializer.inSerializeProcess(manager.getOwnerUUID()));
		this.morphProcess = new Flag(() -> false);
		this.managerInit = new Flag(() -> false);
	}

	public static class Flag {
		private final BooleanSupplier condition;
		private boolean mutableValue;

		protected Flag(BooleanSupplier condition) {
			this.condition = condition;
		}

		public void setValue(boolean yes) {
			if (this.condition.getAsBoolean()) {
				this.mutableValue = yes;
			} else throw new IllegalStateException();
		}

		protected void setDirect(boolean yes) {
			this.mutableValue = yes;
		}

		public boolean isTrue() {
			return this.mutableValue;
		}
	}
}
