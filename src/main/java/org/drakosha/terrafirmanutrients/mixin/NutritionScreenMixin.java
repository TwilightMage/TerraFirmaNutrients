package org.drakosha.terrafirmanutrients.mixin;

import net.dries007.tfc.client.screen.TFCContainerScreen;
import net.dries007.tfc.common.capabilities.food.TFCFoodData;
import net.dries007.tfc.common.container.Container;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodData;
import org.drakosha.terrafirmanutrients.*;
import org.drakosha.terrafirmanutrients.accessors.Accessor;
import org.drakosha.terrafirmanutrients.accessors.NutritionDataAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Objects;

@Mixin(net.dries007.tfc.client.screen.NutritionScreen.class)
public abstract class NutritionScreenMixin extends TFCContainerScreen<Container> {

    @Unique
    private static final ResourceLocation TERRAFIRMANUTRIENTS_TEXTURE = ResourceLocation.fromNamespaceAndPath(TerraFirmaNutrients.MODID, "textures/gui/player_nutrition.png");

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/dries007/tfc/client/screen/TFCContainerScreen;<init>(Lnet/minecraft/world/inventory/AbstractContainerMenu;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/network/chat/Component;Lnet/minecraft/resources/ResourceLocation;)V"
            ),
            index = 3
    )
    private static ResourceLocation modifyTexture(ResourceLocation originalTexture) {
        return TERRAFIRMANUTRIENTS_TEXTURE;
    }

    public NutritionScreenMixin(Container container, Inventory playerInventory, Component name)
    {
        super(container, playerInventory, name, net.dries007.tfc.client.screen.NutritionScreen.TEXTURE);
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTicks, mouseX, mouseY);

        FoodData foodData = playerInventory.player.getFoodData();
        if (foodData instanceof TFCFoodData tfcFoodData) {
            NutritionDataAccessor nutritionAccessor = Accessor.get(tfcFoodData.getNutrition());

            int index = 0;
            for (Nutrient nutrient : nutritionAccessor.getNutrientSet())
            {
                final int width = (int)(nutritionAccessor.getTfnNutrient(nutrient) * 50);

                graphics.blit(texture, leftPos + 117, topPos + 20 + 13 * index, 176, 5, 52, 7);
                graphics.blit(texture, leftPos + 118, topPos + 21 + 13 * index, 176, 0, width, 5);

                index++;
            }
        }
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite()
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        super.renderLabels(graphics, mouseX, mouseY);

        FoodData foodData = playerInventory.player.getFoodData();
        if (foodData instanceof TFCFoodData tfcFoodData) {
            NutritionDataAccessor nutritionAccessor = (NutritionDataAccessor)tfcFoodData.getNutrition();

            int index = 0;
            for (Nutrient nutrient : nutritionAccessor.getNutrientSet())
            {
                final Component text = nutrient.getDisplayName(Objects.requireNonNull(NutrientRegistry.REGISTRY.get().getKey(nutrient)))
                        .withStyle(style -> style.withColor(nutrient.color));
                graphics.drawString(font, text, 112 - font.width(text), 19 + 13 * index, 0x404040, false);

                index++;
            }
        }
    }
}
