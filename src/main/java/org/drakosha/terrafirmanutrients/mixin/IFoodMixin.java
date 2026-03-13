package org.drakosha.terrafirmanutrients.mixin;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.common.capabilities.food.FoodHandler;
import net.dries007.tfc.common.capabilities.food.FoodTrait;
import net.dries007.tfc.common.capabilities.food.IFood;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.drakosha.terrafirmanutrients.Helpers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(net.dries007.tfc.common.capabilities.food.IFood.class)
public interface IFoodMixin {
    @Shadow(remap = false)
    boolean isRotten();

    @Shadow(remap = false)
    long getRottenDate();

    @Shadow(remap = false)
    boolean isTransientNonDecaying();

    @Shadow(remap = false)
    List<FoodTrait> getTraits();

    @Shadow(remap = false)
    long getCreationDate();

    /**
     * @author Drakosha
     * @reason Different behavior
     */
    @Overwrite(remap = false)
    default void addTooltipInfo(ItemStack stack, List<Component> text) {
        // Expiration dates
        if (isRotten()) {
            text.add(Component.translatable("tfc.tooltip.food_rotten").withStyle(ChatFormatting.RED));
            if (((stack.hashCode() * 1928634918231L) & 0xFF) == 0) {
                text.add(Component.translatable("tfc.tooltip.food_rotten_special").withStyle(ChatFormatting.RED));
            }
        } else {
            final long rottenDate = getRottenDate();
            if (rottenDate == FoodHandler.NEVER_DECAY_DATE) {
                if (!isTransientNonDecaying()) {
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
            Helpers.addTooltipNutrients((IFood) this, stack, text);
        } else {
            text.add(Component.translatable("tfc.tooltip.hold_shift_for_nutrition_info").withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }

        // Add info for each trait
        for (FoodTrait trait : getTraits()) {
            trait.addTooltipInfo(stack, text);
        }

        if (TFCConfig.CLIENT.enableDebug.get()) {
            text.add(Component.literal(ChatFormatting.DARK_GRAY + "[Debug] Created at: " + getCreationDate() + " rots at: " + getRottenDate()));
        }
    }
}
