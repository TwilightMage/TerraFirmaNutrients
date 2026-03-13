package org.drakosha.terrafirmanutrients.mixin;

import net.dries007.tfc.common.blockentities.PotBlockEntity;
import net.dries007.tfc.common.capabilities.food.*;
import net.dries007.tfc.common.items.TFCItems;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.common.recipes.SoupPotRecipe;
import net.minecraft.world.item.ItemStack;
import org.drakosha.terrafirmanutrients.*;
import org.drakosha.terrafirmanutrients.Nutrient;
import org.drakosha.terrafirmanutrients.accessors.Accessor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.*;

import static net.dries007.tfc.common.recipes.SoupPotRecipe.SOUP_DECAY_MODIFIER;
import static net.dries007.tfc.common.recipes.SoupPotRecipe.SOUP_HUNGER_VALUE;

@Mixin(net.dries007.tfc.common.recipes.SoupPotRecipe.class)
public class SoupPotRecipeMixin {

    /**
     * @author Drakosha
     * @reason because
     */
    @Overwrite(remap = false)
    public PotRecipe.Output getOutput(PotBlockEntity.PotInventory inventory)
    {
        int ingredientCount = 0;
        float saturation = 2;
        ItemStack soupStack = ItemStack.EMPTY;
        final List<ItemStack> itemIngredients = new ArrayList<>();

        Map<Nutrient, Float> nutrition = new HashMap<>();
        nutrition.put(Nutrients.WATER.get(), 4.0f);

        for (int i = PotBlockEntity.SLOT_EXTRA_INPUT_START; i <= PotBlockEntity.SLOT_EXTRA_INPUT_END; i++)
        {
            final ItemStack stack = inventory.getStackInSlot(i);
            final @Nullable IFood food = FoodCapability.get(stack);
            if (food != null)
            {
                itemIngredients.add(stack);
                if (food.isRotten()) // this should mostly not happen since the ingredients are not rotten to start, but worth checking
                {
                    ingredientCount = 0;
                    break;
                }

                TFNFoodData data;
                if (food instanceof FoodHandler foodHandler) {
                    data = Accessor.get(foodHandler).getTfnFoodData();
                } else {
                    data = new TFNFoodData(food.getData());
                }

                saturation += data.saturation;
                for (Nutrient nutrient : data.nutrients.keySet())
                {
                    var currentValue = nutrition.getOrDefault(nutrient, 0.0f);
                    nutrition.put(nutrient, currentValue + data.nutrient(nutrient));
                }
                ingredientCount++;
            }
        }

        if (ingredientCount > 0)
        {
            float multiplier = 1 - (0.05f * ingredientCount); // per-serving multiplier of nutrition
            saturation *= multiplier;

            nutrition.replaceAll((k, v) -> v * multiplier);

            PriorityQueue<Nutrient> maxNutrients = new PriorityQueue<>(Comparator.comparing(nutrition::get));
            maxNutrients.addAll(nutrition.keySet());

            net.dries007.tfc.common.capabilities.food.Nutrient tfcMaxNutrient = net.dries007.tfc.common.capabilities.food.Nutrient.GRAIN;
            for (var nutrient : maxNutrients) {
                if (nutrient instanceof TFCNutrient tfcNutrient) {
                    tfcMaxNutrient = tfcNutrient.tfcNutrient;
                    break;
                }
            }

            TFNFoodData data = new TFNFoodData(SOUP_HUNGER_VALUE, saturation, nutrition, SOUP_DECAY_MODIFIER);
            int servings = (int) (ingredientCount / 2f) + 1;
            long created = FoodCapability.getRoundedCreationDate();

            soupStack = new ItemStack(TFCItems.SOUPS.get(tfcMaxNutrient).get(), servings);
            final @Nullable IFood food = FoodCapability.get(soupStack);
            if (food instanceof DynamicBowlHandler handler)
            {
                handler.setCreationDate(created);
                handler.setIngredients(itemIngredients);
                Accessor.get(handler).setTfnFoodData(data);
            }
        }

        return new SoupPotRecipe.SoupOutput(soupStack);
    }
}
