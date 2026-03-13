package org.drakosha.terrafirmanutrients.mixin;

import net.dries007.tfc.common.capabilities.food.FoodData;
import net.dries007.tfc.common.capabilities.food.TFCFoodData;
import net.dries007.tfc.config.TFCConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.drakosha.terrafirmanutrients.*;
import org.drakosha.terrafirmanutrients.accessors.NutritionDataAccessor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * This mixin completely overhauls default TFC NutritionData to work with dynamic nutrition sets
 */
@Mixin(net.dries007.tfc.common.capabilities.food.NutritionData.class)
public abstract class NutritionDataMixin implements NutritionDataAccessor {
    @Shadow(remap = false)
    private int hunger;

    @Shadow(remap = false)
    private int hungerWindow;

    @Shadow(remap = false)
    private float averageNutrients;

    @Unique
    private final LinkedList<TFNFoodData> terrafirmanutrients$records = new LinkedList<>();

    @Unique
    private Map<Nutrient, Float> terrafirmanutrients$nutrients = new HashMap<>();

    @Inject(method = "reset", at = @At("HEAD"), remap = false)
    void onReset(CallbackInfo ci) {
        terrafirmanutrients$records.clear();
    }

    @Override
    public void updateNutrientSet(Set<Nutrient> nutrientSet) {
        Set<Nutrient> currentNutrients = Set.copyOf(terrafirmanutrients$nutrients.keySet());

        for (var currentNutrient : currentNutrients) {
            if (!nutrientSet.contains(currentNutrient)) {
                terrafirmanutrients$nutrients.remove(currentNutrient);
            }
        }

        for (var validNutrient : nutrientSet) {
            if (!currentNutrients.contains(validNutrient)) {
                terrafirmanutrients$nutrients.put(validNutrient, 0.0f);
            }
        }

        calculateNutrition();
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite(remap = false)
    public void onClientUpdate(float[] nutrients) {
        List<Nutrient> keySet = terrafirmanutrients$nutrients.keySet().stream().toList();
        for (int i = 0; i < Math.min(keySet.size(), nutrients.length); i++) {
            terrafirmanutrients$nutrients.put(keySet.get(i), nutrients[i]);
        }
        this.updateAverageNutrients();
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite(remap = false)
    public float getNutrient(net.dries007.tfc.common.capabilities.food.Nutrient nutrient) {
        return getTfnNutrient(Nutrients.tfcNutrientToTfn(nutrient));
    }

    @Override
    public float getTfnNutrient(Nutrient nutrient) {
        return terrafirmanutrients$nutrients.getOrDefault(nutrient, 0.0f);
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite(remap = false)
    public void addNutrients(FoodData data) {
        addTfnNutrients(new TFNFoodData(data));
    }

    @Override
    public void addTfnNutrients(TFNFoodData data) {
        if (data.hunger > 0 || terrafirmanutrients$records.isEmpty() || terrafirmanutrients$records.getFirst().hunger > 0) {
            terrafirmanutrients$records.addFirst(data);
            calculateNutrition();
        }
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite(remap = false)
    public float[] getNutrients() {
        Collection<Float> nutrientValues = terrafirmanutrients$nutrients.values();
        float[] result = new float[nutrientValues.size()];
        int i = 0;
        for (Float f : nutrientValues) {
            result[i++] = f;
        }
        return result;
    }

    @Override
    public Set<Nutrient> getNutrientSet() {
        return terrafirmanutrients$nutrients.keySet();
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite(remap = false)
    public CompoundTag writeToNbt() {
        CompoundTag nbt = new CompoundTag();

        nbt.put("records", Helpers.makeListTag(terrafirmanutrients$records.stream().map(TFNFoodData::write).toList()));

        nbt.put("nutrients", Helpers.makeListTag(terrafirmanutrients$nutrients.keySet().stream().map(nutrient -> StringTag.valueOf(Objects.requireNonNull(NutrientRegistry.REGISTRY.get().getKey(nutrient)).toString())).toList()));

        return nbt;
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite(remap = false)
    public void readFromNbt(CompoundTag nbt) {
        terrafirmanutrients$records.clear();
        ListTag recordsNbt = nbt.getList("records", Tag.TAG_COMPOUND);
        for (int i = 0; i < recordsNbt.size(); i++) {
            terrafirmanutrients$records.add(TFNFoodData.read(recordsNbt.getCompound(i)));
        }

        terrafirmanutrients$nutrients.clear();
        ListTag nutrientsNbt = nbt.getList("nutrients", Tag.TAG_STRING);
        for (int i = 0; i < nutrientsNbt.size(); i++) {
            ResourceLocation nutrientKey = ResourceLocation.parse(nutrientsNbt.getString(i));
            Nutrient nutrient = NutrientRegistry.REGISTRY.get().getValue(nutrientKey);

            if (nutrient != null) {
                terrafirmanutrients$nutrients.put(nutrient, 0.0f);
            }
        }

        calculateNutrition();
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite(remap = false)
    private void calculateNutrition() {
        if (terrafirmanutrients$nutrients == null) return;

        // Reset
        terrafirmanutrients$nutrients.replaceAll((k, v) -> 0.0f);

        // Consider any hunger that isn't currently satisfied (i.e. < 20) to be a zero-nutrient gap in the current window. This has the effect
        // of pushing nutrient decay forward, into the time when hunger decays, and making food consumption always push total nutrients positively
        //
        // This does make it almost impossible to stay at "peak nutrition", so we re-weight the average nutrients, so values above 0.95 are
        // effectively 1.0, as far as health is concerned.
        int runningHungerTotal = Math.max(TFCFoodData.MAX_HUNGER - hunger, 0);

        // Reload from config
        hungerWindow = TFCConfig.SERVER.nutritionRotationHungerWindow.get();
        for (int i = 0; i < terrafirmanutrients$records.size(); i++) {
            TFNFoodData record = terrafirmanutrients$records.get(i);
            int nextHunger = record.hunger + runningHungerTotal;
            if (nextHunger <= this.hungerWindow) {
                // Add weighted nutrition, keep moving
                terrafirmanutrients$nutrients.replaceAll((k, v) -> v + record.nutrient(k) * Math.max(record.hunger, 4));
                runningHungerTotal = nextHunger;
            } else {
                // Calculate overshoot, weight appropriately, and exit
                float actualHunger = hungerWindow - runningHungerTotal;
                terrafirmanutrients$nutrients.replaceAll((k, v) -> v + record.nutrient(k) * actualHunger);

                // Remove any excess elements, this has the side effect of exiting the loop
                while (terrafirmanutrients$records.size() > i + 1) {
                    terrafirmanutrients$records.remove(i + 1);
                }
            }
        }

        // Average over hunger window, using default value if beyond the hunger window
        terrafirmanutrients$nutrients.replaceAll((k, v) -> v / hungerWindow);
        if (runningHungerTotal < hungerWindow) {
            float defaultModifier = 1 - (float) runningHungerTotal / hungerWindow;
            terrafirmanutrients$nutrients.replaceAll((k, v) -> v + k.defaultNutritionValue * defaultModifier);
        }
        terrafirmanutrients$nutrients.replaceAll((k, v) -> Math.min(1, v)); // Cap all nutrient averages at 1
        updateAverageNutrients(); // Also calculate overall average
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite(remap = false)
    private void updateAverageNutrients() {
        averageNutrients = 0;

        if (terrafirmanutrients$nutrients.isEmpty()) return;

        for (float nutrient : terrafirmanutrients$nutrients.values()) {
            averageNutrients += nutrient;
        }
        averageNutrients /= terrafirmanutrients$nutrients.size();
    }
}
