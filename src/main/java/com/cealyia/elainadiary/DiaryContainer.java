package com.cealyia.elainadiary;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class DiaryContainer extends AbstractContainerMenu {

    public final ItemStack diaryStack;

    public DiaryContainer(int windowId, Player player, ItemStack diaryStack) {
        super(ModContainers.DIARY_CONTAINER.get(), windowId);
        this.diaryStack = diaryStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getMainHandItem().getItem() == ModItems.ELAINA_DIARY.get();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}