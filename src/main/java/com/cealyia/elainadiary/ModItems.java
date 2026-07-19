package com.cealyia.elainadiary;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ElainaDiaryMod.MOD_ID);

    // 原有日记物品
    public static final RegistryObject<Item> ELAINA_DIARY =
        ITEMS.register("elaina_diary",
                () -> new ElainaDiaryItem(new Item.Properties().stacksTo(1))
        );

    // ===== 新增：伊蕾娜手记 =====
    public static final RegistryObject<Item> ELAINA_NOTE_PAPER =
        ITEMS.register("elainanotepaper",
                () -> new Item(new Item.Properties().stacksTo(64)) {
                    @Override
                    public Component getName(ItemStack stack) {
                        return Component.literal("伊蕾娜手记");
                    }
                    @Override
                    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                        super.appendHoverText(stack, level, tooltip, flag);
                        tooltip.add(Component.literal("伊蕾娜用魔法材料制造的特殊纸张"));
                    }
                });

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}