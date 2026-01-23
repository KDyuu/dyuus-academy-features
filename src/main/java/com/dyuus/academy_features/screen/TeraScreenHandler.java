package com.dyuus.academy_features.screen;

import com.dyuus.academy_features.DyuusAcademyFeatures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;  // ✅ AJOUT : Import manquant
import net.minecraft.screen.ScreenHandler;

public class TeraScreenHandler extends ScreenHandler {
    private final int partySlot;

    // ✅ Constructeur pour registry (ExtendedScreenHandlerType::new)
    public TeraScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new Data(0));  // Slot par défaut 0
    }

    // ✅ Constructeur avec Data (client/serveur)
    public TeraScreenHandler(int syncId, PlayerInventory playerInventory, Data data) {
        super(DyuusAcademyFeatures.TERA_SCREEN_HANDLER, syncId);
        this.partySlot = data.partySlot();
    }

    public int getPartySlot() {
        return partySlot;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    // ✅ Data record avec PacketCodec CORRIGÉ
    public record Data(int partySlot) {
        public static final PacketCodec<RegistryByteBuf, Data> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.INTEGER,     // Gère readInt/writeInt
                Data::partySlot,          // Sélecteur pour encode
                Data::new                 // Constructeur pour decode
        );
    }
}
