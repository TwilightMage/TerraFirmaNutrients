package org.drakosha.terrafirmanutrients;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
@Mod.EventBusSubscriber(modid = TerraFirmaNutrients.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigServer {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static ForgeConfigSpec.ConfigValue<Double> nutritionCapValue = BUILDER.define("nutritionCap", 20.0);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static double nutritionCap;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        nutritionCap = nutritionCapValue.get();
    }
}
