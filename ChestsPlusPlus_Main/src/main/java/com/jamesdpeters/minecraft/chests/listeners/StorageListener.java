package com.jamesdpeters.minecraft.chests.listeners;

import com.jamesdpeters.minecraft.chests.misc.Messages;
import com.jamesdpeters.minecraft.chests.misc.Utils;
import com.jamesdpeters.minecraft.chests.misc.Values;
import com.jamesdpeters.minecraft.chests.runnables.ChestLinkVerifier;
import com.jamesdpeters.minecraft.chests.serialize.Config;
import com.jamesdpeters.minecraft.chests.storage.abstracts.AbstractStorage;
import com.jamesdpeters.minecraft.chests.storage.abstracts.StorageInfo;
import com.jamesdpeters.minecraft.chests.storage.abstracts.StorageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;
import java.util.stream.Collectors;

public class StorageListener implements Listener {

    @EventHandler
    public void playerInteract(BlockPlaceEvent event){
            if(event.getBlockPlaced().getState() instanceof Sign){
                if(Config.getStorageTypes().stream().anyMatch(storageType -> storageType.isValidBlockType(event.getBlockAgainst()))) {
                        new TempListener() {
                            @EventHandler
                            public void onSignChange(SignChangeEvent signChangeEvent) {
                                if (event.getBlockPlaced().getLocation().equals(signChangeEvent.getBlock().getLocation())) {
                                    Sign sign = (Sign) signChangeEvent.getBlock().getState();

                                    for (StorageType storageType : Config.getStorageTypes().stream().filter(storageType -> storageType.isValidBlockType(event.getBlockAgainst())).collect(Collectors.toList())) {
                                        if(storageType.hasPermissionToAdd(event.getPlayer())) {
                                                Location signLocation = event.getBlockPlaced().getLocation();
                                                if (storageType.getStorageUtils().isValidSignPosition(signLocation)) {
                                                    StorageInfo info = storageType.getStorageUtils().getStorageInfo(sign, signChangeEvent.getLines(), event.getPlayer().getUniqueId());
                                                    if (info != null) {
                                                        if (!storageType.add(event.getPlayer(), info.getGroup(), event.getBlockAgainst().getLocation(), event.getBlockPlaced().getLocation(), info.getPlayer())) {
                                                            sign.getBlock().breakNaturally();
                                                            done();
                                                            return;
                                                        }
                                                        storageType.validate(event.getBlockAgainst());
                                                        storageType.getMessages().storageAdded(event.getPlayer(), signChangeEvent.getLine(1), info.getPlayer().getName());
                                                        signChange(sign, signChangeEvent, info.getPlayer(), event.getPlayer());
                                                    }
                                                } else {
                                                    storageType.getMessages().invalidSignPlacement(event.getPlayer());
                                                }
                                        } else {
                                            Messages.NO_PERMISSION(event.getPlayer());
                                        }
                                    }
                                }
                                done();
                            }
                        };
                }
            }
    }

    private void signChange(Sign sign, SignChangeEvent signChangeEvent, OfflinePlayer addedPlayer, Player player){
        setLine(sign, signChangeEvent, 0, ChatColor.RED + ChatColor.stripColor(signChangeEvent.getLine(0)));
        setLine(sign, signChangeEvent, 1, ChatColor.GREEN + ChatColor.stripColor(signChangeEvent.getLine(1)));
        setLine(sign, signChangeEvent, 2, ChatColor.BOLD + ChatColor.stripColor(addedPlayer.getName()));
        sign.getPersistentDataContainer().set(Values.playerUUID, PersistentDataType.STRING, addedPlayer.getUniqueId().toString());
        sign.update();
    }

    @EventHandler
    public void onSignBreak(BlockBreakEvent event){
        if(event.getBlock().getState() instanceof Sign) {
            Sign sign = (Sign) event.getBlock().getState();
            //Get blockface of sign.
            if(sign.getBlockData() instanceof Directional) {

                BlockFace chestFace = ((Directional) sign.getBlockData()).getFacing().getOppositeFace();
                Block block = sign.getBlock().getRelative(chestFace);

                Config.getStorageTypes().forEach(storageType -> {
                    if(storageType.isValidBlockType(block)){
                        StorageInfo info = storageType.getStorageUtils().getStorageInfo(sign,sign.getLines());
                        if(info != null){
                            storageType.removeBlock(info.getPlayer(), info.getGroup(), block.getLocation());
                            storageType.onSignRemoval(block);
                            storageType.getMessages().storageRemoved(event.getPlayer(), info.getGroup(), info.getPlayer().getName());
                        }
                    }
                });
            }
        }
    }

