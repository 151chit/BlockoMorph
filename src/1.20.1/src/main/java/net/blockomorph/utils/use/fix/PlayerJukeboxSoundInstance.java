package net.blockomorph.utils.use.fix;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.Pair;
import net.blockomorph.utils.PlayerAccessor;
import net.blockomorph.utils.use.UseController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.block.Blocks;

import java.util.HashMap;

public class PlayerJukeboxSoundInstance extends AbstractTickableSoundInstance {
    private static final HashMap<Pair<Integer, BlockPos>, PlayerJukeboxSoundInstance> songs = Maps.newHashMap();
    private final Pair<Integer, BlockPos> data;
    private boolean active = true;

    private PlayerJukeboxSoundInstance(RecordItem recordItem, Pair<Integer, BlockPos> info) {
        super(recordItem.getSound(), SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.data = info;
        this.volume = 4;
    }

    @Override
    public void tick() {
        ClientLevel lv = Minecraft.getInstance().level;
        if (lv == null) {
            songs.clear();
            this.stop();
            return;
        }
        Entity entity = lv.getEntity(this.data.key());
        BlockPos offset = this.data.value();
        if (entity instanceof PlayerAccessor pl) {
            UseController ctr = pl.getUseControllers().get(offset);
            if (ctr.getBlockState().getBlock() == Blocks.JUKEBOX) {
                this.x = (float) entity.getX() + offset.getX();
                this.y = (float) entity.getY() + offset.getY();
                this.z = (float) entity.getZ() + offset.getZ();
                this.active = true;
            } else active = false;
        } else active = false;
    }

    @Override
    public float getVolume() {
        if (!active) {
            this.y = -10000;
            return 0f;
        }
        return super.getVolume();
    }

    public static void play(UseController pl, int itemId) {
        Item item = Item.byId(itemId);
        ClientLevel lv = Minecraft.getInstance().level;
        if (item instanceof RecordItem recorditem && lv != null && pl != null) {
            Pair<Integer, BlockPos> pair = Pair.of(pl.getOwner().getId(), pl.getOffset());
            if (!songs.containsKey(pair)) {
                PlayerJukeboxSoundInstance music = new PlayerJukeboxSoundInstance(recorditem, pair);
                songs.put(pair, music);
                Minecraft.getInstance().getSoundManager().play(music);
                Minecraft.getInstance().gui.setNowPlaying(recorditem.getDisplayName());
            }
        }
    }

    public static void stop(UseController pl) {
        Pair<Integer, BlockPos> pair = Pair.of(pl.getOwner().getId(), pl.getOffset());
        PlayerJukeboxSoundInstance inst = songs.get(pair);
        if (inst != null) {
            inst.stop();
        }
        songs.remove(pair);
    }

    public static void stopAll(int playerId) {
        songs.entrySet().removeIf(entry -> {
            if (entry.getKey().key() == playerId) {
                PlayerJukeboxSoundInstance soundInstance = entry.getValue();
                soundInstance.stop();
                return true;
            }
            return false;
        });
    }
}
