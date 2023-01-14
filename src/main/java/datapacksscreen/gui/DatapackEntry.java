package datapacksscreen.gui;

import datapacksscreen.AutoReloadMod;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.Util;

import java.io.InputStream;

public class DatapackEntry extends AlwaysSelectedEntryListWidget.Entry<DatapackEntry> {

    static final Identifier RESOURCE_PACKS_TEXTURE = new Identifier("datapacksscreen", "textures/gui/datapacks_icons.png");
    private static final Identifier UNKNOWN_PACK = new Identifier("textures/misc/unknown_pack.png");
    private final MinecraftClient client;
    private DatapackListWidget widget;
    private final ResourcePackProfile pack;
    private final DatapackScreen screen;
    private final OrderedText displayName;
    private final MultilineText description;
    private boolean isAutoReloadable;

    public DatapackEntry(MinecraftClient client, DatapackListWidget widget, ResourcePackProfile pack, DatapackScreen screen) {
        this.client = client;
        this.pack = pack;
        this.screen = screen;
        this.widget = widget;
        this.displayName = trimTextToWidth(client, pack.getDisplayName());
        this.description = MultilineText.create(client.textRenderer, pack.getDescription(), 130, 2);
        if (AutoReloadMod.getAutoreloadDatapacks().contains(pack.getName())) {
            isAutoReloadable = true;
        }
    }

    private static OrderedText trimTextToWidth(MinecraftClient client, Text text) {
        int i = client.textRenderer.getWidth(text);
        if (i > 157) {
            StringVisitable stringVisitable = StringVisitable.concat(client.textRenderer.trimToWidth(text, 157 - client.textRenderer.getWidth("...")), StringVisitable.plain("..."));
            return Language.getInstance().reorder(stringVisitable);
        }
        return text.asOrderedText();
    }

    @Override
    public Text getNarration() {
        return Text.translatable("narrator.select", this.pack.getDisplayName());
    }

    private Identifier loadPackIcon(TextureManager textureManager, ResourcePackProfile resourcePackProfile) {
        try (ResourcePack resourcePack = resourcePackProfile.createResourcePack()){
            Identifier identifier;
            InputSupplier<InputStream> inputSupplier = resourcePack.openRoot("pack.png");
            if (inputSupplier == null) {
                return UNKNOWN_PACK;
            }
            String string = resourcePackProfile.getName();
            identifier = new Identifier("minecraft", "pack/" + Util.replaceInvalidChars(string, Identifier::isPathCharacterValid) + "/" + Hashing.sha256().hashUnencodedChars(string) + "/icon");
            InputStream inputStream = inputSupplier.get();

            NativeImage nativeImage = NativeImage.read(inputStream);
            textureManager.registerTexture(identifier, new NativeImageBackedTexture(nativeImage));
            inputStream.close();
            return identifier;
        }
        catch (Exception exception) {
            return UNKNOWN_PACK;
        }
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, this.loadPackIcon(this.client.getTextureManager(), pack));
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        DrawableHelper.drawTexture(matrices, x, y, 0.0f, 0.0f, 32, 32, 32, 32);
        this.client.textRenderer.drawWithShadow(matrices, this.displayName, (float)(x + 32 + 2), (float)(y + 1), 0xFFFFFF);
        this.description.drawWithShadow(matrices, x + 32 + 2, y + 12, 10, 0x808080);

        RenderSystem.setShaderTexture(0, RESOURCE_PACKS_TEXTURE);
        int i = mouseX - x;
        int j = mouseY - y;
        if (pack.getName().startsWith("file/")) {
            if (isAutoReloadable) {
                DrawableHelper.drawTexture(matrices, x + entryWidth - 32 - 8, y, 96.0f, 64.0f, 32, 32, 128, 128);
            } else {
                DrawableHelper.drawTexture(matrices, x + entryWidth - 32 - 8, y, 64.0f, 64.0f, 32, 32, 128, 128);
            }
        }
        else {
            DrawableHelper.drawTexture(matrices, x + entryWidth - 32 - 8, y, 32.0f, 64.0f, 32, 32, 128, 128);
        }

