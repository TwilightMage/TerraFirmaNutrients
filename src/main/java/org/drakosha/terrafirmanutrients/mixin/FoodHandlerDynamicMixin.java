package org.drakosha.terrafirmanutrients.mixin;

import net.dries007.tfc.common.capabilities.food.FoodData;
import net.dries007.tfc.common.capabilities.food.FoodHandler;
import org.drakosha.terrafirmanutrients.Accessor;
import org.drakosha.terrafirmanutrients.TFNFoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodHandler.Dynamic.class)
public class FoodHandlerDynamicMixin extends FoodHandler {
    public FoodHandlerDynamicMixin(FoodData data) {
        super(data);
    }

    @Inject(method = "setFood", at = @At("TAIL"), remap = false)
    void onSetFood(FoodData data, CallbackInfo ci) {
        Accessor.get(this).setTfnFoodData(new TFNFoodData(this.data));
    }
}
