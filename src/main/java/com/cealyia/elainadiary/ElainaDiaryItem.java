package com.cealyia.elainadiary;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

public class ElainaDiaryItem extends Item {

    public ElainaDiaryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            net.minecraft.nbt.CompoundTag tag = itemStack.getOrCreateTag();
            if (!tag.contains("diary_day")) {
                tag.putInt("diary_day", 1);
                tag.putLong("diary_last_world_day", level.getGameTime() / 24000L);
                itemStack.setTag(tag);
            }
        } else {
            // 使用 DistExecutor 延迟加载客户端 GUI 类，避免服务端类加载失败
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                net.minecraft.client.Minecraft.getInstance().setScreen(
                    new DiaryScreen(player, itemStack)
                );
            });
        }
        
        return InteractionResultHolder.success(itemStack);
    }

    // ===== 新增：硬编码显示名 =====
    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("《魔女之旅》");
    }

    // ===== 新增：硬编码描述（悬停提示） =====
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("伊蕾娜模仿母亲给的日记重新制作的《魔女之旅》，用来记载自己新的故事。"));
    }
}