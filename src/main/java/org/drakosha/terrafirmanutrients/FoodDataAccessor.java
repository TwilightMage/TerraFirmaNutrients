package org.drakosha.terrafirmanutrients;

import java.util.Set;

public interface FoodDataAccessor {
    void eatTfn(TFNFoodData data);

    Nutrient getThirstNutrient();
    void setThirstNutrient(Nutrient nutrient);

    boolean canConsumeNutrientSet(Set<Nutrient> nutrients);
}
