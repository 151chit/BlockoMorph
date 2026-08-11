/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.blockomorph.utils.compat;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public class AtlasSpriteFinder {
	private final Node root = new Node(0.5f, 0.5f, 0.25f);

	public AtlasSpriteFinder(Map<Identifier, TextureAtlasSprite> sprites) {
		sprites.values().forEach(this.root::add);
	}

	public TextureAtlasSprite find(float u, float v) {
		return root.find(u, v);
	}

	private static class Node {
		static final float EPS = 0.00001f;
		final float midU;
		final float midV;
		final float cellRadius;

		@Nullable
		Object lowLow = null;
		@Nullable
		Object lowHigh = null;
		@Nullable
		Object highLow = null;
		@Nullable
		Object highHigh = null;

		Node(float midU, float midV, float radius) {
			this.midU = midU;
			this.midV = midV;
			cellRadius = radius;
		}

		void add(TextureAtlasSprite sprite) {
			if (sprite.getU0() < 0 - EPS || sprite.getU1() > 1 + EPS || sprite.getV0() < 0 - EPS || sprite.getV1() > 1 + EPS)
				return;

			final boolean lowU = sprite.getU0() < this.midU - EPS;
			final boolean highU = sprite.getU1() > this.midU + EPS;
			final boolean lowV = sprite.getV0() < this.midV - EPS;
			final boolean highV = sprite.getV1() > this.midV + EPS;

			if (lowU && lowV) {
				this.lowLow = addInner(sprite, this.lowLow, -1, -1);
			}

			if (lowU && highV) {
				this.lowHigh = addInner(sprite, this.lowHigh, -1, 1);
			}

			if (highU && lowV) {
				this.highLow = addInner(sprite, this.highLow, 1, -1);
			}

			if (highU && highV) {
				this.highHigh = addInner(sprite, this.highHigh, 1, 1);
			}
		}

		private Object addInner(TextureAtlasSprite sprite, @Nullable Object quadrant, int uStep, int vStep) {
			if (quadrant == null) {
				return sprite;
			} else if (quadrant instanceof Node node) {
				node.add(sprite);
				return quadrant;
			} else {
				Node n = new Node(this.midU + this.cellRadius * uStep, this.midV + this.cellRadius * vStep, this.cellRadius * 0.5f);

				if (quadrant instanceof TextureAtlasSprite prevSprite) {
					n.add(prevSprite);
				}

				n.add(sprite);
				return n;
			}
		}

		TextureAtlasSprite find(float u, float v) {
			if (u < this.midU) {
				return v < this.midV ? findInner(this.lowLow, u, v) : findInner(this.lowHigh, u, v);
			} else {
				return v < this.midV ? findInner(this.highLow, u, v) : findInner(this.highHigh, u, v);
			}
		}

		private TextureAtlasSprite findInner(@Nullable Object quadrant, float u, float v) {
			if (quadrant instanceof Node node) {
				return node.find(u, v);
			} else if (quadrant instanceof TextureAtlasSprite sprite) {
				return sprite;
			}
			return null;
		}
	}

	@FunctionalInterface
	public interface Holder {
		AtlasSpriteFinder getOrMake();
	}
}
