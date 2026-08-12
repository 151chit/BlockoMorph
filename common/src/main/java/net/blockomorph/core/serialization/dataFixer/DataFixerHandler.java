package net.blockomorph.core.serialization.dataFixer;

import net.blockomorph.core.serialization.PlayerWorldSerializer;
import net.blockomorph.utils.MorphUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class DataFixerHandler {
	private static final List<DataFixer> FIXERS = List.of(new FixerV2(), new FixerV1());

	public static void checkAndFix(MinecraftServer sv, UUID playerId, RegistryAccess registry, Function<String, CompoundTag> rootTag) {
		CompoundTag newTg = null;
		for (DataFixer fixer : FIXERS) {
			newTg = fixer.fixOrNull(playerId, registry, rootTag);
			if (newTg != null) break;
		}
		if (newTg != null) {
			var path = sv.getWorldPath(LevelResource.ROOT).resolve(PlayerWorldSerializer.SAVE_DIR).resolve(playerId + ".dat");
			try {
				if (PlayerWorldSerializer.inSerializeProcess(playerId))
					throw new IOException("Mix old tag into player data when player loading");
				Files.createDirectories(path.getParent());
				NbtIo.writeCompressed(newTg, path);
			} catch (IOException err) {
				MorphUtils.LOGGER.error("Cannot migrate old data, old tag for manual input to {} with nbt editor: {}", path, newTg, err);
			}
		}
	}

	static abstract class DataFixer {

		private CompoundTag fixOrNull(UUID playerId, RegistryAccess registry, Function<String, CompoundTag> rootPlayerDataTag) {
			CompoundTag candidate = rootPlayerDataTag.apply(this.oldTagRootName());
			if (candidate != null) return this.tryFixRootTag(playerId, registry, candidate);
			return null;
		}

		abstract String oldTagRootName();
		abstract CompoundTag tryFixRootTag(UUID playerId, RegistryAccess registry, CompoundTag oldTag);
	}
}
