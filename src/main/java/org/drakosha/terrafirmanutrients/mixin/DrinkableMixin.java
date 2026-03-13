package org.drakosha.terrafirmanutrients.mixin;

import com.google.gson.JsonObject;
import net.dries007.tfc.common.capabilities.food.FoodData;
import net.dries007.tfc.common.capabilities.food.TFCFoodData;
import net.dries007.tfc.common.capabilities.player.PlayerData;
import net.dries007.tfc.util.Drinkable;
import net.dries007.tfc.util.JsonHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.drakosha.terrafirmanutrients.*;
import org.drakosha.terrafirmanutrients.accessors.Accessor;
import org.drakosha.terrafirmanutrients.accessors.DrinkableAccessor;
import org.drakosha.terrafirmanutrients.accessors.FoodDataAccessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(net.dries007.tfc.util.Drinkable.class)
public class DrinkableMixin implements DrinkableAccessor {
    @Shadow(remap = false)
    @Final
    private @Nullable FoodData food;

    @Shadow(remap = false)
    @Final
    private int thirst;

    @Shadow(remap = false)
    @Final
    private int intoxication;

    @Shadow(remap = false)
    @Final
    private List<Drinkable.Effect> effects;

    @Unique
    TFNFoodData terrafirmanutrients$foodData;

    @Override
    public TFNFoodData getTfnFoodData() {
        return terrafirmanutrients$foodData;
    }

    @Inject(method = "doDrink", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onDoDrink(Level level, Player player, BlockState state, BlockPos pos, PlayerData playerData, Drinkable drinkable, CallbackInfo ci) {
        if (player.getFoodData() instanceof TFCFoodData foodData) {
            FoodDataAccessor playerFoodAccessor = Accessor.get(foodData);
            DrinkableAccessor drinkableAccessor = Accessor.get(drinkable);
            if (!playerFoodAccessor.canConsumeNutrientSet(drinkableAccessor.getTfnFoodData().nutrients.keySet())) {
                ci.cancel();
            }
        }
    }

    /**
     * @author Drakosha
     * @reason Different behavior
     */
    @Overwrite(remap = false)
    public void onDrink(Player player, int mB) {
        assert !player.level().isClientSide;

        float multiplier = (float) mB / 25.0F;

        var foodDataAccessor = Accessor.get((TFCFoodData) player.getFoodData());
        foodDataAccessor.eatTfn(terrafirmanutrients$foodData.getScaledCopy(multiplier));

        if (intoxication > 0) {
            PlayerData.get(player).addIntoxicatedTicks((long) ((float) this.intoxication * multiplier));
        }

        RandomSource random = player.getRandom();
        for (Drinkable.Effect effect : effects) {
            if ((double) 1.0F - Math.pow(1.0F - effect.chance(), multiplier) > (double) random.nextFloat()) {
                player.addEffect(new MobEffectInstance(effect.type(), effect.duration(), effect.amplifier(), false, false, true));
            }
        }

        player.setSprinting(false);
    }

    @Inject(method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)V", at = @At("TAIL"), remap = false)
    void onInit(ResourceLocation id, JsonObject json, CallbackInfo ci) {
        int hunger = 0;
        float saturation = 0;
        float decayModifier = 0;
        Map<Nutrient, Float> nutrients = new HashMap<>();

        if (food != null) {
            hunger = food.hunger();
            saturation = food.saturation();
            decayModifier = food.decayModifier();

            Helpers.foodDataToMap(nutrients, food);
        }

        Helpers.waterToMap(nutrients, thirst);

        var nutrientsJson = JsonHelpers.getAsJsonObject(json, "nutrients", null);
        if (nutrientsJson != null) {
            for (String key : nutrientsJson.keySet()) {
                ResourceLocation nutrientKey = ResourceLocation.parse(key);
                Nutrient nutrient = NutrientRegistry.REGISTRY.get().getValue(nutrientKey);
                if (nutrient != null) {
                    float nutritionValue = JsonHelpers.getAsFloat(nutrientsJson, key, 0.0f);
                    if (nutritionValue != 0) {
                        nutrients.put(nutrient, nutritionValue);
                    } else {
                        TerraFirmaNutrients.LOGGER.warn("{} drinkable nutrients block have nutrient {} with 0 value, which doesn't makes sense", id, key);
                    }
                } else {
                    TerraFirmaNutrients.LOGGER.warn("{} drinkable nutrients block reference unknown nutrient {}", id, key);
                }
            }
        }

        terrafirmanutrients$foodData = new TFNFoodData(hunger, saturation, nutrients, decayModifier);
    }
}
