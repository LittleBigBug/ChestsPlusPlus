package com.jamesdpeters.minecraft.chests.api;

import com.jamesdpeters.minecraft.chests.API;
import com.jamesdpeters.minecraft.chests.ChestOpener;
import com.jamesdpeters.minecraft.chests.NMSProvider;
import com.jamesdpeters.minecraft.chests.MaterialChecker;
import org.bukkit.Bukkit;


public class ApiSpecific {

    private static MaterialChecker materialChecker;
    private static ChestOpener chestOpener;
    private static NMSProvider nmsProvider;

    public static void init(){
        nmsProvider = API.getNMSProvider();
        if(nmsProvider == null) nmsProvider = new NMSProviderDefault();
        materialChecker = nmsProvider.getMaterialChecker();
        chestOpener = nmsProvider.getChestOpener();
    }

    public static MaterialChecker getMaterialChecker() {
        return materialChecker;
    }
    public static ChestOpener getChestOpener() {
        return chestOpener;
    }
    public static NMSProvider getNmsProvider() {
        return nmsProvider;
    }
    public static String getApiVersion(){ return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]; }
}
