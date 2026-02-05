package net.blockomorph.screens.morphConfig.nbtEditor;

import java.util.ArrayList;
import java.util.List;

public class NbtPath {
	private final String part;
	protected NbtPath walk;

	private NbtPath(String part) {
		this.part = part;
	}

	public NbtPath append(String name) {
		if (this.walk != null) {
			this.walk.append(name);
		} else this.walk = new NbtPath(name);
		return this;
	}

	public NbtPath back() {
		if (this.walk != null) {
			if (this.walk.walk == null) {
				this.walk = null;
			} else this.walk.back();
		}
		return this;
	}

	public List<String> constructPath(List<String> folders) {
		folders.add(this.part);
		if (this.walk != null) {
			this.walk.constructPath(folders);
		}
		return folders;
	}

	public String getVisualString() {
		List<String> folders = this.constructPath(new ArrayList<>());
		StringBuilder root = new StringBuilder();
		for (String folder : folders) {
			root.append("/").append(folder);
		}
		return root.toString();
	}

	@Override
	public String toString() {
		return this.getVisualString();
	}

	public boolean isEmpty() {
		return this.walk == null;
	}

	public static final class RootNbtPath extends NbtPath {

		public RootNbtPath() {
			super(null);
		}

		@Override
		public List<String> constructPath(List<String> folders) {
			if (this.walk != null) {
				this.walk.constructPath(folders);
			}
			return folders;
		}
	}
}
