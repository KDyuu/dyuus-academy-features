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
                    // /pokedollars → show your balance
                    .executes(PokeDollarsCommand::checkOwnBalance)

                    // /pokedollars <player> → show another player's balance (admin)
                    .then(CommandManager.argument("player", EntityArgumentType.player())
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(PokeDollarsCommand::checkOtherBalance)
                    )

                    // /pokedollars pay <player> <amount> → pay another player
                    .then(CommandManager.literal("pay")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(PokeDollarsCommand::payPlayer)
                                    )
                            )
                    )

                    // /pokedollars set <player> <amount> → set player's balance (admin)
                    .then(CommandManager.literal("set")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                            .executes(PokeDollarsCommand::setMoney)
                                    )
                            )
                    )

                    // /pokedollars add <player> <amount> → add to player's balance (admin)
                    // This command is used by NPCs to reward players after battles
                    .then(CommandManager.literal("add")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(PokeDollarsCommand::addMoney)
                                    )
                            )
                    )

                    // /pokedollars remove <player> <amount> → remove from player's balance (admin)
                    .then(CommandManager.literal("remove")
                            .requires(source -> source.hasPermissionLevel(2))
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(PokeDollarsCommand::removeMoney)
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

            // Check that the player is not paying themselves
            if (sender.getUuid().equals(target.getUuid())) {
                sender.sendMessage(
                        Text.literal("Vous ne pouvez pas vous payer vous-même!")
                                .formatted(Formatting.RED),
                        false
                );
                return 0;
            }

            // Check that the sender has enough money
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

    /**
     * Add money to a player's balance.
     * Used by NPCs to reward players after winning battles.
     * Command: /pokedollars add <player> <amount>
     */
    private static int addMoney(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            CurrencyManager.addBalance(target, amount);

            // Feedback to command executor (usually console or command block)
            context.getSource().sendFeedback(
                    () -> Text.literal("Ajouté " + amount + " PokéDollars à " + target.getName().getString())
                            .formatted(Formatting.GREEN),
                    true
            );

            // Notification to the player
            target.sendMessage(
                    Text.literal("Vous avez reçu ")
                            .formatted(Formatting.GREEN)
                            .append(Text.literal(amount + " PokéDollars")
                                    .formatted(Formatting.GOLD))
                            .append(Text.literal("!")
                                    .formatted(Formatting.GREEN)),
                    false
            );

            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Remove money from a player's balance.
     * Can be used for penalties or purchases via commands.
     * Command: /pokedollars remove <player> <amount>
     */
    private static int removeMoney(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
            int amount = IntegerArgumentType.getInteger(context, "amount");

            int currentBalance = CurrencyManager.getBalance(target);

            if (currentBalance < amount) {
                context.getSource().sendFeedback(
                        () -> Text.literal("Impossible: " + target.getName().getString() + " n'a que " + currentBalance + " PokéDollars")
                                .formatted(Formatting.RED),
                        false
                );
                return 0;
            }

            CurrencyManager.removeBalance(target, amount);

            // Feedback to command executor
            context.getSource().sendFeedback(
                    () -> Text.literal("Retiré " + amount + " PokéDollars de " + target.getName().getString())
                            .formatted(Formatting.YELLOW),
                    true
            );

            // Notification to the player
            target.sendMessage(
                    Text.literal("On vous a retiré ")
                            .formatted(Formatting.RED)
                            .append(Text.literal(amount + " PokéDollars")
                                    .formatted(Formatting.GOLD))
                            .append(Text.literal(".")
                                    .formatted(Formatting.RED)),
                    false
            );

            return 1;
        } catch (Exception e) {
            return 0;
        }
    }
}