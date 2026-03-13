package org.drakosha.terrafirmanutrients.accessors;

import org.drakosha.terrafirmanutrients.Nutrient;
import org.drakosha.terrafirmanutrients.TFNFoodData;

import java.util.Set;

public interface NutritionDataAccessor {
    void updateNutrientSet(Set<Nutrient> nutrientSet);
    float getTfnNutrient(Nutrient nutrient);
    void addTfnNutrients(TFNFoodData data);

    Set<Nutrient> getNutrientSet();
}
