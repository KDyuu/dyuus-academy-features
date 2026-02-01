package com.dyuus.academy_features.screen;

import com.dyuus.academy_features.config.ShopItem;
import com.dyuus.academy_features.network.ShopNetworkingClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends HandledScreen<ShopScreenHandler> {
    private static final Identifier SHOP_TEXTURE = Identifier.of("dyuus-academy-features", "textures/gui/shop_background.png");
    private static final Identifier INVENTORY_TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");
    private static final int ITEMS_PER_PAGE = 27; // 9 columns × 3 rows
    private static final int SHOP_ROWS = 3; // 3 rows of items

    private ButtonWidget previousButton;
    private ButtonWidget nextButton;

    public ShopScreen(ShopScreenHandler handler, PlayerInventory inventory, Text title) {
        // Use the shop title from the handler data instead of a static "Shop"
        super(handler, inventory, Text.literal(handler.getShopTitle()));
        this.backgroundHeight = 107 + 96; // Custom shop + inventory
        this.backgroundWidth = 176;
        this.playerInventoryTitleY = 107 + 4;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;

        // Buttons below the 3 rows of items
        // 30 (items start) + 3*18 (3 rows) + 5 (spacing) = 89
        int buttonY = this.y + 89;

        this.previousButton = ButtonWidget.builder(Text.literal("◀"), button -> {
                    int currentPage = handler.getCurrentPage();
                    if (currentPage > 0) {
                        handler.setCurrentPage(currentPage - 1);
                    }
                })
                .dimensions(this.x + 10, buttonY, 54, 14)
                .build();
        this.addDrawableChild(previousButton);

        this.nextButton = ButtonWidget.builder(Text.literal("▶"), button -> {
                    int currentPage = handler.getCurrentPage();
                    int totalPages = handler.getTotalPages();
                    if (currentPage < totalPages - 1) {
                        handler.setCurrentPage(currentPage + 1);
                    }
                })
                .dimensions(this.x + 112, buttonY, 54, 14)
                .build();
        this.addDrawableChild(nextButton);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // Draw custom shop background
        context.drawTexture(SHOP_TEXTURE, x, y, 0, 0, 176, 107, 256, 256);

        // Draw vanilla inventory below
        context.drawTexture(INVENTORY_TEXTURE, x, y + 107, 0, 126, 176, 96);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        renderShopItems(context, mouseX, mouseY);

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void renderShopItems(DrawContext context, int mouseX, int mouseY) {
        List<ShopItem> allItems = handler.getItems();
        int currentPage = handler.getCurrentPage();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem shopItem = allItems.get(i);
            int relativeIndex = i - startIndex;
            int row = relativeIndex / 9; // 9 items per row
            int col = relativeIndex % 9;

            int itemX = x + 10 + col * 18;
            int itemY = y + 30 + row * 18; // Only 3 rows

            Identifier itemId = Identifier.tryParse(shopItem.itemId);
            if (itemId != null) {
                Item item = Registries.ITEM.get(itemId);
                ItemStack stack = new ItemStack(item);
                context.drawItem(stack, itemX, itemY);

                if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                    renderShopItemTooltip(context, shopItem, mouseX, mouseY);
                }
            }
        }
    }

    private void renderShopItemTooltip(DrawContext context, ShopItem shopItem, int mouseX, int mouseY) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(Text.literal(shopItem.displayName).formatted(Formatting.AQUA));
        tooltip.add(Text.literal(""));

        if (shopItem.canBuy) {
            tooltip.add(Text.literal("Achat: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(shopItem.buyPrice + " PokéDollars")
                            .formatted(Formatting.GREEN)));
        }
        if (shopItem.canSell) {
            tooltip.add(Text.literal("Vente: ")
                    .formatted(Formatting.GRAY)
                    .append(Text.literal(shopItem.sellPrice + " PokéDollars")
                            .formatted(Formatting.GOLD)));
        }

        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("Clic gauche: Acheter x1").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("Clic droit: Vendre x1").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.literal("Shift + Clic: x64").formatted(Formatting.DARK_GRAY));

        context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<ShopItem> allItems = handler.getItems();
        int currentPage = handler.getCurrentPage();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int relativeIndex = i - startIndex;
            int row = relativeIndex / 9;
            int col = relativeIndex % 9;

            int itemX = x + 10 + col * 18;
            int itemY = y + 30 + row * 18;

            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                boolean isShiftDown = hasShiftDown();
                int quantity = isShiftDown ? 64 : 1;

                if (button == 0) {
                    ShopNetworkingClient.sendBuyRequest(i, quantity);
                } else if (button == 1) {
                    ShopNetworkingClient.sendSellRequest(i, quantity);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}