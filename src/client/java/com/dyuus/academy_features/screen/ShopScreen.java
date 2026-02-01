package com.dyuus.academy_features.screen;

import com.dyuus.academy_features.config.ShopItem;
import com.dyuus.academy_features.network.ShopNetworkingClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShopScreen extends HandledScreen<ShopScreenHandler> {
    private static final Identifier SHOP_TEXTURE = Identifier.of("dyuus-academy-features", "textures/gui/shop_background.png");
    private static final Identifier INVENTORY_TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");
    private static final int ITEMS_PER_PAGE = 27; // 9 columns × 3 rows
    private static final int SHOP_ROWS = 3;

    private ButtonWidget previousButton;
    private ButtonWidget nextButton;

    // ==================== Search ====================
    private TextFieldWidget searchField;
    private String searchQuery = "";
    private List<ShopItem> filteredItems = new ArrayList<>();

    // ==================== Confirmation dialog ====================
    private boolean showConfirmation = false;
    private int pendingItemIndex = -1;
    private int pendingQuantity = 1;
    private boolean pendingIsBuy = true;
    private ShopItem pendingShopItem = null;

    public ShopScreen(ShopScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.literal(handler.getShopTitle()));
        this.backgroundHeight = 107 + 96;
        this.backgroundWidth = 176;
        this.playerInventoryTitleY = 107 + 4;
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 6;

        // ==================== Search field ====================
        int searchX = this.x + 8;
        int searchY = this.y + 17;
        int searchWidth = this.backgroundWidth - 16;

        this.searchField = new TextFieldWidget(this.textRenderer, searchX, searchY, searchWidth, 12, Text.literal("Rechercher..."));
        this.searchField.setMaxLength(50);
        this.searchField.setDrawsBackground(true);
        this.searchField.setEditableColor(0xFFFFFF);
        this.searchField.setPlaceholder(Text.literal("Rechercher...").formatted(Formatting.DARK_GRAY));
        this.searchField.setChangedListener(this::onSearchChanged);
        this.addDrawableChild(this.searchField);

        // ==================== Page buttons ====================
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
                    int totalPages = getFilteredTotalPages();
                    if (currentPage < totalPages - 1) {
                        handler.setCurrentPage(currentPage + 1);
                    }
                })
                .dimensions(this.x + 112, buttonY, 54, 14)
                .build();
        this.addDrawableChild(nextButton);

        // Initialize filtered items
        updateFilteredItems();
    }

    // ==================== Search logic ====================

    private void onSearchChanged(String query) {
        this.searchQuery = query;
        handler.setCurrentPage(0);
        updateFilteredItems();
    }

    private void updateFilteredItems() {
        List<ShopItem> allItems = handler.getItems();
        if (searchQuery == null || searchQuery.isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            String lowerQuery = searchQuery.toLowerCase(Locale.ROOT);
            filteredItems = new ArrayList<>();
            for (ShopItem item : allItems) {
                if (item.displayName.toLowerCase(Locale.ROOT).contains(lowerQuery)) {
                    filteredItems.add(item);
                }
            }
        }
    }

    private int getFilteredTotalPages() {
        if (filteredItems.isEmpty()) return 1;
        return (filteredItems.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
    }

    private int getRealIndex(ShopItem item) {
        return handler.getItems().indexOf(item);
    }

    // ==================== Confirmation dialog logic ====================

    private void openConfirmation(int realItemIndex, int quantity, boolean isBuy, ShopItem shopItem) {
        this.showConfirmation = true;
        this.pendingItemIndex = realItemIndex;
        this.pendingQuantity = quantity;
        this.pendingIsBuy = isBuy;
        this.pendingShopItem = shopItem;

        this.searchField.setFocused(false);
        this.searchField.visible = false;
        this.previousButton.active = false;
        this.previousButton.visible = false;
        this.nextButton.active = false;
        this.nextButton.visible = false;
    }

    private void confirmPurchase() {
        if (pendingItemIndex >= 0 && pendingShopItem != null) {
            if (pendingIsBuy) {
                ShopNetworkingClient.sendBuyRequest(pendingItemIndex, pendingQuantity);
            } else {
                ShopNetworkingClient.sendSellRequest(pendingItemIndex, pendingQuantity);
            }
        }
        closeConfirmation();
    }

    private void closeConfirmation() {
        this.showConfirmation = false;
        this.pendingItemIndex = -1;
        this.pendingQuantity = 1;
        this.pendingShopItem = null;

        this.previousButton.active = true;
        this.previousButton.visible = true;
        this.searchField.visible = true;
        this.nextButton.active = true;
        this.nextButton.visible = true;
    }

    // ==================== Rendering ====================

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        context.drawTexture(SHOP_TEXTURE, x, y, 0, 0, 176, 107, 256, 256);
        context.drawTexture(INVENTORY_TEXTURE, x, y + 107, 0, 126, 176, 96);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        if (!showConfirmation) {
            renderShopItems(context, mouseX, mouseY);
        }

        if (showConfirmation) {
            renderConfirmationDialog(context, mouseX, mouseY);
        }

        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void renderShopItems(DrawContext context, int mouseX, int mouseY) {
        updateFilteredItems();

        int currentPage = handler.getCurrentPage();
        int maxPage = Math.max(0, getFilteredTotalPages() - 1);
        if (currentPage > maxPage) {
            handler.setCurrentPage(maxPage);
            currentPage = maxPage;
        }

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem shopItem = filteredItems.get(i);
            int relativeIndex = i - startIndex;
            int row = relativeIndex / 9;
            int col = relativeIndex % 9;

            int itemX = x + 10 + col * 18;
            int itemY = y + 30 + row * 18;

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
        tooltip.add(Text.literal("Shift + Clic: x64 / max stock").formatted(Formatting.DARK_GRAY));

        context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
    }

    private void renderConfirmationDialog(DrawContext context, int mouseX, int mouseY) {
        if (pendingShopItem == null) return;

        int dialogWidth = 120;
        int dialogHeight = 62;
        int dialogX = this.x + (this.backgroundWidth / 2) - (dialogWidth / 2);
        int dialogY = this.y + 50;

        // Dark semi-transparent overlay
        context.fill(this.x, this.y, this.x + this.backgroundWidth, this.y + this.backgroundHeight, 0x88000000);

        // Dialog background
        context.fill(dialogX - 1, dialogY - 1, dialogX + dialogWidth + 1, dialogY + dialogHeight + 1, 0xFF555555);
        context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xFF2A2A2A);

        // Title
        String actionText = pendingIsBuy ? "Acheter" : "Vendre";
        Formatting actionColor = pendingIsBuy ? Formatting.GREEN : Formatting.GOLD;
        Text titleLine = Text.literal(actionText + " ?").formatted(actionColor, Formatting.BOLD);
        int titleWidth = this.textRenderer.getWidth(titleLine);
        context.drawText(this.textRenderer, titleLine, dialogX + (dialogWidth - titleWidth) / 2, dialogY + 4, 0xFFFFFF, true);

        // Item name + quantity
        String qtyText = (pendingQuantity == -1) ? "max" : (pendingQuantity + "x");
        Text itemLine = Text.literal(qtyText + " ")
                .formatted(Formatting.WHITE)
                .append(Text.literal(pendingShopItem.displayName).formatted(Formatting.AQUA));
        int itemWidth = this.textRenderer.getWidth(itemLine);
        context.drawText(this.textRenderer, itemLine, dialogX + (dialogWidth - itemWidth) / 2, dialogY + 16, 0xFFFFFF, true);

        // Price
        if (pendingQuantity == -1) {
            int unitPrice = pendingIsBuy ? pendingShopItem.buyPrice : pendingShopItem.sellPrice;
            Text priceLine = Text.literal(unitPrice + " PokéDollars/u").formatted(Formatting.YELLOW);
            int priceWidth = this.textRenderer.getWidth(priceLine);
            context.drawText(this.textRenderer, priceLine, dialogX + (dialogWidth - priceWidth) / 2, dialogY + 27, 0xFFFFFF, true);
        } else {
            int totalPrice = pendingIsBuy ? (pendingShopItem.buyPrice * pendingQuantity) : (pendingShopItem.sellPrice * pendingQuantity);
            Text priceLine = Text.literal(totalPrice + " PokéDollars").formatted(Formatting.YELLOW);
            int priceWidth = this.textRenderer.getWidth(priceLine);
            context.drawText(this.textRenderer, priceLine, dialogX + (dialogWidth - priceWidth) / 2, dialogY + 27, 0xFFFFFF, true);
        }

        // ==================== Custom buttons with hover ====================
        int btnY = dialogY + 42;
        int btnHeight = 16;

        // Confirm button
        int confirmX = dialogX + 4;
        int confirmW = 54;
        boolean confirmHover = mouseX >= confirmX && mouseX < confirmX + confirmW && mouseY >= btnY && mouseY < btnY + btnHeight;
        int confirmBg = confirmHover ? 0xFF2D5A2D : 0xFF333333;
        int confirmBorder = confirmHover ? 0xFF55FF55 : 0xFF555555;
        context.fill(confirmX - 1, btnY - 1, confirmX + confirmW + 1, btnY + btnHeight + 1, confirmBorder);
        context.fill(confirmX, btnY, confirmX + confirmW, btnY + btnHeight, confirmBg);
        Text confirmText = Text.literal("Confirmer").formatted(Formatting.GREEN);
        int confirmTw = this.textRenderer.getWidth(confirmText);
        context.drawText(this.textRenderer, confirmText, confirmX + (confirmW - confirmTw) / 2, btnY + 4, 0xFFFFFF, true);

        // Cancel button
        int cancelX = dialogX + dialogWidth - 54 - 4;
        int cancelW = 54;
        boolean cancelHover = mouseX >= cancelX && mouseX < cancelX + cancelW && mouseY >= btnY && mouseY < btnY + btnHeight;
        int cancelBg = cancelHover ? 0xFF5A2D2D : 0xFF333333;
        int cancelBorder = cancelHover ? 0xFFFF5555 : 0xFF555555;
        context.fill(cancelX - 1, btnY - 1, cancelX + cancelW + 1, btnY + btnHeight + 1, cancelBorder);
        context.fill(cancelX, btnY, cancelX + cancelW, btnY + btnHeight, cancelBg);
        Text cancelText = Text.literal("Annuler").formatted(Formatting.RED);
        int cancelTw = this.textRenderer.getWidth(cancelText);
        context.drawText(this.textRenderer, cancelText, cancelX + (cancelW - cancelTw) / 2, btnY + 4, 0xFFFFFF, true);
    }

    // ==================== Input handling ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showConfirmation) {
            if (button == 0) {
                int dialogWidth = 120;
                int dialogX = this.x + (this.backgroundWidth / 2) - (dialogWidth / 2);
                int dialogY = this.y + 50;
                int btnY = dialogY + 42;
                int btnHeight = 16;

                // Confirm button hit
                int confirmX = dialogX + 4;
                int confirmW = 54;
                if (mouseX >= confirmX && mouseX < confirmX + confirmW && mouseY >= btnY && mouseY < btnY + btnHeight) {
                    confirmPurchase();
                    return true;
                }

                // Cancel button hit
                int cancelX = dialogX + dialogWidth - 54 - 4;
                int cancelW = 54;
                if (mouseX >= cancelX && mouseX < cancelX + cancelW && mouseY >= btnY && mouseY < btnY + btnHeight) {
                    closeConfirmation();
                    return true;
                }
            }
            return true; // Block all other clicks while dialog is open
        }

        int currentPage = handler.getCurrentPage();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, filteredItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            int relativeIndex = i - startIndex;
            int row = relativeIndex / 9;
            int col = relativeIndex % 9;

            int itemX = x + 10 + col * 18;
            int itemY = y + 30 + row * 18;

            if (mouseX >= itemX && mouseX < itemX + 16 && mouseY >= itemY && mouseY < itemY + 16) {
                ShopItem shopItem = filteredItems.get(i);
                int realIndex = getRealIndex(shopItem);
                if (realIndex < 0) return true;

                boolean isBuy = (button == 0);
                boolean isShift = hasShiftDown();

                if (isBuy && !shopItem.canBuy) return true;
                if (!isBuy && !shopItem.canSell) return true;

                // Shift+click: buy 64, sell max stock (server resolves -1 to min(64, available))
                // Normal click: buy/sell 1
                int quantity;
                if (isShift) {
                    quantity = isBuy ? 64 : -1;
                } else {
                    quantity = 1;
                }

                openConfirmation(realIndex, quantity, isBuy, shopItem);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showConfirmation) {
            if (keyCode == 256) {
                closeConfirmation();
                return true;
            }
            return true;
        }

        if (this.searchField.isFocused()) {
            if (keyCode == 256) {
                this.searchField.setFocused(false);
                return true;
            }
            return this.searchField.keyPressed(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (showConfirmation) return true;

        if (this.searchField.isFocused()) {
            return this.searchField.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }
}