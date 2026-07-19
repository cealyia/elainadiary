package com.cealyia.elainadiary;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;  // ← 添加这一行
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModContainers {

    public static final DeferredRegister<MenuType<?>> CONTAINERS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ElainaDiaryMod.MOD_ID);

    // 注册我们的日记容器，工厂方法接收 windowId、inv、data
    public static final RegistryObject<MenuType<DiaryContainer>> DIARY_CONTAINER =
            CONTAINERS.register("diary_container",
                    () -> IForgeMenuType.create((windowId, inv, data) -> {
                        // data 是 ItemStack 的额外数据，我们直接使用玩家主手中的物品
                        ItemStack stack = inv.player.getMainHandItem();
                        return new DiaryContainer(windowId, inv.player, stack);
                    })
            );

    public static void register(IEventBus eventBus) {
        CONTAINERS.register(eventBus);
    }
}