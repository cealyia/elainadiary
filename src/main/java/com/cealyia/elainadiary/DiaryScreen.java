package com.cealyia.elainadiary;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public class DiaryScreen extends Screen {

    private final ItemStack diaryStack;
    private final Player player;
    private final Map<Integer, List<String>> diaryPages = new LinkedHashMap<>();
    private final List<Integer> availableDays = new ArrayList<>();
    private int viewingDay = -1;
    private List<String> currentLines = new ArrayList<>();
    private String status = "draft";
    private int leftPanelWidth = 60;
    private int textLeft, textTop, textBottom;
    private static final int LINE_HEIGHT = 12;
    private Button submitButton;
    private int today;
    private boolean rewardGiven = false;
    private boolean showingDirectory = false;
    private MultiLineEditBox diaryEditBox;

    private int directoryScrollOffset = 0;
    private int directoryPage = 0;
    private static final int DAYS_PER_PAGE = 10;
    
    // 编辑框尺寸
    private int editBoxWidth;
    private int editBoxHeight;
    private int editBoxLeft;
    private int editBoxTop;

    public DiaryScreen(Player player, ItemStack diaryStack) {
        super(Component.literal("魔女之旅日记"));
        this.player = player;
        this.diaryStack = diaryStack;
        loadAllFromNBT(diaryStack);
        this.today = getCurrentDay();

        if (!diaryPages.containsKey(today)) {
            diaryPages.put(today, new ArrayList<>());
            availableDays.add(today);
            Collections.sort(availableDays);
        }

        this.viewingDay = today;
        loadDay(viewingDay);
        this.showingDirectory = false;
        this.directoryScrollOffset = 0;
        this.directoryPage = 0;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // ===== 计算编辑框尺寸（更宽敞） =====
        editBoxWidth = Math.min(420, (int)(this.width * 0.55));   // 宽度占屏幕55%，最多420
        editBoxHeight = Math.min(200, (int)(this.height * 0.45)); // 高度占屏幕45%，最多200
        editBoxLeft = centerX - editBoxWidth / 2 + leftPanelWidth / 2 + 5; // 居中偏右，留出目录空间
        editBoxTop = centerY - editBoxHeight / 2 - 5;

        // 更新文本区域边界（用于显示历史记录）
        textLeft = editBoxLeft;
        textTop = editBoxTop;
        textBottom = editBoxTop + editBoxHeight;

        if (showingDirectory) {
            // ---------- 目录视图 ----------
            int totalDays = availableDays.size();
            int startIdx = directoryPage * DAYS_PER_PAGE;
            int endIdx = Math.min(startIdx + DAYS_PER_PAGE, totalDays);
            List<Integer> pageDays = new ArrayList<>();
            if (startIdx < totalDays) {
                pageDays = availableDays.subList(startIdx, endIdx);
            }

            int yOffset = 40;
            int buttonWidth = 120;
            int buttonHeight = 20;
            for (int day : pageDays) {
                boolean isSubmitted = diaryStack.getOrCreateTag().getString("status_" + day).equals("submitted");
                String label = "第" + day + "天" + (isSubmitted ? " ✅" : "");
                int finalDay = day;
                addRenderableWidget(Button.builder(
                        Component.literal(label),
                        button -> {
                            saveCurrentDay();
                            showingDirectory = false;
                            viewingDay = finalDay;
                            loadDay(finalDay);
                            clearWidgets();
                            init();
                        }
                ).bounds(centerX - buttonWidth/2, yOffset, buttonWidth, buttonHeight).build());
                yOffset += buttonHeight + 4;
                if (yOffset > this.height - 80) break;
            }

            if (pageDays.isEmpty()) {
                addRenderableWidget(Button.builder(
                        Component.literal("📭 暂无日记记录"),
                        button -> {}
                ).bounds(centerX - 60, centerY - 10, 120, 20).build());
            }

            int totalPages = (int) Math.ceil((double) totalDays / DAYS_PER_PAGE);
            if (totalPages > 1) {
                int pageY = this.height - 30;
                if (directoryPage > 0) {
                    addRenderableWidget(Button.builder(
                            Component.literal("◀ 上一页"),
                            button -> {
                                directoryPage--;
                                clearWidgets();
                                init();
                            }
                    ).bounds(centerX - 100, pageY, 60, 20).build());
                }
                addRenderableWidget(Button.builder(
                        Component.literal((directoryPage + 1) + "/" + totalPages),
                        button -> {}
                ).bounds(centerX - 20, pageY, 40, 20).build());
                if (directoryPage < totalPages - 1) {
                    addRenderableWidget(Button.builder(
                            Component.literal("下一页 ▶"),
                            button -> {
                                directoryPage++;
                                clearWidgets();
                                init();
                            }
                    ).bounds(centerX + 40, pageY, 60, 20).build());
                }
            }

            addRenderableWidget(Button.builder(
                    Component.literal("✕ 关闭"),
                    button -> onClose()
            ).bounds(this.width - 60, 10, 50, 20).build());

        } else {
            // ---------- 日记视图 ----------
            
            // 标题（顶部居中）
            String title = "📖 第 " + viewingDay + " 天";
            if ("submitted".equals(status)) title += " ✅";
            if (viewingDay != today && viewingDay >= 0) title += " (历史)";
            // 直接绘制在渲染中，不需要按钮

            // 目录按钮（左上角）
            addRenderableWidget(Button.builder(
                    Component.literal("📂 目录"),
                    button -> {
                        saveCurrentDay();
                        showingDirectory = true;
                        directoryPage = 0;
                        clearWidgets();
                        init();
                    }
            ).bounds(10, 10, 50, 20).build());

            // 左侧目录面板（靠左显示）
            int panelLeft = 10;
            int panelTop = 40;
            int panelWidth = 55;
            int panelHeight = editBoxHeight + 20;
            
            // 绘制目录面板背景（在render中绘制）
            // 这里只初始化按钮

            // 提交按钮（右下角）
            boolean alreadySubmitted = "submitted".equals(status);
            boolean isToday = viewingDay == today;
            boolean canSubmit = isToday && !alreadySubmitted;

            int buttonY = editBoxTop + editBoxHeight + 8;
            int buttonWidth = 100;
            int buttonHeight = 20;

            submitButton = addRenderableWidget(Button.builder(
                    Component.literal(alreadySubmitted ? "✅ 今日已完成" : (canSubmit ? "✅ 完成今日旅行日记" : "⏳ 今日已完成")),
                    button -> submitDiary()
            ).bounds(editBoxLeft + editBoxWidth - buttonWidth - 10, buttonY, buttonWidth, buttonHeight).build());

            if (!canSubmit) {
                submitButton.active = false;
            }

            // 保存按钮（右下角，提交按钮左边）
            Button saveButton = addRenderableWidget(Button.builder(
                    Component.literal("💾 暂时停笔"),
                    button -> {
                        saveCurrentDay();
                        onClose();
                    }
            ).bounds(editBoxLeft + editBoxWidth - buttonWidth * 2 - 20, buttonY, buttonWidth, buttonHeight).build());

            if (!isToday) {
                saveButton.active = false;
            }
            
            // 创建编辑框
            boolean canEditNow = isToday && !alreadySubmitted;
            if (canEditNow) {
                diaryEditBox = new MultiLineEditBox(
                        this.font, editBoxLeft, editBoxTop, editBoxWidth, editBoxHeight,
                        Component.literal("在此书写今日日记..."),
                        Component.literal("日记"));
                diaryEditBox.setCharacterLimit(2000);
                diaryEditBox.setValue(String.join("\n", currentLines));
                addRenderableWidget(diaryEditBox);
                setInitialFocus(diaryEditBox);
            } else {
                diaryEditBox = null;
            }
        }
    }

    private void loadDay(int day) {
        if (diaryPages.containsKey(day)) {
            this.viewingDay = day;
            this.currentLines = new ArrayList<>(diaryPages.get(day));
            if (currentLines.isEmpty()) {
                currentLines.add("");
            }
            CompoundTag tag = diaryStack.getOrCreateTag();
            String key = "status_" + day;
            this.status = tag.getString(key);
            if (status.isEmpty()) status = "draft";
            this.rewardGiven = tag.getBoolean("reward_given_" + day);
        }
    }

    private void saveCurrentDay() {
        if (diaryEditBox != null) {
            String text = diaryEditBox.getValue();
            currentLines = new ArrayList<>(Arrays.asList(text.split("\n", -1)));
            if (currentLines.isEmpty()) currentLines.add("");
        }
        if (viewingDay >= 0 && !currentLines.isEmpty()) {
            while (currentLines.size() > 1 && currentLines.get(currentLines.size() - 1).isEmpty()) {
                currentLines.remove(currentLines.size() - 1);
            }
            diaryPages.put(viewingDay, new ArrayList<>(currentLines));
            if (!availableDays.contains(viewingDay)) {
                availableDays.add(viewingDay);
                Collections.sort(availableDays);
            }
            saveAllToNBT(diaryStack);
            ModNetwork.CHANNEL.sendToServer(
                    new DiaryPacket(viewingDay, String.join("\n", currentLines), false));
        }
    }

    private void submitDiary() {
        if (today < 0 || viewingDay != today) return;
        if (rewardGiven) return;

        saveCurrentDay();

        CompoundTag tag = diaryStack.getOrCreateTag();
        tag.putString("status_" + today, "submitted");
        tag.putBoolean("reward_given_" + today, true);
        diaryStack.setTag(tag);
        this.status = "submitted";
        this.rewardGiven = true;

        ModNetwork.CHANNEL.sendToServer(
                new DiaryPacket(today, String.join("\n", currentLines), true));

        if (submitButton != null) {
            submitButton.setMessage(Component.literal("✅ 今日已完成"));
            submitButton.active = false;
        }

        player.sendSystemMessage(Component.literal("📖 今日旅行日记已完成！获得魔女之旅的馈赠 ✨"));
        onClose();
    }

    // ---------- 渲染 ----------
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        if (showingDirectory) {
            guiGraphics.drawCenteredString(font, "📖 魔女之旅日记 - 目录", centerX, 20, 0xFF000000);
            String totalInfo = "共 " + availableDays.size() + " 篇日记";
            guiGraphics.drawCenteredString(font, totalInfo, centerX, this.height - 50, 0xFF888888);
        } else {
            // ===== 左侧目录面板 =====
            int panelLeft = 8;
            int panelTop = 38;
            int panelWidth = 58;
            int panelHeight = editBoxHeight + 24;
            
            // 面板背景
            guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xDD333333);
            guiGraphics.fill(panelLeft + 1, panelTop + 1, panelLeft + panelWidth - 1, panelTop + panelHeight - 1, 0xDD555555);
            
            // 面板标题
            guiGraphics.drawString(font, "📖", panelLeft + 4, panelTop + 4, 0xFFFFFF, false);
            guiGraphics.drawString(font, "目录", panelLeft + 20, panelTop + 4, 0xFFFFFF, false);
            
            // 目录列表
            int y = panelTop + 24;
            int maxY = panelTop + panelHeight - 4;
            int totalDays = availableDays.size();
            int startIndex = Math.min(directoryScrollOffset, Math.max(0, totalDays - 8));
            if (startIndex < 0) startIndex = 0;

            for (int i = startIndex; i < totalDays && y <= maxY; i++) {
                int day = availableDays.get(i);
                boolean isSubmitted = diaryStack.getOrCreateTag().getString("status_" + day).equals("submitted");
                String display = "第" + day + "天" + (isSubmitted ? "✓" : "");
                int color = (day == viewingDay) ? 0x66FF66 : 0xCCCCCC;
                guiGraphics.drawString(font, display, panelLeft + 4, y, color, false);
                y += 14;
            }
            
            // 滚动提示
            if (totalDays > 8) {
                String hint = directoryScrollOffset > 0 ? "↑" : "↓";
                guiGraphics.drawString(font, hint, panelLeft + panelWidth - 10, panelTop + panelHeight - 12, 0x888888, false);
            }

            // ===== 标题 =====
            String title = "📖 第 " + viewingDay + " 天";
            if ("submitted".equals(status)) title += " ✅";
            if (viewingDay != today && viewingDay >= 0) title += " (历史)";
            guiGraphics.drawCenteredString(font, title, centerX, 16, 0xFFFFFF);

            // ===== 状态提示 =====
            if (viewingDay != today && viewingDay >= 0) {
                guiGraphics.drawCenteredString(font, "📌 查看历史记录（只读）", centerX, editBoxTop + editBoxHeight + 32, 0xFF888888);
            } else if ("submitted".equals(status)) {
                guiGraphics.drawCenteredString(font, "今日已完成，不可修改", centerX, editBoxTop + editBoxHeight + 32, 0x88FF88);
            }
            
            // 如果不可编辑，显示历史内容
            boolean canEdit = viewingDay == today && !"submitted".equals(status);
            if (!canEdit && diaryEditBox == null) {
                int yPos = editBoxTop + 4;
                for (int i = 0; i < currentLines.size() && yPos < editBoxTop + editBoxHeight - 4; i++) {
                    String line = currentLines.get(i);
                    // 截断过长文本
                    int maxChars = editBoxWidth / 6;
                    if (line.length() > maxChars) {
                        line = line.substring(0, maxChars - 3) + "...";
                    }
                    guiGraphics.drawString(font, line, editBoxLeft + 4, yPos, 0xDDDDDD, false);
                    yPos += 14;
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    // ---------- 鼠标滚轮（目录滚动） ----------
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (!showingDirectory) {
            int panelLeft = 8;
            int panelTop = 38;
            int panelWidth = 58;
            int panelHeight = editBoxHeight + 24;
            if (mouseX >= panelLeft && mouseX <= panelLeft + panelWidth &&
                mouseY >= panelTop && mouseY <= panelTop + panelHeight) {
                int totalDays = availableDays.size();
                int maxOffset = Math.max(0, totalDays - 8);
                if (scrollDelta < 0) {
                    directoryScrollOffset = Math.min(directoryScrollOffset + 1, maxOffset);
                } else if (scrollDelta > 0) {
                    directoryScrollOffset = Math.max(directoryScrollOffset - 1, 0);
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    // ---------- 鼠标点击（目录跳转） ----------
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!showingDirectory && button == 0) {
            int panelLeft = 8;
            int panelTop = 38;
            int panelWidth = 58;
            int panelHeight = editBoxHeight + 24;
            if (mouseX >= panelLeft + 4 && mouseX <= panelLeft + panelWidth - 4 &&
                mouseY >= panelTop + 22 && mouseY <= panelTop + panelHeight - 4) {
                int index = (int)((mouseY - panelTop - 22) / 14);
                int startIndex = directoryScrollOffset;
                int realIndex = startIndex + index;
                if (realIndex >= 0 && realIndex < availableDays.size()) {
                    int day = availableDays.get(realIndex);
                    if (viewingDay != day) {
                        saveCurrentDay();
                        viewingDay = day;
                        loadDay(day);
                        clearWidgets();
                        init();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ---------- 键盘控制 ----------
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showingDirectory) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                onClose();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
                int totalPages = (int) Math.ceil((double) availableDays.size() / DAYS_PER_PAGE);
                if (directoryPage < totalPages - 1) {
                    directoryPage++;
                    clearWidgets();
                    init();
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
                if (directoryPage > 0) {
                    directoryPage--;
                    clearWidgets();
                    init();
                }
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            saveCurrentDay();
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---------- 天数计算 ----------
    private int getCurrentDay() {
        CompoundTag tag = diaryStack.getOrCreateTag();
        int realDay = tag.contains("diary_day") ? tag.getInt("diary_day") : 1;
        int maxDay = availableDays.isEmpty() ? 0 : Collections.max(availableDays);
        return Math.max(realDay, maxDay);
    }

    // ---------- NBT 读写 ----------
    private void loadAllFromNBT(ItemStack stack) {
        diaryPages.clear();
        availableDays.clear();
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains("diary_all_pages")) {
            CompoundTag allPages = tag.getCompound("diary_all_pages");
            List<String> keysToRemove = new ArrayList<>();
            for (String key : allPages.getAllKeys()) {
                try {
                    int day = Integer.parseInt(key);
                    if (day <= 0) {
                        keysToRemove.add(key);
                        continue;
                    }
                    ListTag list = allPages.getList(key, net.minecraft.nbt.Tag.TAG_STRING);
                    List<String> lines = new ArrayList<>();
                    for (int i = 0; i < list.size(); i++) {
                        lines.add(list.getString(i));
                    }
                    diaryPages.put(day, lines);
                    availableDays.add(day);
                } catch (NumberFormatException ignored) {}
            }
            for (String key : keysToRemove) {
                allPages.remove(key);
            }
            if (!keysToRemove.isEmpty()) {
                tag.put("diary_all_pages", allPages);
                stack.setTag(tag);
            }
        }
        Collections.sort(availableDays);
        if (diaryPages.isEmpty()) {
            int today = getCurrentDay();
            diaryPages.put(today, new ArrayList<>());
            availableDays.add(today);
        }
    }

    private void saveAllToNBT(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag allPages = new CompoundTag();
        for (Map.Entry<Integer, List<String>> entry : diaryPages.entrySet()) {
            if (entry.getKey() <= 0) continue;
            ListTag list = new ListTag();
            for (String line : entry.getValue()) {
                list.add(StringTag.valueOf(line));
            }
            allPages.put(String.valueOf(entry.getKey()), list);
        }
        tag.put("diary_all_pages", allPages);
        stack.setTag(tag);
    }
}