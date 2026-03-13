package org.drakosha.terrafirmanutrients;

import net.minecraft.network.chat.TextColor;

import java.awt.*;

/**
 * Parent class for all vanilla nutrients, do not extend
 */
public class TFNNutrient extends Nutrient {
    public TFNNutrient(TextColor color, float defaultNutritionValue) {
        super(color, defaultNutritionValue);
    }

    public TFNNutrient(Color color, float defaultNutritionValue) {
        super(color, defaultNutritionValue);
    }
}