    @EventHandler
    public void onChestPlace(BlockPlaceEvent event){
        for(StorageType storageType : Config.getStorageTypes()){
            if(storageType.isValidBlockType(event.getBlockPlaced())){
                ItemMeta itemMeta = event.getItemInHand().getItemMeta();
                if(itemMeta != null){
                    String playerUUID = itemMeta.getPersistentDataContainer().get(Values.playerUUID, PersistentDataType.STRING);
                    String storageID = itemMeta.getPersistentDataContainer().get(Values.storageID, PersistentDataType.STRING);

                    if(playerUUID != null && storageID != null) {
                        OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(playerUUID));

                        BlockFace blockFace = storageType.onStoragePlacedBlockFace(event.getPlayer(), event.getBlockPlaced());
                        Block signSpace = event.getBlockPlaced().getRelative(blockFace);
                        if (!Utils.isAir(signSpace)) {
                            event.setCancelled(true);
                            return;
                        }
                        if (storageType.hasPermissionToAdd(event.getPlayer())) {
                            storageType.createStorageFacing(event.getPlayer(), owner, event.getBlockPlaced(), storageID, blockFace, false);
                        } else {
                            event.setCancelled(true);
                            Messages.NO_PERMISSION(event.getPlayer());
                            return;
                        }
                    }
                }
            }
        }
        if(event.getBlockPlaced().getState() instanceof Chest){
            new ChestLinkVerifier(event.getBlock()).check();
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChestBreak(BlockBreakEvent event){
        if(event.isCancelled()) return;
        for (StorageType storageType : Config.getStorageTypes()) {
            if(storageType.isValidBlockType(event.getBlock())) {
                boolean hasPickedUp = false;
                ItemStack mainHand = event.getPlayer().getInventory().getItemInMainHand();
                if(mainHand.containsEnchantment(Enchantment.SILK_TOUCH)){
                    hasPickedUp = true;
                }
                AbstractStorage storage = storageType.removeBlock(event.getBlock().getLocation(),hasPickedUp);
                if (storage != null) {
                    if(hasPickedUp){
                        event.setCancelled(true);
                        Block storageBlock = event.getBlock();
                        Location signLoc = storage.getSignLocation(storageBlock.getLocation());

                        //Custom dropped Chest
                        ItemStack customChest = new ItemStack(storageBlock.getType(),1);
                        ItemMeta itemMeta = customChest.getItemMeta();
                        if(itemMeta != null){
                            itemMeta.setDisplayName(ChatColor.AQUA+""+storageType.getSignTag()+" "+storage.getIdentifier());
                            itemMeta.getPersistentDataContainer().set(Values.playerUUID, PersistentDataType.STRING, storage.getOwner().getUniqueId().toString());
                            itemMeta.getPersistentDataContainer().set(Values.storageID, PersistentDataType.STRING, storage.getIdentifier());
                        }
                        customChest.setItemMeta(itemMeta);
                        storageBlock.getWorld().dropItemNaturally(storageBlock.getLocation(),customChest);

                        if(signLoc != null) signLoc.getBlock().setType(Material.AIR);
                        storageBlock.setType(Material.AIR);

                    } else {
                        storageType.getMessages().storageRemoved(event.getPlayer(), storage.getIdentifier(), storage.getOwner().getName());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPistonMove(BlockPistonExtendEvent event){
        event.getBlocks().forEach(block -> {
            for(StorageType storageType : Config.getStorageTypes()){
                if(storageType.isValidBlockType(block)){
                    Location blockLoc = block.getLocation();
                    AbstractStorage storage = storageType.getStorage(blockLoc);
                    if(storage != null) event.setCancelled(true);
                }
            }
        });
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event){
        event.getBlocks().forEach(block -> {
            for(StorageType storageType : Config.getStorageTypes()){
                if(storageType.isValidBlockType(block)){
                    Location blockLoc = block.getLocation();
                    AbstractStorage storage = storageType.getStorage(blockLoc);
                    if(storage != null) event.setCancelled(true);
                }
            }
        });
    }

    private void setLine(Sign sign, SignChangeEvent signChangeEvent, int i, String s){
        sign.setLine(i,s);
        signChangeEvent.setLine(i,s);
    }

}
