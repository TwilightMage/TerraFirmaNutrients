package org.drakosha.terrafirmanutrients;

import net.dries007.tfc.common.capabilities.food.FoodData;
import net.dries007.tfc.common.capabilities.food.FoodHandler;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;
import org.drakosha.terrafirmanutrients.accessors.Accessor;

public class TFNFoodHandler extends FoodHandler {
    public TFNFoodHandler(TFNFoodData data) {
        super(FoodData.EMPTY);
        Accessor.get(this).setTfnFoodData(data);
    }

    @Override
    public float getDecayDateModifier() {
        // Copypasta of super implementation that uses tfnFoodData

        // Decay modifiers are higher = shorter
        float mod = Accessor.get(this).getTfnFoodData().decayModifier * Helpers.getValueOrDefault(TFCConfig.SERVER.foodDecayModifier).floatValue();
        for (FoodTrait trait : foodTraits)
        {
            mod *= trait.getDecayModifier();
        }
        // The modifier returned is used to calculate time, so higher = longer
        return mod == 0 ? Float.POSITIVE_INFINITY : 1 / mod;
    }
}
