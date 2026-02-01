package com.dyuus.academy_features.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.dyuus.academy_features.config.ShopConfig;
import com.dyuus.academy_features.config.ShopConfigManager;
import com.dyuus.academy_features.screen.ShopScreenHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ShopCommand {

    // Suggestion provider for shop IDs (autocomplete)
    private static final SuggestionProvider<ServerCommandSource> SHOP_SUGGESTIONS = (context, builder) ->
            CommandSource.suggestMatching(ShopConfigManager.getShopIds(), builder);

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("shop")
                    .requires(source -> source.hasPermissionLevel(2))

                    // /shop → open default shop for self
                    .executes(ShopCommand::openDefaultShop)

                    // /shop <shopId> → open specific shop for self
                    .then(CommandManager.argument("shopId", StringArgumentType.word())
                            .suggests(SHOP_SUGGESTIONS)
                            .executes(ShopCommand::openShopById)

                            // /shop <shopId> <player> → open specific shop for target player
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                    .executes(ShopCommand::openShopForTarget)
                            )
                    )

                    // /shop reload → reload all shop configs
                    .then(CommandManager.literal("reload")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(ShopCommand::reloadConfig)
                    )

                    // /shop list → list all available shops
                    .then(CommandManager.literal("list")
                            .executes(ShopCommand::listShops)
                    )
            );
        });
    }

    /**
     * Opens the default shop for the command executor.
     */
    private static int openDefaultShop(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = getPlayerFromContext(context);
        if (player == null) return 0;

        ShopConfig config = ShopConfigManager.getDefaultShop();
        if (config == null) {
            context.getSource().sendError(Text.literal("Aucun shop par défaut configuré"));
            return 0;
        }

        return openShopForPlayer(player, config);
    }

    /**
     * Opens a specific shop by ID for the command executor.
     */
    private static int openShopById(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = getPlayerFromContext(context);
        if (player == null) return 0;

        String shopId = StringArgumentType.getString(context, "shopId");
        ShopConfig config = ShopConfigManager.getShop(shopId);

        if (config == null) {
            context.getSource().sendError(
                    Text.literal("Shop introuvable: " + shopId).formatted(Formatting.RED)
            );
            return 0;
        }

        return openShopForPlayer(player, config);
    }

    /**
     * Opens a specific shop for a target player.
     */
    private static int openShopForTarget(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "target");
            String shopId = StringArgumentType.getString(context, "shopId");
            ShopConfig config = ShopConfigManager.getShop(shopId);

            if (config == null) {
                context.getSource().sendError(
                        Text.literal("Shop introuvable: " + shopId).formatted(Formatting.RED)
                );
                return 0;
            }

            return openShopForPlayer(targetPlayer, config);
        } catch (CommandSyntaxException e) {
            context.getSource().sendError(Text.literal("Joueur introuvable"));
            return 0;
        }
    }

    /**
     * Opens a shop GUI for a specific player.
     *
     * @param player The player to open the shop for
     * @param config The shop configuration
     * @return 1 on success, 0 on failure
     */
    private static int openShopForPlayer(ServerPlayerEntity player, ShopConfig config) {
        DyuusAcademyFeatures.LOGGER.info("Opening shop '{}' for player {}",
                config.shopId, player.getName().getString());

        try {
            ShopScreenHandler.Data data = ShopScreenHandler.Data.fromConfig(config);

            player.openHandledScreen(new ExtendedScreenHandlerFactory<ShopScreenHandler.Data>() {
                @Override
                public ShopScreenHandler.Data getScreenOpeningData(ServerPlayerEntity player) {
                    return data;
                }

                @Override
                public Text getDisplayName() {
                    // Parse formatting codes (§) in display name
                    return Text.literal(config.displayName);
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    return new ShopScreenHandler(syncId, playerInventory, data);
                }
            });

            return 1;
        } catch (Exception e) {
            DyuusAcademyFeatures.LOGGER.error("Error opening shop '{}' for player {}",
                    config.shopId, player.getName().getString(), e);
            player.sendMessage(
                    Text.literal("Erreur lors de l'ouverture du shop: " + e.getMessage())
                            .formatted(Formatting.RED),
                    false
            );
            return 0;
        }
    }

    /**
     * Lists all available shops.
     */
    private static int listShops(CommandContext<ServerCommandSource> context) {
        var shopIds = ShopConfigManager.getShopIds();

        if (shopIds.isEmpty()) {
            context.getSource().sendFeedback(
                    () -> Text.literal("Aucun shop configuré").formatted(Formatting.YELLOW),
                    false
            );
            return 1;
        }

        context.getSource().sendFeedback(
                () -> Text.literal("Shops disponibles:").formatted(Formatting.GOLD),
                false
        );

        for (String shopId : shopIds) {
            ShopConfig config = ShopConfigManager.getShop(shopId);
            String displayName = config != null ? config.displayName : shopId;

            context.getSource().sendFeedback(
                    () -> Text.literal(" - ")
                            .formatted(Formatting.GRAY)
                            .append(Text.literal(shopId)
                                    .formatted(Formatting.GREEN))
                            .append(Text.literal(" (" + displayName + ")")
                                    .formatted(Formatting.DARK_GRAY)),
                    false
            );
        }

        return 1;
    }

    /**
     * Reloads all shop configurations.
     */
    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        ShopConfigManager.reloadConfig();
        context.getSource().sendFeedback(
                () -> Text.literal("Configuration des shops rechargée! (" +
                                ShopConfigManager.getShopIds().size() + " shops)")
                        .formatted(Formatting.GREEN),
                true
        );
        return 1;
    }

    /**
     * Gets the player from the command context.
     */
    private static ServerPlayerEntity getPlayerFromContext(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        try {
            return source.getPlayerOrThrow();
        } catch (CommandSyntaxException e) {
            if (source.getEntity() instanceof ServerPlayerEntity player) {
                return player;
            }
        }

        source.sendError(Text.literal("Cette commande doit être exécutée par un joueur"));
        return null;
    }
}