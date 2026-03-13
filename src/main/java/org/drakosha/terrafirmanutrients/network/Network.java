package org.drakosha.terrafirmanutrients.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.drakosha.terrafirmanutrients.TerraFirmaNutrients;

public class Network {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(TerraFirmaNutrients.MODID, "dragon_capability"))
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(packetId++,
                SyncNutrientSetPacket.class,
                SyncNutrientSetPacket::encode,
                SyncNutrientSetPacket::decode,
                SyncNutrientSetPacket::handle
        );
    }
}
