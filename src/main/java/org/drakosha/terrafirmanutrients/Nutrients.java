package org.drakosha.terrafirmanutrients;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.drakosha.terrafirmanutrients.events.AteBadNutrientEvent;
import org.drakosha.terrafirmanutrients.events.DefineNutrientsEvent;

import java.awt.*;

import static org.drakosha.terrafirmanutrients.TerraFirmaNutrients.MODID;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Nutrients {
    public static final DeferredRegister<Nutrient> NUTRIENTS = DeferredRegister.create(NutrientRegistry.REGISTRY_KEY, TerraFirmaNutrients.MODID);

    public static final ResourceLocation NUTRIENT_SET_DEFAULT = ResourceLocation.fromNamespaceAndPath(MODID, "default_nutrient_set");

    public static final RegistryObject<Nutrient> GRAIN = NUTRIENTS.register("grain", () -> new TFCNutrient(net.dries007.tfc.common.capabilities.food.Nutrient.GRAIN, 0.5f));
    public static final RegistryObject<Nutrient> FRUIT = NUTRIENTS.register("fruit", () -> new TFCNutrient(net.dries007.tfc.common.capabilities.food.Nutrient.FRUIT, 0.5f));
    public static final RegistryObject<Nutrient> VEGETABLES = NUTRIENTS.register("vegetables", () -> new TFCNutrient(net.dries007.tfc.common.capabilities.food.Nutrient.VEGETABLES, 0.5f));
    public static final RegistryObject<Nutrient> PROTEIN = NUTRIENTS.register("protein", () -> new TFCNutrient(net.dries007.tfc.common.capabilities.food.Nutrient.PROTEIN, 0.5f));
    public static final RegistryObject<Nutrient> DAIRY = NUTRIENTS.register("dairy", () -> new TFCNutrient(net.dries007.tfc.common.capabilities.food.Nutrient.DAIRY, 0.0f));

    public static final RegistryObject<Nutrient> WATER = NUTRIENTS.register("water", () -> new TFNNutrient(new Color(53, 109, 220), 0.5f));

    public static Nutrient tfcNutrientToTfn(net.dries007.tfc.common.capabilities.food.Nutrient nutrient) {
        return switch (nutrient) {
            case GRAIN -> GRAIN.get();
            case FRUIT -> FRUIT.get();
            case VEGETABLES -> VEGETABLES.get();
            case PROTEIN -> PROTEIN.get();
            case DAIRY -> DAIRY.get();
        };
    }

    @SubscribeEvent
    public static void defineNutrientsDefault(DefineNutrientsEvent event) {
        event.patch()
                .withName(NUTRIENT_SET_DEFAULT)
                .add(Nutrients.GRAIN.get())
                .add(Nutrients.FRUIT.get())
                .add(Nutrients.VEGETABLES.get())
                .add(Nutrients.PROTEIN.get())
                .add(Nutrients.DAIRY.get())
                .build();

        event.thirstNutrientType = Nutrients.WATER.get();
    }

    @SubscribeEvent
    public static void ateBadNutrientDefault(AteBadNutrientEvent event) {
        if (event.nutrient instanceof TFNNutrient) {
            final RandomSource random = event.player.getRandom();
            if (random.nextFloat() < 0.6)
            {
                event.player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 1800, 1));
                if (random.nextFloat() < 0.15)
                {
                    event.player.addEffect(new MobEffectInstance(MobEffects.POISON, 1800, 0));
                }
            }
        }
    }
}