        if (hovered) {
            DrawableHelper.fill(matrices, x, y, x + 32, y + 32, -1601138544);
            if (i > entryWidth - 32 - 8 && j > 8 && j < 24 && pack.getName().startsWith("file/")) {
                screen.setDrawInfo();
                if (isAutoReloadable) {
                    DrawableHelper.drawTexture(matrices, x + entryWidth - 32 - 8, y, 96.0f, 96.0f, 32, 32, 128, 128);
                }
                else{
                    DrawableHelper.drawTexture(matrices, x + entryWidth - 32 - 8, y, 64.0f, 96.0f, 32, 32, 128, 128);
                }
            }
            if (mouseX > screen.width / 2) {
                DrawableHelper.drawTexture(matrices, x, y, 32.0f, 0.0f, 32, 32, 128, 128);
                if (canMoveDown()) {
                    if (i >= 16 && i < 32 && j >= 16) {
                        DrawableHelper.drawTexture(matrices, x, y, 64.0f, 32.0f, 32, 32, 128, 128);
                    }
                    else {
                        DrawableHelper.drawTexture(matrices, x, y, 64.0f, 0.0f, 32, 32, 128, 128);
                    }
                }
                if (canMoveUp()) {
                    if (i >= 16 && i < 32 && j < 16) {
                        DrawableHelper.drawTexture(matrices, x, y, 96.0f, 32.0f, 32, 32, 128, 128);
                    }
                    else {
                        DrawableHelper.drawTexture(matrices, x, y, 96.0f, 0.0f, 32, 32, 128, 128);
                    }
                }
                if (i < 16) {
                    DrawableHelper.drawTexture(matrices, x, y, 32.0f, 32.0f, 32, 32, 128, 128);
                }
            }
            else {
                DrawableHelper.drawTexture(matrices, x, y, 0.0f, 0.0f, 32, 32, 128, 128);
                if (i < 32) {
                    DrawableHelper.drawTexture(matrices, x, y, 0.0f, 32.0f, 32, 32, 128, 128);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double i = mouseX - (double) this.widget.getRowLeft();
        double j = mouseY - (double) this.widget.getRowTop(this.widget.children().indexOf(this));
        if (mouseX > (double) screen.width / 2){
            if (i < 16) {
                screen.disablePack(this);
                isAutoReloadable = false;
                return true;
            }
            if (i >= 16 && i < 32 && j < 16 && canMoveUp()) {
                int index = widget.children().indexOf(this);
                widget.children().set(index, widget.children().get(index - 1));
                widget.children().set(index - 1, this);
                return true;
            }
            if (i >= 16 && i < 32 && j >= 16 && canMoveDown()) {
                int index = widget.children().indexOf(this);
                widget.children().set(index, widget.children().get(index + 1));
                widget.children().set(index + 1, this);
                return true;
            }
        }
        else {
            if (i < 32) {
                screen.enablePack(this);
                return true;
            }
        }

        if (i > widget.getRowWidth() - 32 - 8 && j > 8 && j < 24 && pack.getName().startsWith("file/")) {
            if (mouseX > (double) screen.width / 2) {
                isAutoReloadable = !isAutoReloadable;
            }
            else {
                screen.enablePack(this);
                isAutoReloadable = true;
            }
            return true;
        }

        return false;
    }

    public void setWidget(DatapackListWidget widget) {
        this.widget = widget;
    }

    public ResourcePackProfile getPack() {
        return pack;
    }

    public boolean isAutoReloadable() {
        return isAutoReloadable;
    }

    private boolean canMoveUp() {
        return widget.children().indexOf(this) > 0;
    }

    private boolean canMoveDown() {
        return widget.children().indexOf(this) < widget.children().size() - 1;
    }
}
