package org.drakosha.terrafirmanutrients;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

public class TFCNutrient extends TFNNutrient {
    public net.dries007.tfc.common.capabilities.food.Nutrient tfcNutrient;

    public TFCNutrient(net.dries007.tfc.common.capabilities.food.Nutrient tfcNutrient, float defaultNutritionValue) {
        super(TextColor.fromLegacyFormat(tfcNutrient.getColor()), defaultNutritionValue);

        this.tfcNutrient = tfcNutrient;
    }

    @Override
    public MutableComponent getDisplayName(ResourceLocation key) {
        return net.dries007.tfc.util.Helpers.translateEnum(tfcNutrient);
    }
}
