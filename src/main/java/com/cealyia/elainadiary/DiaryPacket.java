package com.cealyia.elainadiary;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class DiaryPacket {

    private final int day;
    private final String content;
    private final boolean submit;

    public DiaryPacket(int day, String content, boolean submit) {
        this.day = day;
        this.content = content;
        this.submit = submit;
    }

    public static void encode(DiaryPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.day);
        buf.writeUtf(msg.content);
        buf.writeBoolean(msg.submit);
    }

    public static DiaryPacket decode(FriendlyByteBuf buf) {
        return new DiaryPacket(buf.readInt(), buf.readUtf(), buf.readBoolean());
    }
    
    private static final List<ItemStack> REWARDS = Arrays.asList(
            new ItemStack(Items.EMERALD, 2),
            new ItemStack(Items.DIAMOND, 1),
            new ItemStack(Items.GOLD_INGOT, 3),
            new ItemStack(Items.IRON_INGOT, 5),
            new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1),
            new ItemStack(Items.EXPERIENCE_BOTTLE, 3),
            new ItemStack(Items.ENDER_PEARL, 2),
            new ItemStack(Items.NAME_TAG, 1)
    );

    public static void handle(DiaryPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ItemStack stack = findDiary(player);
            if (stack.isEmpty()) return;
            if (msg.day <= 0) return;

            CompoundTag tag = stack.getOrCreateTag();
            
            CompoundTag allPages = tag.getCompound("diary_all_pages");
            ListTag list = new ListTag();
            for (String line : msg.content.split("\n", -1)) {
                list.add(StringTag.valueOf(line));
            }
            allPages.put(String.valueOf(msg.day), list);
            tag.put("diary_all_pages", allPages);

            if (msg.submit) {
                tag.putString("status_" + msg.day, "submitted");
                if (!tag.getBoolean("reward_given_" + msg.day)) {
                    tag.putBoolean("reward_given_" + msg.day, true);
                    giveReward(player);
                    player.sendSystemMessage(Component.literal("🎁 获得魔女之旅的馈赠！"));
                }
            }

            stack.setTag(tag);
        });
        ctx.get().setPacketHandled(true);
    }

    private static ItemStack findDiary(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() == ModItems.ELAINA_DIARY.get()) return main;
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() == ModItems.ELAINA_DIARY.get()) return s;
        }
        return ItemStack.EMPTY;
    }

    private static void giveReward(ServerPlayer player) {
        Random random = new Random();
        ItemStack reward = REWARDS.get(random.nextInt(REWARDS.size())).copy();
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
    }
}