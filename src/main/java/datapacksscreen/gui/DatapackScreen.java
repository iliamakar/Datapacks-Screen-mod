package datapacksscreen.gui;

import datapacksscreen.AutoReloadMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.HoveredTooltipPositioner;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DatapackScreen extends Screen {

    private DatapackListWidget availableList;
    private DatapackListWidget enabledList;
    private Tooltip info;
    private boolean isDrawInfo = false;

    public DatapackScreen() {
        super(Text.translatable("iliamakar.datapacksscreen.mod_name"));
    }

    @Override
    public void init() {
        MinecraftClient client = MinecraftClient.getInstance();
        ResourcePackManager manager = client.getServer().getDataPackManager();
        manager.scanPacks();
        info = Tooltip.of(Text.literal(Language.getInstance().get("iliamakar.currently_unstable")).append("\n\n").append(Language.getInstance().get("iliamakar.autoreload_info")));

        int listHeight = Math.max(height - 50, 240);

        availableList = new DatapackListWidget(client, 200, listHeight, Text.translatable("iliamakar.available_datapacks"), manager.getProfiles().stream().filter(profile -> !manager.getEnabledProfiles().contains(profile)).toList(), this);
        availableList.setLeftPos(width / 2 - 4 - 200);
        addSelectableChild(availableList);

        enabledList = new DatapackListWidget(client, 200, listHeight, Text.translatable("iliamakar.enabled_datapacks"), manager.getEnabledProfiles().stream().toList(), this);
        enabledList.setLeftPos(width / 2 + 4);
        addSelectableChild(enabledList);

        addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> close()).dimensions(width / 2 - 4 - 150, height - 48, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> doneButton()).dimensions(width / 2 + 4, height - 48, 150, 20).build());
    }

    @Override
    public void close() {
        super.close();
    }

    private void doneButton() {
        close();
        List<String> autoreload = new ArrayList<>();
        List<String> reload = new ArrayList<>();
        for (DatapackEntry pack : enabledList.children()) {
            reload.add(pack.getPack().getName());
            if (pack.isAutoReloadable()){
                autoreload.add(pack.getPack().getName());
            }
        }
        AutoReloadMod.updateConfig(autoreload);
        client.getServer().reloadResources(reload);
        if (autoreload.size() > 0) {
            try {
                AutoReloadMod.startWatcher();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            AutoReloadMod.stopWatcher();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (availableList.isMouseOver(mouseX, mouseY)) {
            return availableList.mouseScrolled(mouseX, mouseY, amount);
        }
        if (enabledList.isMouseOver(mouseX, mouseY)) {
            return enabledList.mouseScrolled(mouseX, mouseY, amount);
        }
        return false;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderBackgroundTexture(0);
        availableList.render(matrices, mouseX, mouseY, delta);
        enabledList.render(matrices, mouseX, mouseY, delta);
        DatapackScreen.drawCenteredText(matrices, textRenderer, Text.translatable("iliamakar.select_datapack"), width / 2, 8, 0xFFFFFF);
        super.render(matrices, mouseX, mouseY, delta);
        if (isDrawInfo) {
            setTooltip(info, HoveredTooltipPositioner.INSTANCE, true);
            isDrawInfo = false;
        }
    }

    public void enablePack(DatapackEntry pack) {
        enabledList.addEntry(pack);
        availableList.removeEntryWithoutScrolling(pack);
        pack.setWidget(enabledList);
    }

    public void disablePack(DatapackEntry pack) {
        enabledList.removeEntryWithoutScrolling(pack);
        availableList.addEntry(pack);
        pack.setWidget(availableList);
    }

    public void setDrawInfo() {
        isDrawInfo = true;
    }
}
