package net.blockomorph.screens.utils;

public interface ConfigSyncListener {
	void onConfigSynced();

	default void onOperatorRightsChanged() {
	}
}
