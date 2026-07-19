package com.cealyia.elainadiary;

import com.mojang.logging.LogUtils;

import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ElainaDiaryMod.MOD_ID)
public class ElainaDiaryMod {

    public static final String MOD_ID = "elainadiary";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ElainaDiaryMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 暂时先不注册物品，等 ModItems 创建好后再加这行
        ModItems.register(modEventBus);
        ModContainers.register(modEventBus);
        
        ModNetwork.register();

        LOGGER.info("魔女之旅模组正在加载...");
    }
}