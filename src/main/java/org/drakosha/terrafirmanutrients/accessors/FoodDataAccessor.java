package org.drakosha.terrafirmanutrients.accessors;

import org.drakosha.terrafirmanutrients.Nutrient;
import org.drakosha.terrafirmanutrients.TFNFoodData;

import java.util.Set;

public interface FoodDataAccessor {
    void eatTfn(TFNFoodData data);

    Nutrient getThirstNutrient();
    void setThirstNutrient(Nutrient nutrient);

    boolean canConsumeNutrientSet(Set<Nutrient> nutrients);
}
