package datapacksscreen.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class DatapackListWidget extends AlwaysSelectedEntryListWidget<DatapackEntry> {

    private final Text title;

    public DatapackListWidget(MinecraftClient client, int width, int height, Text title, List<ResourcePackProfile> list, DatapackScreen screen) {
        super(client, width, height, 32, height - 55 + 4, 36);
        this.title = title;
        list.forEach(pack -> children().add(new DatapackEntry(client, this, pack, screen)));

        setRenderHeader(true, (int) (9.0f * 1.5f));
        centerListVertically = false;
    }

    @Override
    protected void renderHeader(MatrixStack matrices, int x, int y, Tessellator tessellator) {
        MutableText text = Text.empty().append(title).formatted(Formatting.UNDERLINE, Formatting.BOLD);
        int posX = x + width / 2 - client.textRenderer.getWidth(text) / 2;
        int posY = Math.min(top + 3, y);
        this.client.textRenderer.draw(matrices, text, posX, posY, 0xFFFFFF);
    }

    @Override
    public void setSelected(DatapackEntry entry) {
        super.setSelected(entry);
    }

    @Override
    public int getRowWidth() {
        return width;
    }

    @Override
    protected int getScrollbarPositionX() {
        return right - 6;
    }

    @Override
    public int addEntry(DatapackEntry entry) {
        return super.addEntry(entry);
    }

    @Override
    protected boolean removeEntryWithoutScrolling(DatapackEntry entry) {
        return super.removeEntryWithoutScrolling(entry);
    }

    @Override
    public int getRowTop(int index) {
        return super.getRowTop(index);
    }
}
