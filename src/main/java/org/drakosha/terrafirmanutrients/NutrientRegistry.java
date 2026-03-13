package org.drakosha.terrafirmanutrients;


import net.dries007.tfc.common.capabilities.food.TFCFoodData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

import java.awt.*;
import java.util.function.Supplier;

import static org.drakosha.terrafirmanutrients.TerraFirmaNutrients.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NutrientRegistry {
    public static final ResourceKey<Registry<Nutrient>> REGISTRY_KEY = ResourceKey
            .createRegistryKey(ResourceLocation.fromNamespaceAndPath(MODID, "nutrient_types"));

    public static Supplier<IForgeRegistry<Nutrient>> REGISTRY;

    public static @Nullable ResourceLocation getKey(Nutrient nutrient) {
        return REGISTRY.get().getKey(nutrient);
    }

    public static @NotNull ResourceLocation getKeyNotNull(Nutrient nutrient) {
        ResourceLocation key = REGISTRY.get().getKey(nutrient);
        if (key == null) {
            key = ResourceLocation.fromNamespaceAndPath(MODID, "empty");
        }

        return key;
    }

    @SubscribeEvent
    public static void onNewRegistry(NewRegistryEvent event) {
        REGISTRY = event.create(new RegistryBuilder<Nutrient>()
                .setName(NutrientRegistry.REGISTRY_KEY.location())
                .missing((key, isNetwork) -> new Nutrient(new Color(0, 0, 0), TFCFoodData.DEFAULT_AVERAGE_NUTRITION))
                .setDefaultKey(ResourceLocation.fromNamespaceAndPath(MODID, "empty")));
    }
}
