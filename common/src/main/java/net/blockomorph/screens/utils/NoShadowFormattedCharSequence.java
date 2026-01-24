package net.blockomorph.screens.utils;

import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

public record NoShadowFormattedCharSequence(
		FormattedCharSequence original) implements FormattedCharSequence {//for mixin detect

	@Override
	public boolean accept(FormattedCharSink formattedCharSink) {
		return this.original.accept(formattedCharSink);
	}
}
