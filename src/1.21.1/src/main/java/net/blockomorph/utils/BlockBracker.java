package net.blockomorph.utils;

import net.blockomorph.utils.*;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import java.util.AbstractMap;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.Entity;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlockBracker {
    private final PlayerAccessor player;
    private final Player owner;
    private final BlockPos offset;
    private final BlockState blockstate;
    private final EntityDataAccessor<CompoundTag> PROGRESSES;
    private final SynchedEntityData entityData;
    public List<Player> players = new CopyOnWriteArrayList<>();
    private Player attacker; 
    private boolean braking;
    private boolean brake;
    private float progress;
    private float pie;
    private int animate = 0;
    
    public BlockBracker(PlayerAccessor player, BlockPos offset, BlockState state, SynchedEntityData entityData, EntityDataAccessor<CompoundTag> progress) {
    	this.player = player;
    	this.owner = (Player)player;
    	this.offset = offset;
    	this.blockstate = state;
    	this.PROGRESSES = progress;
    	this.entityData = entityData;
    	this.setProgress(-1);
    }

    public BlockPos getPos() {
    	return this.offset;
    }

    public void tick() {
    	if (this.braking) {
			if (player.getTnt() != null) {
				this.stopDestroy();
				return;
			}
   		    //progressCount
      	    if (this.brake) {
                this.progress += 1 / this.pie;
                while (this.progress >= 1.0f) {
                	this.setProgress(this.getProgress() + 1);
                    if (this.getProgress() > 9) {
                        this.destroy(this.getAttacker());
                    }
                    this.progress -= 1;
                }
      	    }
      	    //playerWorkHandler
      	    synchronized (this.players) {
      	    //for (Iterator<Player> iterator = this.players.iterator(); iterator.hasNext();) {
      	    for (Player pl : this.players) {
                //Player pl = iterator.next();
                PlayerAccessor pla = (PlayerAccessor)pl;
                AbstractMap.SimpleEntry<BlockPos, EntityHitResult> hit = MorphUtils.getPlayerLookedResult(pl, -1, 1);
                if (pla.readyForDestroy()) {
                	if (hit.getValue() == null || hit.getValue().getEntity() != owner) {
                		//iterator.remove();
                	    this.players.remove(pl);
                	} else {
                		BlockPos pos = hit.getKey();
                		if (!pos.equals(this.offset)) {
                			this.removePlayer(pl);
                			this.player.addPlayer(pos, pl);
                		}
                	}
                } else if (!pla.readyForDestroy()) {
             	    pla.setReady(true);
                }
            }
      	    }
            float min = Float.MAX_VALUE;
            for (Player pl : this.players) {
            	float time = this.getTime(this.blockstate, pl.blockPosition(), pl);
            	if (time < min) {
        	    	min = time;
        	    	this.attacker = pl;
        	    }
        	
            }
            this.setTimeFloat(min);
            //animate
        	if (this.animate % 4 == 0 && owner.level() instanceof ServerLevel lv) {
      		    for (Player pla : this.players) {
      			     pla.swing(InteractionHand.MAIN_HAND, true);
      		    } 
      		    SoundType soundtype = this.blockstate.getSoundType();
                owner.level().playSound(null, owner.blockPosition().offset(this.offset), soundtype.getHitSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 8.0F, soundtype.getPitch() * 0.5F);
          	}
      	    this.animate++;
      	    //emptyBrake
      	    if (this.players.isEmpty()) {
      		    this.stopDestroy();
      	    }
       }
    }

	private void destroy(Player attacker) {
		BlockState st = player.getBlockState();
		if (st.getBlock() instanceof TntBlock && player.getTnt() == null && st.getValue(BlockStateProperties.UNSTABLE)) {
			player.setTnt();
			if (owner.level() instanceof ServerLevel lv) {
				BlockPos pos = this.offset;
				BlockState val = this.blockstate;
				Player mob = this.owner;
				VoxelShape shape2 = val.getCollisionShape(lv, mob.blockPosition(), CollisionContext.of(mob));
				MorphUtils.particle(lv, mob.getX() + pos.getX(), mob.getY() + pos.getY(), mob.getZ() + pos.getZ(), val, shape2);
				SoundType soundtype = val.getSoundType();
				lv.playSound(null, mob.blockPosition().offset(pos), soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
			}
			return;
		}
		MorphUtils.destroy(player, attacker);
	}

    private String getKey() {
    	return this.offset.getX() +
    		" " +
    		this.offset.getY() +
    		" " +
    		this.offset.getZ();
    }

    private void setProgress(int i) {
    	CompoundTag tag = this.entityData.get(PROGRESSES);
    	tag = tag.copy();
    	tag.putInt(this.getKey(), i);
    	this.entityData.set(PROGRESSES, tag);
    }

    public int getProgress() {
    	CompoundTag tag = this.entityData.get(PROGRESSES);
    	if (!tag.contains(this.getKey(), 3)) return -1;
    	return tag.getInt(this.getKey());
    }

    public void stopDestroy() {
        this.braking = false;
      	this.setProgress(-1);
      	this.progress = 0;
      	this.pie = 0;
      	this.animate = 0;
      	this.brake = false;
    }

    public synchronized void removePlayer(Player pl) {
        if (this.players.contains(pl)) this.players.remove(pl);
    }

    @Nullable
    public Player getAttacker() {
    	return this.attacker;
    }

    private float getCoolDown(BlockState blockState, BlockPos blockPos, Player pl) {
        float progress = blockState.getDestroyProgress(pl, pl.level(), blockPos);
        if (Float.isInfinite(progress) || progress == 0) {
        	if (Float.isInfinite(progress)) {
        		this.destroy(pl);
        		pl.swing(InteractionHand.MAIN_HAND, true);
        	}
        	return -1;
        }
        return (float)(0.1F / progress);
    }

    private float getTime(BlockState blockState, BlockPos blockPos, Player pl) {
   	    float cooldown = this.getCoolDown(blockState, blockPos, pl);
    	if (cooldown == -1) return cooldown;
    	float time = cooldown / 20;
    	return time;
    }

    private void setTimeFloat(float time2) {
   	    if (time2 <= 0) return;
   	    float time = time2 * 10;
    	this.pie = time * 20 / 10;
    }

    private boolean setTime(BlockState blockState, BlockPos blockPos, Player pl) {
    	float cooldown = this.getCoolDown(blockState, blockPos, pl);
    	if (cooldown == -1) return false;
    	float time = cooldown / 20;
    	time = time * 10;
    	this.pie = time * 20 / 10;
    	return true;
    }

    public synchronized void addPlayer(Player pl) {
    	if (!this.players.contains(pl)) {
    		((PlayerAccessor)pl).setReady(false);
    		this.players.add(pl);
    	}
    	if (!this.braking) {
    	    this.setProgress(0);
    	    this.brake = this.setTime(this.blockstate, pl.blockPosition(), pl);
    	    this.braking = true;
    	}
    }
}
