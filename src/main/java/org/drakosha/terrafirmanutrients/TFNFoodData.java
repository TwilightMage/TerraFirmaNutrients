package org.drakosha.terrafirmanutrients;

import com.google.gson.JsonObject;
import net.dries007.tfc.common.capabilities.food.FoodData;
import net.dries007.tfc.util.JsonHelpers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TFNFoodData {
    public int hunger;
    public float saturation;
    public final Map<Nutrient, Float> nutrients;
    public float decayModifier;

    public static final TFNFoodData EMPTY = new TFNFoodData(0, 0.0F, new HashMap<>(), 0.0F);

    public TFNFoodData(int hunger, float saturation, Map<Nutrient, Float> nutrients, float decayModifier) {
        this.hunger = hunger;
        this.saturation = saturation;
        this.nutrients = new HashMap<>(nutrients);
        this.decayModifier = decayModifier;
    }

    public TFNFoodData(FoodData foodData) {
        this.hunger = foodData.hunger();
        this.saturation = foodData.saturation();
        this.nutrients = new HashMap<>();
        this.decayModifier = foodData.decayModifier();

        Helpers.foodDataToMap(nutrients, foodData);
    }

    public TFNFoodData getScaledCopy(float scale) {
        var nutrientsScaled = new HashMap<>(nutrients);
        nutrientsScaled.replaceAll((k, v) -> v * scale);

        return new TFNFoodData((int)(hunger * scale), saturation * scale, nutrientsScaled, decayModifier * scale);
    }

    public static TFNFoodData decayOnly(float decayModifier) {
        return new TFNFoodData(0, 0.0F, new HashMap<>(), decayModifier);
    }

    public static TFNFoodData decode(FriendlyByteBuf buffer) {
        int hunger = buffer.readVarInt();
        float saturation = buffer.readFloat();
        float decayModifier = buffer.readFloat();

        Map<Nutrient, Float> nutrition = buffer.readMap(friendlyByteBuf -> NutrientRegistry.REGISTRY.get().getValue(friendlyByteBuf.readResourceLocation()), FriendlyByteBuf::readFloat);

        return new TFNFoodData(hunger, saturation, nutrition, decayModifier);
    }

    public static TFNFoodData read(JsonObject json) {
        int hunger = JsonHelpers.getAsInt(json, "hunger", 4);
        float saturation = JsonHelpers.getAsFloat(json, "saturation", 0.0F);
        float decayModifier = JsonHelpers.getAsFloat(json, "decay_modifier", 1.0F);

        Map<Nutrient, Float> nutrition = json
                .getAsJsonObject("nutrition").entrySet().stream()
                .collect(Collectors.toMap(entry -> NutrientRegistry.REGISTRY.get().getValue(ResourceLocation.parse(entry.getKey())), entry -> entry.getValue().getAsFloat()));

        return new TFNFoodData(hunger, saturation, nutrition, decayModifier);
    }

    public static TFNFoodData read(CompoundTag nbt) {
        int hunger = nbt.getInt("food");
        float saturation = nbt.getFloat("sat");
        float decayModifier = nbt.getFloat("decay");

        Map<Nutrient, Float> nutrition = new HashMap<>();

        Helpers.foodDataToMap(nutrition,
                nbt.getFloat("grain"),
                nbt.getFloat("fruit"),
                nbt.getFloat("veg"),
                nbt.getFloat("meat"),
                nbt.getFloat("dairy"));

        Helpers.waterToMap(nutrition, nbt.getFloat("water"));

        if (nbt.contains("nutrition")) {
            CompoundTag nutritionTag = nbt.getCompound("nutrition");

            for (String key : nutritionTag.getAllKeys()) {
                var nutrientKey = ResourceLocation.parse(key);
                var nutrient = NutrientRegistry.REGISTRY.get().getValue(nutrientKey);
                if (nutrient != null) {
                    nutrition.put(nutrient, nutritionTag.getFloat(key));
                }
            }
        }


        return new TFNFoodData(hunger, saturation, nutrition, decayModifier);
    }

    public float nutrient(Nutrient nutrient) {
        return nutrients.getOrDefault(nutrient, 0.0f);
    }

    public void ifHasNutrient(Nutrient nutrient, Consumer<Float> amountConsumer) {
        float amount = nutrient(nutrient);
        if (amount > 0) {
            amountConsumer.accept(amount);
        }
    }

    public CompoundTag write() {
        CompoundTag nbt = new CompoundTag();

        nbt.putInt("food", this.hunger);
        nbt.putFloat("sat", this.saturation);
        nbt.putFloat("decay", this.decayModifier);

        if (!nutrients.isEmpty()) {
            CompoundTag nutritionTag = new CompoundTag();
            nutrients.forEach((nutrient, value) -> nutritionTag.putFloat(Objects.requireNonNull(NutrientRegistry.REGISTRY.get().getKey(nutrient)).toString(), value));
            nbt.put("nutrition", nutritionTag);
        }

        return nbt;
    }

    public CompoundTag writeVanillaCompatible() {
        CompoundTag nbt = write();

        nbt.putFloat("water", this.nutrient(Nutrients.WATER.get()));

        ifHasNutrient(Nutrients.tfcNutrientToTfn(net.dries007.tfc.common.capabilities.food.Nutrient.GRAIN), amount -> nbt.putFloat("grain", amount));
        ifHasNutrient(Nutrients.tfcNutrientToTfn(net.dries007.tfc.common.capabilities.food.Nutrient.VEGETABLES), amount -> nbt.putFloat("veg", amount));
        ifHasNutrient(Nutrients.tfcNutrientToTfn(net.dries007.tfc.common.capabilities.food.Nutrient.FRUIT), amount -> nbt.putFloat("fruit", amount));
        ifHasNutrient(Nutrients.tfcNutrientToTfn(net.dries007.tfc.common.capabilities.food.Nutrient.PROTEIN), amount -> nbt.putFloat("meat", amount));
        ifHasNutrient(Nutrients.tfcNutrientToTfn(net.dries007.tfc.common.capabilities.food.Nutrient.DAIRY), amount -> nbt.putFloat("dairy", amount));

        return nbt;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.hunger);
        buffer.writeFloat(this.saturation);
        buffer.writeFloat(this.decayModifier);

        buffer.writeMap(nutrients,
                (friendlyByteBuf, nutrient) -> friendlyByteBuf.writeResourceLocation(Objects.requireNonNull(NutrientRegistry.REGISTRY.get().getKey(nutrient))),
                FriendlyByteBuf::writeFloat
        );
    }
}
