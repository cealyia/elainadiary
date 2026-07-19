package com.cealyia.elainadiary;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ElainaDiaryMod.MOD_ID)
public class ModEvents {

    // ========== 带权重的奖励池 ==========
    private static class WeightedReward {
        final ItemStack stack;
        final int weight;
        WeightedReward(ItemStack stack, int weight) {
            this.stack = stack;
            this.weight = weight;
        }
    }

    private static final List<WeightedReward> REWARDS = new ArrayList<>();
    private static final Random RANDOM = new Random();

    static {
        // ---- 基础材料（权重较高） ----
        REWARDS.add(new WeightedReward(new ItemStack(Items.EMERALD, 2), 30));           // 2 个绿宝石
        REWARDS.add(new WeightedReward(new ItemStack(Items.IRON_INGOT, 5), 30));        // 5 个铁锭
        REWARDS.add(new WeightedReward(new ItemStack(Items.GOLD_INGOT, 3), 25));        // 3 个金锭
        REWARDS.add(new WeightedReward(new ItemStack(Items.COPPER_INGOT, 8), 20));      // 8 个铜锭
        REWARDS.add(new WeightedReward(new ItemStack(Items.AMETHYST_SHARD, 4), 20));    // 4 个紫水晶碎片
        REWARDS.add(new WeightedReward(new ItemStack(Items.PRISMARINE_SHARD, 6), 15));  // 6 个海晶石碎片

        // ---- 中等稀有（权重中等） ----
        REWARDS.add(new WeightedReward(new ItemStack(Items.DIAMOND, 1), 15));           // 1 个钻石
        REWARDS.add(new WeightedReward(new ItemStack(Items.ENDER_PEARL, 2), 15));       // 2 个末影珍珠
        REWARDS.add(new WeightedReward(new ItemStack(Items.GOLDEN_APPLE, 2), 12));      // 2 个金苹果
        REWARDS.add(new WeightedReward(new ItemStack(Items.EXPERIENCE_BOTTLE, 3), 12)); // 3 瓶经验瓶
        REWARDS.add(new WeightedReward(new ItemStack(Items.BOOK, 1), 12));              // 1 本书
        REWARDS.add(new WeightedReward(new ItemStack(Items.NAME_TAG, 1), 12));          // 1 个命名牌
        REWARDS.add(new WeightedReward(new ItemStack(Items.HONEY_BOTTLE, 3), 10));      // 3 瓶蜂蜜
        REWARDS.add(new WeightedReward(new ItemStack(Items.COOKIE, 16), 8));            // 16 个曲奇

        // ---- 稀有物品（权重较低） ----
        REWARDS.add(new WeightedReward(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1), 5));  // 附魔金苹果
        REWARDS.add(new WeightedReward(new ItemStack(Items.TOTEM_OF_UNDYING, 1), 5));        // 不死图腾
        REWARDS.add(new WeightedReward(new ItemStack(Items.NETHERITE_SCRAP, 2), 5));        // 2 个下界合金残骸
        REWARDS.add(new WeightedReward(new ItemStack(Items.SHULKER_SHELL, 1), 4));          // 1 个潜影壳
        REWARDS.add(new WeightedReward(new ItemStack(Items.DRAGON_BREATH, 1), 4));          // 1 瓶龙息
        REWARDS.add(new WeightedReward(new ItemStack(Items.WITHER_SKELETON_SKULL, 1), 3));  // 凋灵骷髅头颅
        REWARDS.add(new WeightedReward(new ItemStack(Items.CREEPER_HEAD, 1), 3));           // 苦力怕头颅

        // ---- 极稀有（权重极低） ----
        REWARDS.add(new WeightedReward(new ItemStack(Items.NETHERITE_INGOT, 1), 2));        // 1 个下界合金锭
        REWARDS.add(new WeightedReward(new ItemStack(Items.DRAGON_HEAD, 1), 1));            // 龙首
        REWARDS.add(new WeightedReward(new ItemStack(Items.EMERALD_BLOCK, 1), 2));          // 1 个绿宝石块
        REWARDS.add(new WeightedReward(new ItemStack(Items.DIAMOND_BLOCK, 1), 1));          // 1 个钻石块
        REWARDS.add(new WeightedReward(new ItemStack(Items.NETHERITE_BLOCK, 1), 1));        // 1 个下界合金块

        // ---- 音乐唱片（权重低） ----
        REWARDS.add(new WeightedReward(new ItemStack(Items.MUSIC_DISC_13, 1), 3));
        REWARDS.add(new WeightedReward(new ItemStack(Items.MUSIC_DISC_CAT, 1), 3));
        REWARDS.add(new WeightedReward(new ItemStack(Items.MUSIC_DISC_WARD, 1), 2));
        REWARDS.add(new WeightedReward(new ItemStack(Items.MUSIC_DISC_PIGSTEP, 1), 2));
    }

    // ========== 原有事件监听保持不变 ==========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level level = player.level();
        if (level.isClientSide) return;

        if (player.tickCount % 20 != 0) return;

        long currentWorldDay = level.getGameTime() / 24000L;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == ModItems.ELAINA_DIARY.get()) {
                CompoundTag tag = stack.getOrCreateTag();

                if (!tag.contains("diary_last_world_day")) continue;

                long lastWorldDay = tag.getLong("diary_last_world_day");
                if (currentWorldDay > lastWorldDay) {
                    int diaryDay = tag.getInt("diary_day");
                    diaryDay += 1;
                    tag.putInt("diary_day", diaryDay);
                    tag.putLong("diary_last_world_day", currentWorldDay);
                    stack.setTag(tag);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (level.isClientSide) return;

        int today = (int) (level.getDayTime() / 24000);

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == ModItems.ELAINA_DIARY.get()) {
                var tag = stack.getOrCreateTag();
                int diaryDay = tag.getInt("diary_day");
                String status = tag.getString("diary_status");

                if (diaryDay < today) {
                    if ("draft".equals(status)) {
                        tag.putString("diary_status", "submitted");
                        giveReward((ServerPlayer) player);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "🌙 你在睡梦中写完了前一天的日记，获得了魔女之旅的馈赠！"
                        ));
                    }
                    if (tag.contains("diary_pages")) {
                        var pages = tag.getList("diary_pages", net.minecraft.nbt.Tag.TAG_STRING);
                        if (pages.isEmpty() || (pages.size() == 1 && pages.getString(0).isEmpty())) {
                            tag.remove("diary_pages");
                            tag.remove("diary_day");
                            tag.remove("diary_status");
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "📭 前一天的日记是空白的，已被自动清理。"
                            ));
                        }
                    }
                    stack.setTag(tag);
                }
            }
        }
    }

    // ========== 带权重的奖励发放 ==========
    private static void giveReward(ServerPlayer player) {
        // 计算总权重
        int totalWeight = 0;
        for (WeightedReward wr : REWARDS) {
            totalWeight += wr.weight;
        }

        // 随机选择
        int roll = RANDOM.nextInt(totalWeight);
        int cumulative = 0;
        ItemStack selected = null;
        for (WeightedReward wr : REWARDS) {
            cumulative += wr.weight;
            if (roll < cumulative) {
                selected = wr.stack.copy();
                break;
            }
        }

        if (selected == null) {
            // 安全回退（理论上不会发生）
            selected = new ItemStack(Items.EMERALD, 1);
        }

        // 发给玩家
        if (!player.getInventory().add(selected)) {
            player.drop(selected, false);
        }
    }
}