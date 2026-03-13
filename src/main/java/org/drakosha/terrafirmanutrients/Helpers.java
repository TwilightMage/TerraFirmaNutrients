package org.drakosha.terrafirmanutrients;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.common.capabilities.food.*;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.drakosha.terrafirmanutrients.accessors.Accessor;
import org.drakosha.terrafirmanutrients.accessors.NutritionDataAccessor;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Helpers {
    public static NutritionData getNutritionData(Player player) {
        if (player.getFoodData() instanceof TFCFoodData tfcFoodData) {
            return tfcFoodData.getNutrition();
        }

        return null;
    }

    public static NutritionDataAccessor getTfnNutritionData(Player player) {
        if (player.getFoodData() instanceof TFCFoodData tfcFoodData) {
            return Accessor.get(tfcFoodData.getNutrition());
        }

        return null;
    }

    public static ListTag makeListTag(Iterable<? extends Tag> iterable) {
        ListTag result = new ListTag();
        for (var tag : iterable) {
            result.add(tag);
        }
        return result;
    }

    public static void foodDataToMap(Map<Nutrient, Float> map, float grain, float fruit, float vegetables, float protein, float dairy) {
        if (grain != 0) map.put(Nutrients.GRAIN.get(), grain);
        if (fruit != 0) map.put(Nutrients.FRUIT.get(), fruit);
        if (vegetables != 0) map.put(Nutrients.VEGETABLES.get(), vegetables);
        if (protein != 0) map.put(Nutrients.PROTEIN.get(), protein);
        if (dairy != 0) map.put(Nutrients.DAIRY.get(), dairy);
    }

    public static void foodDataToMap(Map<Nutrient, Float> map, FoodData foodData) {
        if (foodData.grain() != 0) map.put(Nutrients.GRAIN.get(), foodData.grain());
        if (foodData.fruit() != 0) map.put(Nutrients.FRUIT.get(), foodData.fruit());
        if (foodData.vegetables() != 0) map.put(Nutrients.VEGETABLES.get(), foodData.vegetables());
        if (foodData.protein() != 0) map.put(Nutrients.PROTEIN.get(), foodData.protein());
        if (foodData.dairy() != 0) map.put(Nutrients.DAIRY.get(), foodData.dairy());

        waterToMap(map, foodData.water());
    }

    public static void waterToMap(Map<Nutrient, Float> map, float water) {
        // Scale thirst from 0..20 to 0..1, just like all other nutrients
        // We divide by MAX_HUNGER instead of MAX_THIRST because later we multiply by the same constant
        // and scaling with 20 allows storing nutrients in data in similar ranges
        // e.g. thirst = 10 equal to nutrients/terrafirmanutrients:water = 2
        if (water != 0) map.put(Nutrients.WATER.get(), water / (float) TFCFoodData.MAX_HUNGER);
    }

    public static void addFoodTooltip(FoodHandler foodHandler, ItemStack stack, List<Component> text) {
        // Expiration dates
        if (foodHandler.isRotten()) {
            text.add(Component.translatable("tfc.tooltip.food_rotten").withStyle(ChatFormatting.RED));
            if (((stack.hashCode() * 1928634918231L) & 0xFF) == 0) {
                text.add(Component.translatable("tfc.tooltip.food_rotten_special").withStyle(ChatFormatting.RED));
            }
        } else {
            final long rottenDate = foodHandler.getRottenDate();
            if (rottenDate == FoodHandler.NEVER_DECAY_DATE) {
                if (!foodHandler.isTransientNonDecaying()) {
                    text.add(Component.translatable("tfc.tooltip.food_infinite_expiry").withStyle(ChatFormatting.GOLD));
                }
            } else {
                final long rottenCalendarTime = Calendars.CLIENT.ticksToCalendarTicks(rottenDate); // Date food rots on.
                final long ticksRemaining = rottenDate - Calendars.CLIENT.getTicks(); // Ticks remaining until rotten

                final MutableComponent tooltip = switch (TFCConfig.CLIENT.foodExpiryTooltipStyle.get()) {
                    case EXPIRY ->
                            Component.translatable("tfc.tooltip.food_expiry_date", ICalendar.getTimeAndDate(rottenCalendarTime, Calendars.CLIENT.getCalendarDaysInMonth()));
                    case TIME_LEFT ->
                            Component.translatable("tfc.tooltip.food_expiry_left", Calendars.CLIENT.getTimeDelta(ticksRemaining));
                    case BOTH ->
                            Component.translatable("tfc.tooltip.food_expiry_date_and_left", ICalendar.getTimeAndDate(rottenCalendarTime, Calendars.CLIENT.getCalendarDaysInMonth()), Calendars.CLIENT.getTimeDelta(ticksRemaining));
                    default -> null;
                };
                if (tooltip != null) {
                    text.add(tooltip.withStyle(ChatFormatting.DARK_GREEN));
                }
            }
        }

        // Nutrition / Hunger / Saturation / Water Values
        // Hide this based on the shift key (because it's a lot of into)
        if (ClientHelpers.hasShiftDown()) {
            addTooltipNutrients(foodHandler, stack, text);
        } else {
            text.add(Component.translatable("tfc.tooltip.hold_shift_for_nutrition_info").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }

        // Add info for each trait
        for (FoodTrait trait : foodHandler.getTraits()) {
            trait.addTooltipInfo(stack, text);
        }

        if (TFCConfig.CLIENT.enableDebug.get()) {
            text.add(Component.literal(ChatFormatting.DARK_GRAY + "[Debug] Created at: " + foodHandler.getCreationDate() + " rots at: " + foodHandler.getRottenDate()));
        }
    }

    public static void addTooltipNutrients(IFood food, ItemStack stack, List<Component> text) {
        text.add(Component.translatable("tfc.tooltip.nutrition").withStyle(ChatFormatting.GRAY));

        boolean any = false;
        if (food instanceof FoodHandler foodHandler) {
            if (!foodHandler.isRotten())
            {
                final TFNFoodData data = Accessor.get(foodHandler).getTfnFoodData();

                float saturation = data.saturation;
                if (saturation > 0)
                {
                    // This display makes it so 100% saturation means a full hunger bar worth of saturation.
                    text.add(Component.translatable("tfc.tooltip.nutrition_saturation", String.format("%d", (int) (saturation * 5))).withStyle(ChatFormatting.GRAY));
                    any = true;
                }

                for(var nutrient : data.nutrients.keySet()) {
                    float value = data.nutrient(nutrient);
                    if (value > 0.0F) {
                        text.add(Component.literal(" - ")
                                .append(nutrient.getDisplayName(Objects.requireNonNull(NutrientRegistry.REGISTRY.get().getKey(nutrient))))
                                .append(Component.literal(": " + String.format("%.1f", value)))
                                .withStyle(style -> style.withColor(nutrient.color)));
                        any = true;
                    }
                }
            }
        } else {
            final FoodData data = food.getData();

            float saturation = data.saturation();
            if (saturation > 0)
            {
                // This display makes it so 100% saturation means a full hunger bar worth of saturation.
                text.add(Component.translatable("tfc.tooltip.nutrition_saturation", String.format("%d", (int) (saturation * 5))).withStyle(ChatFormatting.GRAY));
                any = true;
            }
            int water = (int) data.water();
            if (water > 0)
            {
                text.add(Component.translatable("tfc.tooltip.nutrition_water", String.format("%d", water)).withStyle(ChatFormatting.GRAY));
                any = true;
            }

            for (net.dries007.tfc.common.capabilities.food.Nutrient nutrient : net.dries007.tfc.common.capabilities.food.Nutrient.VALUES)
            {
                float value = data.nutrient(nutrient);
                if (value > 0)
                {
                    text.add(Component.literal(" - ")
                            .append(net.dries007.tfc.util.Helpers.translateEnum(nutrient))
                            .append(": " + String.format("%.1f", value))
                            .withStyle(nutrient.getColor()));
                    any = true;
                }
            }
        }

        if (!any)
        {
            text.add(Component.translatable("tfc.tooltip.nutrition_none").withStyle(ChatFormatting.GRAY));
        }
    }

    // Inject here to add checks and allow in some cases
    public static boolean canEatRottenFood(Player player) {
        return false;
    }

    public static void redefinePlayerNutritionData(Player player) {
        TFCFoodData.restoreFoodStatsAfterDeath(player, player);
    }
}
