package org.drakosha.terrafirmanutrients.network;

import net.dries007.tfc.common.capabilities.food.TFCFoodData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.drakosha.terrafirmanutrients.Accessor;
import org.drakosha.terrafirmanutrients.Nutrient;
import org.drakosha.terrafirmanutrients.NutrientRegistry;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public record SyncNutrientSetPacket(int playerEntityId, Nutrient thirstNutrient, Set<Nutrient> nutrientSet) {

    public static void encode(SyncNutrientSetPacket message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.playerEntityId);

        buffer.writeResourceLocation(NutrientRegistry.getKeyNotNull(message.thirstNutrient));

        buffer.writeInt(message.nutrientSet.size());
        for (var nutrient : message.nutrientSet) {
            buffer.writeResourceLocation(NutrientRegistry.getKeyNotNull(nutrient));
        }
    }

    public static SyncNutrientSetPacket decode(FriendlyByteBuf buffer) {
        int playerEntityId = buffer.readInt();

        Nutrient thirstNutrient = NutrientRegistry.REGISTRY.get().getValue(buffer.readResourceLocation());

        int nutrientSetSize = buffer.readInt();
        Set<Nutrient> nutrientSet = new HashSet<>();
        for (int i = 0; i < nutrientSetSize; i++) {
            nutrientSet.add(NutrientRegistry.REGISTRY.get().getValue(buffer.readResourceLocation()));
        }

        return new SyncNutrientSetPacket(playerEntityId, thirstNutrient, nutrientSet);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            context.enqueueWork(this::run);
        }
        context.setPacketHandled(true);
    }

    private void run() {
        if (Minecraft.getInstance().level.getEntity(playerEntityId) instanceof LocalPlayer player) {
            if (player.getFoodData() instanceof TFCFoodData foodData) {
                Accessor.get(foodData.getNutrition()).updateNutrientSet(nutrientSet);
                Accessor.get(foodData).setThirstNutrient(thirstNutrient);
            }
        }
    }
}
