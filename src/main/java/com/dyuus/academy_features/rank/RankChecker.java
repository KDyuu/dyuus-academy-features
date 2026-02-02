package com.dyuus.academy_features.rank;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RankChecker {

    // Stocke les joueurs qui ont déjà reçu chaque rang
    private static final Set<UUID> hasDresseur = new HashSet<>();
    private static final Set<UUID> hasDresseurEntraine = new HashSet<>();
    private static final Set<UUID> hasTopDresseur = new HashSet<>();
    private static final Set<UUID> hasChampion = new HashSet<>();

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(RankChecker::onTick);
        DyuusAcademyFeatures.LOGGER.info("RankChecker registered");
    }

    private static void onTick(MinecraftServer server) {
        // Check toutes les 20 ticks (1 seconde)
        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        Scoreboard scoreboard = server.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective("ranking");
        if (objective == null) return;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            int score = scoreboard.getOrCreateScore(ScoreHolder.fromProfile(player.getGameProfile()), objective).getScore();
            String name = player.getGameProfile().getName();
            UUID uuid = player.getUuid();

            // Dresseur (3+)
            if (score >= 3 && !hasDresseur.contains(uuid)) {
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), "role assign " + name + " dresseur");
                hasDresseur.add(uuid);
            }

            // Dresseur Entraîné (6+)
            if (score >= 6 && !hasDresseurEntraine.contains(uuid)) {
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), "role assign " + name + " dresseur_entraine");
                hasDresseurEntraine.add(uuid);
            }

            // Top Dresseur (12+)
            if (score >= 12 && !hasTopDresseur.contains(uuid)) {
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), "role assign " + name + " top_dresseur");
                hasTopDresseur.add(uuid);
            }

            // Champion (18+)
            if (score >= 18 && !hasChampion.contains(uuid)) {
                server.getCommandManager().executeWithPrefix(server.getCommandSource(), "role assign " + name + " champion");
                hasChampion.add(uuid);
            }
        }
    }
}