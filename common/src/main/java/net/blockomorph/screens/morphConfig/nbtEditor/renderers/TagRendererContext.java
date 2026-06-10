package net.blockomorph.screens.morphConfig.nbtEditor.renderers;

import net.blockomorph.screens.morphConfig.nbtEditor.renderers.overlays.TagEditingOverlay;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record TagRendererContext<T extends Tag>(
		HolderGetter.Provider provider,
		@Nullable Consumer<T> onTagUpdate,
		BiConsumer<String, Boolean> onEntering,
		Runnable onMainTagEdited,
		Consumer<TagEditingOverlay> onTagEditingRequested,
		Consumer<HashMap<String, String>> onSoftRebuildRequested,
		@Nullable Runnable onInterpretationBrake
) {

	public TagRendererContext(HolderGetter.Provider provider, BiConsumer<String, Boolean> onEntering, Runnable onMainTagEdited, Consumer<TagEditingOverlay> onTagEditingRequested, Consumer<HashMap<String, String>> onSoftRebuildRequested) {
		this(provider, null, onEntering, onMainTagEdited, onTagEditingRequested, onSoftRebuildRequested, null);
	}

	public <TYPE extends Tag> TagRendererContext<TYPE> withTagUpdateListener(Consumer<TYPE> type) {
		return new TagRendererContext<>(this.provider, type, this.onEntering, this.onMainTagEdited, this.onTagEditingRequested, this.onSoftRebuildRequested, this.onInterpretationBrake);
	}

	public TagRendererContext<T> forInterpretation(Runnable onInterpretationBrake) {
		return new TagRendererContext<>(this.provider, this.onTagUpdate, this.onEntering, this.onMainTagEdited, this.onTagEditingRequested, this.onSoftRebuildRequested, onInterpretationBrake);
	}
}
