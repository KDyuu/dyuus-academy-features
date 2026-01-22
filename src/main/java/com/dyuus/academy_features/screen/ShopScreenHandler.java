package com.dyuus.academy_features.screen;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.dyuus.academy_features.config.ShopConfigManager;
import com.dyuus.academy_features.config.ShopItem;
import com.dyuus.academy_features.currency.CurrencyManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ShopScreenHandler extends ScreenHandler {
    private static final int ITEMS_PER_PAGE = 27; // 9x3 grille
    private final Data data;
    private int currentPage = 0;

    public ShopScreenHandler(int syncId, PlayerInventory playerInventory, Data data) {
        super(DyuusAcademyFeatures.SHOP_SCREEN_HANDLER, syncId);
        this.data = data;

        // Ajouter l'inventaire du joueur
        // Inventaire principal (3 lignes de 9 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }

        // Hotbar (9 slots)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 180));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Empêcher toute interaction avec les slots de l'inventaire quand on clique dans le shop
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            super.onSlotClick(slotIndex, button, actionType, player);
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public List<ShopItem> getItems() {
        return data.items;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        int maxPage = Math.max(0, (data.items.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE - 1);
        this.currentPage = Math.max(0, Math.min(page, maxPage));
    }

    public int getTotalPages() {
        if (data.items.isEmpty()) return 1;
        return (data.items.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
    }

    // Méthode appelée côté serveur pour acheter un item
    public boolean buyItem(ServerPlayerEntity player, int itemIndex, int quantity) {
        if (itemIndex < 0 || itemIndex >= data.items.size()) return false;

        ShopItem shopItem = data.items.get(itemIndex);
        int totalCost = shopItem.buyPrice * quantity;

        if (!shopItem.canBuy) {
            player.sendMessage(Text.literal("Cet item ne peut pas être acheté").formatted(Formatting.RED), false);
            return false;
        }

        if (CurrencyManager.getBalance(player) < totalCost) {
            player.sendMessage(Text.literal("Solde insuffisant!").formatted(Formatting.RED), false);
            return false;
        }

        Identifier itemId = Identifier.tryParse(shopItem.itemId);
        if (itemId == null) {
            DyuusAcademyFeatures.LOGGER.error("Invalid item ID: {}", shopItem.itemId);
            return false;
        }

        Item item = Registries.ITEM.get(itemId);
        ItemStack stack = new ItemStack(item, quantity);

        if (!player.getInventory().insertStack(stack)) {
            player.sendMessage(Text.literal("Inventaire plein!").formatted(Formatting.RED), false);
            return false;
        }

        CurrencyManager.removeBalance(player, totalCost);

        player.sendMessage(
                Text.literal("Acheté ")
                        .formatted(Formatting.GREEN)
                        .append(Text.literal(quantity + "x " + shopItem.displayName)
                                .formatted(Formatting.YELLOW))
                        .append(Text.literal(" pour " + totalCost + " PokéDollars")
                                .formatted(Formatting.GOLD)),
                false
        );

        return true;
    }

    // Méthode appelée côté serveur pour vendre un item
    public boolean sellItem(ServerPlayerEntity player, int itemIndex, int quantity) {
        if (itemIndex < 0 || itemIndex >= data.items.size()) return false;

        ShopItem shopItem = data.items.get(itemIndex);

        if (!shopItem.canSell) {
            player.sendMessage(Text.literal("Cet item ne peut pas être vendu").formatted(Formatting.RED), false);
            return false;
        }

        Identifier itemId = Identifier.tryParse(shopItem.itemId);
        if (itemId == null) {
            DyuusAcademyFeatures.LOGGER.error("Invalid item ID: {}", shopItem.itemId);
            return false;
        }

        Item item = Registries.ITEM.get(itemId);

        int available = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() == item) {
                available += stack.getCount();
            }
        }

        if (available < quantity) {
            player.sendMessage(
                    Text.literal("Vous n'avez que " + available + " " + shopItem.displayName)
                            .formatted(Formatting.RED),
                    false
            );
            return false;
        }

        int toRemove = quantity;
        for (int i = 0; i < player.getInventory().main.size() && toRemove > 0; i++) {
            ItemStack stack = player.getInventory().main.get(i);
            if (stack.getItem() == item) {
                int removed = Math.min(toRemove, stack.getCount());
                stack.decrement(removed);
                toRemove -= removed;
            }
        }

        int totalEarned = shopItem.sellPrice * quantity;
        CurrencyManager.addBalance(player, totalEarned);

        player.sendMessage(
                Text.literal("Vendu ")
                        .formatted(Formatting.GREEN)
                        .append(Text.literal(quantity + "x " + shopItem.displayName)
                                .formatted(Formatting.YELLOW))
                        .append(Text.literal(" pour " + totalEarned + " PokéDollars")
                                .formatted(Formatting.GOLD)),
                false
        );

        return true;
    }

    // Record pour les données transmises au client
    public record Data(List<ShopItem> items) {
        public static final PacketCodec<RegistryByteBuf, Data> PACKET_CODEC =
                new PacketCodec<>() {
                    @Override
                    public Data decode(RegistryByteBuf buf) {
                        int size = buf.readInt();
                        List<ShopItem> items = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            items.add(ShopItem.decode(buf));
                        }
                        return new Data(items);
                    }

                    @Override
                    public void encode(RegistryByteBuf buf, Data data) {
                        buf.writeInt(data.items.size());
                        for (ShopItem item : data.items) {
                            ShopItem.encode(buf, item);
                        }
                    }
                };

        public static Data fromConfig() {
            return new Data(new ArrayList<>(ShopConfigManager.getConfig().items));
        }
    }
}
