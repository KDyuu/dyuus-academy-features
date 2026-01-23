package com.dyuus.academy_features.screen;

import com.dyuus.academy_features.config.TeraItemConfig;  // Nouveau
import com.dyuus.academy_features.network.TeraNetworkingClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TeraTypeSelectionScreen extends HandledScreen<TeraScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("dyuus-academy-features", "textures/gui/tera_background.png");
    private static final Identifier INV_TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");

    // Grille 6x3 = 19 Tera Types
    private static final int COLS = 8, ROWS = 3;

    public TeraTypeSelectionScreen(TeraScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.literal("Tera Types"));
        this.backgroundWidth = 176;
        this.backgroundHeight = 107 + 96;
        this.playerInventoryTitleY = 107 + 4;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;
        // ✅ AUCUN bouton = exactement comme shop
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {}

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, 176, 107, 256, 256);
        context.drawTexture(INV_TEXTURE, x, y + 107, 0, 126, 176, 96);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderTeraItems(context, mouseX, mouseY);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void renderTeraItems(DrawContext context, int mouseX, int mouseY) {
        List<TeraItemConfig> items = TeraItemConfig.getTeraItems();
        for (int i = 0; i < Math.min(items.size(), ROWS * COLS); i++) {
            TeraItemConfig itemConfig = items.get(i);
            int col = i % COLS;
            int row = i / COLS;

            int ix = x + 12 + col * 20;  // Même espacement que shop
            int iy = y + 28 + row * 20;

            ItemStack stack = new ItemStack(Registries.ITEM.get(Identifier.tryParse(itemConfig.itemId)));
            context.drawItem(stack, ix, iy);
            context.drawItemInSlot(this.textRenderer, stack, ix, iy);

            if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                renderTooltip(context, itemConfig, mouseX, mouseY);
            }
        }
    }

    private void renderTooltip(DrawContext context, TeraItemConfig config, int mouseX, int mouseY) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal(config.displayName).formatted(Formatting.DARK_PURPLE));
        tooltip.add(Text.literal("Type: " + config.teraType.toUpperCase()).formatted(Formatting.GRAY));
        context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<TeraItemConfig> items = TeraItemConfig.getTeraItems();
        for (int i = 0; i < Math.min(items.size(), ROWS * COLS); i++) {
            int col = i % COLS;
            int row = i / COLS;
            int ix = x + 12 + col * 20;
            int iy = y + 28 + row * 20;

            if (mouseX >= ix && mouseX < ix + 16 && mouseY >= iy && mouseY < iy + 16) {
                // ✅ Utilise teraType (ignore item visuel)
                TeraNetworkingClient.sendSelectTeraType(handler.getPartySlot(), items.get(i).teraType);
                this.close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
