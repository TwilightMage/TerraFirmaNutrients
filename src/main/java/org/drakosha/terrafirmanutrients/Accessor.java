package org.drakosha.terrafirmanutrients;

import net.dries007.tfc.common.capabilities.food.FoodDefinition;
import net.dries007.tfc.common.capabilities.food.FoodHandler;
import net.dries007.tfc.common.capabilities.food.NutritionData;
import net.dries007.tfc.common.capabilities.food.TFCFoodData;
import net.dries007.tfc.util.Drinkable;

public class Accessor {
    public static DrinkableAccessor get(Drinkable obj) {
        return (DrinkableAccessor) obj;
    }

    public static FoodDataAccessor get(TFCFoodData obj) {
        return (FoodDataAccessor) obj;
    }

    public static FoodDefinitionAccessor get(FoodDefinition obj) {
        return (FoodDefinitionAccessor) obj;
    }

    public static FoodHandlerAccessor get(FoodHandler obj) {
        return (FoodHandlerAccessor) obj;
    }

    public static NutritionDataAccessor get(NutritionData obj) {
        return (NutritionDataAccessor) obj;
    }
}
