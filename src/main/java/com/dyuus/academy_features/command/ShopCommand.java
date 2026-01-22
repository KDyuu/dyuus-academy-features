package com.dyuus.academy_features.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.dyuus.academy_features.DyuusAcademyFeatures;
import com.dyuus.academy_features.config.ShopConfigManager;
import com.dyuus.academy_features.screen.ShopScreenHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
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
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("shop")
                    // /shop → ouvrir le shop pour soi-même
                    .executes(ShopCommand::openShop)

                    // /shop <joueur> → ouvrir le shop pour un joueur spécifique (avec @s)
                    .then(CommandManager.argument("target", EntityArgumentType.player())
                            .executes(ShopCommand::openShopForTarget)
                    )

                    // /shop reload → recharger la config (admin)
                    .then(CommandManager.literal("reload")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(ShopCommand::reloadConfig)
                    )
            );
        });
    }

    private static int openShop(CommandContext<ServerCommandSource> context) {
        DyuusAcademyFeatures.LOGGER.info("=== DEBUT openShop (sans argument) ===");

        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = null;

        // Essayer de récupérer le joueur de différentes manières
        try {
            player = source.getPlayerOrThrow();
        } catch (CommandSyntaxException e) {
            // Si getPlayerOrThrow échoue, essayer getEntity
            if (source.getEntity() instanceof ServerPlayerEntity) {
                player = (ServerPlayerEntity) source.getEntity();
            }
        }

        if (player == null) {
            DyuusAcademyFeatures.LOGGER.error("Player is null!");
            source.sendError(Text.literal("Cette commande doit être exécutée par un joueur"));
            return 0;
        }

        return openShopForPlayer(player);
    }

    private static int openShopForTarget(CommandContext<ServerCommandSource> context) {
        DyuusAcademyFeatures.LOGGER.info("=== DEBUT openShop (avec argument target) ===");

        try {
            ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "target");
            return openShopForPlayer(targetPlayer);
        } catch (CommandSyntaxException e) {
            DyuusAcademyFeatures.LOGGER.error("Impossible de récupérer le joueur cible", e);
            context.getSource().sendError(Text.literal("Joueur introuvable"));
            return 0;
        }
    }

    private static int openShopForPlayer(ServerPlayerEntity player) {
        DyuusAcademyFeatures.LOGGER.info("Player: {}", player.getName().getString());

        try {
            DyuusAcademyFeatures.LOGGER.info("Tentative de création de Data...");
            ShopScreenHandler.Data data = ShopScreenHandler.Data.fromConfig();
            DyuusAcademyFeatures.LOGGER.info("Data créée avec {} items", data.items().size());

            DyuusAcademyFeatures.LOGGER.info("Tentative d'ouverture du screen...");

            player.openHandledScreen(new ExtendedScreenHandlerFactory<ShopScreenHandler.Data>() {
                @Override
                public ShopScreenHandler.Data getScreenOpeningData(ServerPlayerEntity player) {
                    return data;
                }

                @Override
                public Text getDisplayName() {
                    return Text.literal("Shop");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    DyuusAcademyFeatures.LOGGER.info("createMenu appelé avec syncId: {}", syncId);
                    return new ShopScreenHandler(syncId, playerInventory, data);
                }
            });

            DyuusAcademyFeatures.LOGGER.info("=== FIN openShop SUCCESS ===");
            return 1;
        } catch (Exception e) {
            DyuusAcademyFeatures.LOGGER.error("=== ERREUR dans openShop ===", e);
            e.printStackTrace();
            player.sendMessage(
                    Text.literal("Erreur shop: " + e.getMessage()).formatted(Formatting.RED),
                    false
            );
            return 0;
        }
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        ShopConfigManager.reloadConfig();
        context.getSource().sendFeedback(
                () -> Text.literal("Configuration du shop rechargée!").formatted(Formatting.GREEN),
                true
        );
        return 1;
    }
}
