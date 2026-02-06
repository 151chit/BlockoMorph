package net.blockomorph.screens.morphConfig.nbtEditor;

import net.blockomorph.screens.morphConfig.nbtEditor.renderers.TagRendererContext;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.CollectionTagRenderer;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.CompoundTagRenderer;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.TagRenderer;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.primitive.NumericTagRenderer;
import net.blockomorph.screens.morphConfig.nbtEditor.renderers.tagRenderers.primitive.StringTagRenderer;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.function.Supplier;

public class TagTypes {
	private static final HashMap<TagType<?>, TagRendererFactory<?>> RENDERERS = new HashMap<>();
	private static final HashMap<TagType<?>, Supplier<?>> TAGS = new HashMap<>();

	public static <T extends Tag> void registerRenderer(TagType<T> type, TagRendererFactory<T> factory) {
		if (RENDERERS.containsKey(type))
			throw new IllegalArgumentException("Type " + type.getName() + " already registered!");
		RENDERERS.put(type, factory);
	}

	public static <T extends Tag> void registerTagCreator(TagType<T> type, Supplier<T> creator) {
		if (TAGS.containsKey(type))
			throw new IllegalArgumentException("Type " + type.getName() + " already registered!");
		TAGS.put(type, creator);
	}

	public static TagType<?>[] getRegisteredTags() {
		TagType<?>[] array = new TagType<?>[TAGS.size()];
		int i = 0;
		for (TagType<?> tagType : TAGS.keySet()) {
			array[i] = tagType;
			i++;
		}
		return array;
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public static <T extends Tag> TagRenderer<T> getRendererForTag(String name, T tag, TagRendererContext<T> ctx) {
		try {
			TagRendererFactory<T> renderSource = (TagRendererFactory<T>) RENDERERS.get(tag.getType());
			if (renderSource != null) {
				return renderSource.create(name, tag, ctx);
			}
		} catch (ClassCastException e) {
			MorphUtils.LOGGER.error("Invalid tag registration for NBT Editor: ", e);
			return null;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Tag> Supplier<T> createDefaultTag(TagType<T> type) {
		try {
			return (Supplier<T>) TAGS.get(type);
		} catch (Exception e) {
			MorphUtils.LOGGER.error("Invalid tag registration for NBT Editor: ", e);
			return null;
		}
	}

	@FunctionalInterface
	public interface TagRendererFactory<T extends Tag> {
		TagRenderer<T> create(String tagName, T tag, TagRendererContext<T> ctx);
	}

	//RENDERERS
	static {
		registerRenderer(CompoundTag.TYPE, CompoundTagRenderer::new);
		registerRenderer(StringTag.TYPE, StringTagRenderer::new);

		registerRenderer(IntTag.TYPE, NumericTagRenderer.IntTagRenderer::new);
		registerRenderer(LongTag.TYPE, NumericTagRenderer.LongTagRenderer::new);
		registerRenderer(DoubleTag.TYPE, NumericTagRenderer.DoubleTagRenderer::new);
		registerRenderer(FloatTag.TYPE, NumericTagRenderer.FloatTagRenderer::new);
		registerRenderer(ShortTag.TYPE, NumericTagRenderer.ShortTagRenderer::new);
		registerRenderer(ByteTag.TYPE, NumericTagRenderer.ByteTagRenderer::new);

		registerRenderer(ListTag.TYPE, CollectionTagRenderer.ListTagRenderer::new);
		registerRenderer(IntArrayTag.TYPE, CollectionTagRenderer.IntArrayTagRenderer::new);
		registerRenderer(LongArrayTag.TYPE, CollectionTagRenderer.LongArrayTagRenderer::new);
		registerRenderer(ByteArrayTag.TYPE, CollectionTagRenderer.ByteArrayTagRenderer::new);
	}

	//TAG GETTERS
	static {
		registerTagCreator(StringTag.TYPE, () -> StringTag.valueOf(""));
		registerTagCreator(CompoundTag.TYPE, CompoundTag::new);

		registerTagCreator(ListTag.TYPE, ListTag::new);
		registerTagCreator(IntArrayTag.TYPE, () -> new IntArrayTag(new int[0]));
		registerTagCreator(LongArrayTag.TYPE, () -> new LongArrayTag(new long[0]));
		registerTagCreator(ByteArrayTag.TYPE, () -> new ByteArrayTag(new byte[0]));

		registerTagCreator(IntTag.TYPE, () -> IntTag.valueOf(0));
		registerTagCreator(LongTag.TYPE, () -> LongTag.valueOf(0));
		registerTagCreator(DoubleTag.TYPE, () -> DoubleTag.valueOf(0));
		registerTagCreator(FloatTag.TYPE, () -> FloatTag.valueOf(0));
		registerTagCreator(ShortTag.TYPE, () -> ShortTag.valueOf((short) 0));
		registerTagCreator(ByteTag.TYPE, () -> ByteTag.valueOf((byte) 0));
	}
}
