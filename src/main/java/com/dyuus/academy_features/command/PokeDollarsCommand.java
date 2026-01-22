package com.dyuus.academy_features.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.dyuus.academy_features.currency.CurrencyManager;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class PokeDollarsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("pokedollars")
                    // /pokedollars → affiche ton solde
                    .executes(PokeDollarsCommand::checkOwnBalance)

                    // /pokedollars <joueur> → affiche le solde d'un autre joueur (admin)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(PokeDollarsCommand::checkOtherBalance)
                    )

                    // /pokedollars pay <joueur> <montant> → donner de l'argent à un joueur
                    .then(CommandManager.literal("pay")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(PokeDollarsCommand::payPlayer)
                                    )
                            )
                    )

                    // /pokedollars set <joueur> <montant> → définir l'argent (admin)
                    .then(CommandManager.literal("set")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                            .executes(PokeDollarsCommand::setMoney)
                                    )
                            )
                    )
            );
        });
    }

    private static int checkOwnBalance(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        int balance = CurrencyManager.getBalance(player);
        player.sendMessage(
                Text.literal("Solde: ")
                        .formatted(Formatting.GOLD)
                        .append(Text.literal(String.valueOf(balance))
                                .formatted(Formatting.YELLOW))
                        .append(Text.literal(" PokéDollars")
                                .formatted(Formatting.GOLD)),
                false
        );
        return balance;
    }

    private static int checkOtherBalance(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
            int balance = CurrencyManager.getBalance(target);

            context.getSource().sendFeedback(
                    () -> Text.literal("Solde de " + target.getName().getString() + ": ")
                            .formatted(Formatting.GOLD)
                            .append(Text.literal(String.valueOf(balance))
                                    .formatted(Formatting.YELLOW))
                            .append(Text.literal(" PokéDollars")
                                    .formatted(Formatting.GOLD)),
                    false
            );

            return balance;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int payPlayer(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity sender = context.getSource().getPlayer();
        if (sender == null) return 0;

        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            // Vérifier que le joueur ne se paye pas lui-même
            if (sender.getUuid().equals(target.getUuid())) {
                sender.sendMessage(
                        Text.literal("Vous ne pouvez pas vous payer vous-même!")
                                .formatted(Formatting.RED),
                        false
                );
                return 0;
            }

            // Vérifier que le sender a assez d'argent
            int senderBalance = CurrencyManager.getBalance(sender);
            if (senderBalance < amount) {
                sender.sendMessage(
                        Text.literal("Solde insuffisant! Vous avez " + senderBalance + " PokéDollars")
                                .formatted(Formatting.RED),
                        false
                );
                return 0;
            }

            // Transaction
            CurrencyManager.removeBalance(sender, amount);
            CurrencyManager.addBalance(target, amount);

            // Messages
            sender.sendMessage(
                    Text.literal("Vous avez envoyé ")
                            .formatted(Formatting.GREEN)
                            .append(Text.literal(amount + " PokéDollars")
                                    .formatted(Formatting.GOLD))
                            .append(Text.literal(" à " + target.getName().getString())
                                    .formatted(Formatting.GREEN)),
                    false
            );

            target.sendMessage(
                    Text.literal("Vous avez reçu ")
                            .formatted(Formatting.GREEN)
                            .append(Text.literal(amount + " PokéDollars")
                                    .formatted(Formatting.GOLD))
                            .append(Text.literal(" de " + sender.getName().getString())
                                    .formatted(Formatting.GREEN)),
                    false
            );

            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int setMoney(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            CurrencyManager.setBalance(target, amount);

            context.getSource().sendFeedback(
                    () -> Text.literal("Solde de " + target.getName().getString() + " défini à " + amount + " PokéDollars")
                            .formatted(Formatting.GREEN),
                    true
            );

            target.sendMessage(
                    Text.literal("Votre solde a été défini à " + amount + " PokéDollars")
                            .formatted(Formatting.GOLD),
                    false
            );

            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}
