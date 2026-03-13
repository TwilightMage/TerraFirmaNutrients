package org.drakosha.terrafirmanutrients.mixin;

import com.google.gson.JsonObject;
import net.dries007.tfc.common.capabilities.food.*;
import net.dries007.tfc.util.JsonHelpers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.drakosha.terrafirmanutrients.*;
import org.drakosha.terrafirmanutrients.Nutrient;
import org.drakosha.terrafirmanutrients.accessors.Accessor;
import org.drakosha.terrafirmanutrients.accessors.FoodDefinitionAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(net.dries007.tfc.common.capabilities.food.FoodDefinition.class)
public class FoodDefinitionMixin implements FoodDefinitionAccessor {
    @Shadow(remap = false)
    @Final
    private FoodData data;

    @Unique
    TFNFoodData terrafirmanutrients$foodData;

    /**
     * @author Drakosha
     * @reason Different behavior
     */
    @Overwrite(remap = false)
    public static FoodHandler getHandler(FoodDefinition definition, ItemStack stack) {
        return switch (definition.getHandlerType()) {
            case STATIC -> new TFNFoodHandler(Accessor.get(definition).getTfnFoodData());
            case DYNAMIC -> new FoodHandler.Dynamic();
            case DYNAMIC_BOWL -> new DynamicBowlHandler(stack);
            default -> throw new IncompatibleClassChangeError();
        };
    }

    @Inject(method = "<init>(Lnet/minecraft/resources/ResourceLocation;Lcom/google/gson/JsonObject;)V", at = @At("TAIL"), remap = false)
    void onInit(ResourceLocation id, JsonObject json, CallbackInfo ci) {
        Map<Nutrient, Float> nutrients = new HashMap<>();

        Helpers.foodDataToMap(nutrients, data);

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
                        TerraFirmaNutrients.LOGGER.warn("{} food_item nutrients block have nutrient {} with 0 value, which doesn't makes sense", id, key);
                    }
                } else {
                    TerraFirmaNutrients.LOGGER.warn("{} food_item nutrients block reference unknown nutrient {}", id, key);
                }
            }
        }

        terrafirmanutrients$foodData = new TFNFoodData(data.hunger(), data.saturation(), nutrients, data.decayModifier());
    }

    @Override
    public TFNFoodData getTfnFoodData() {
        return terrafirmanutrients$foodData;
    }
}
