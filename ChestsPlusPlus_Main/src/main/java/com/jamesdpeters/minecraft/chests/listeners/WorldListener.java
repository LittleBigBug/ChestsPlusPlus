package com.jamesdpeters.minecraft.chests.listeners;

import com.jamesdpeters.minecraft.chests.ChestsPlusPlus;
import com.jamesdpeters.minecraft.chests.misc.Utils;
import com.jamesdpeters.minecraft.chests.serialize.Config;
import com.jamesdpeters.minecraft.chests.serialize.ConfigStorage;
import com.jamesdpeters.minecraft.chests.serialize.LocationInfo;
import com.jamesdpeters.minecraft.chests.storage.abstracts.AbstractStorage;
import com.jamesdpeters.minecraft.chests.storage.abstracts.StorageType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class WorldListener implements Listener {

    private static boolean justSaved = false;

    @EventHandler
    public void onWorldSave(WorldSaveEvent event){
        if(!justSaved){
            Config.saveASync();
            justSaved = true;
            new BukkitRunnable(){
                @Override
                public void run() {
                    justSaved = false;
                }
            }.runTaskLater(ChestsPlusPlus.PLUGIN,20);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event){
        Utils.removeEntities(event.getWorld());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event){
        if(!event.isNewChunk()){
            Utils.fixEntities(event.getChunk());
            Config.getStorageTypes().forEach(storageType -> {
                storageType.getStorageMap().values().forEach(stringHashMap -> {
                    stringHashMap.values().forEach(o -> {
                        o.getLocations().forEach(locationInfo -> {
                            if(locationInfo != null && locationInfo.getSignLocation() != null && locationInfo.getSignLocation().getChunk().equals(event.getChunk())){
                                o.updateClient(locationInfo);
                            }
                        });
                    });
                });
            });
        }
    }
}
