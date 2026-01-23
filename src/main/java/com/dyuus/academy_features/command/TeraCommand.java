package com.dyuus.academy_features.command;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.types.tera.TeraType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.dyuus.academy_features.screen.TeraScreenHandler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class TeraCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("tera")
                    // ✅ COMMANDE NPC/JOUEUR : Affiche liste cliquable
                    .then(CommandManager.literal("select")
                            .requires(source -> source.hasPermissionLevel(2))
                            .executes(TeraCommand::showTeraSelect)
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                    .executes(TeraCommand::showTeraSelectForTarget)
                            )
                    )
                    // ✅ COMMANDE GUI (inchangée)
                    .then(CommandManager.literal("gui")
                            .then(CommandManager.argument("slot", IntegerArgumentType.integer(0, 5))
                                    .executes(TeraCommand::openTeraGui)
                            )
                    )
            );
        });
    }

    private static int showTeraSelect(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = null;

        try {
            player = source.getPlayerOrThrow();
        } catch (CommandSyntaxException e) {
            if (source.getEntity() instanceof ServerPlayerEntity) {
                player = (ServerPlayerEntity) source.getEntity();
            }
        }

        if (player == null) {
            source.sendError(Text.literal("§cCette commande doit être exécutée par un joueur"));
            return 0;
        }

        return showTeraSelectForPlayer(player);
    }

    private static int showTeraSelectForTarget(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "target");
            return showTeraSelectForPlayer(targetPlayer);
        } catch (CommandSyntaxException e) {
            context.getSource().sendError(Text.literal("§cJoueur introuvable"));
            return 0;
        }
    }

    private static int showTeraSelectForPlayer(ServerPlayerEntity player) {
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);

        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§6§l═══════════════════════════════════"), false);
        player.sendMessage(Text.literal("§e§l    Changeur de Tera Type"), false);
        player.sendMessage(Text.literal("§6§l═══════════════════════════════════"), false);
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§7Cliquez sur un Pokémon pour modifier son Tera Type:"), false);
        player.sendMessage(Text.literal(""), false);

        boolean hasPokemon = false;
        for (int i = 0; i < 6; i++) {
            Pokemon pokemon = party.get(i);
            if (pokemon != null) {
                hasPokemon = true;

                String speciesName = pokemon.getSpecies().getName();
                int level = pokemon.getLevel();

                String currentTeraType = "§7Aucun";
                TeraType teraType = pokemon.getTeraType();
                if (teraType != null) {
                    currentTeraType = "§b" + teraType.getName();
                }

                Formatting nameColor = level >= 50 ? Formatting.GOLD : Formatting.GREEN;

                Text slotNumber = Text.literal("§8[" + (i + 1) + "] §r");
                Text pokemonName = Text.literal(speciesName).formatted(nameColor);
                Text levelText = Text.literal(" §7(Lv. " + level + ")");

                final String finalTeraType = currentTeraType;
                int finalI = i;
                Text clickableText = slotNumber
                        .copy()
                        .append(pokemonName)
                        .append(levelText)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/tera gui " + finalI
                                ))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("§e✦ Cliquez pour sélectionner\n\n")
                                                .append(Text.literal("§7Type Tera actuel: " + finalTeraType + "\n"))
                                                .append(Text.literal("§8Espèce: §f" + speciesName + "\n"))
                                                .append(Text.literal("§8Niveau: §f" + level))
                                ))
                        );

                player.sendMessage(clickableText, false);
            }
        }

        if (!hasPokemon) {
            player.sendMessage(Text.literal("  §c✖ Vous n'avez aucun Pokémon!"), false);
        }

        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("§6§l═══════════════════════════════════"), false);

        return 1;
    }

    private static int openTeraGui(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        int slot = IntegerArgumentType.getInteger(context, "slot");

        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        Pokemon pokemon = party.get(slot);

        if (pokemon == null) {
            player.sendMessage(Text.literal("§c✖ Aucun Pokémon dans ce slot!"), false);
            return 0;
        }

        player.openHandledScreen(new ExtendedScreenHandlerFactory<TeraScreenHandler.Data>() {
            @Override
            public TeraScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity playerEntity) {
                return new TeraScreenHandler(syncId, inv, new TeraScreenHandler.Data(slot));
            }

            @Override
            public Text getDisplayName() {
                return Text.literal("Tera Type - " + pokemon.getSpecies().getName());
            }

            @Override
            public TeraScreenHandler.Data getScreenOpeningData(ServerPlayerEntity player) {
                return new TeraScreenHandler.Data(slot);
            }
        });

        return 1;
    }
}
