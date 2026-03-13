package org.drakosha.terrafirmanutrients;

import java.util.Set;

public interface NutritionDataAccessor {
    void updateNutrientSet(Set<Nutrient> nutrientSet);
    float getTfnNutrient(Nutrient nutrient);
    void addTfnNutrients(TFNFoodData data);

    Set<Nutrient> getNutrientSet();
}
