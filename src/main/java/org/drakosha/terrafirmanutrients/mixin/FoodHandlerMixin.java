package org.drakosha.terrafirmanutrients.mixin;

import net.dries007.tfc.common.capabilities.food.FoodData;
import net.minecraft.nbt.CompoundTag;
import org.drakosha.terrafirmanutrients.FoodHandlerAccessor;
import org.drakosha.terrafirmanutrients.TFNFoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin is about attaching TFN FoodData to FoodHandler
 */
@Mixin(net.dries007.tfc.common.capabilities.food.FoodHandler.class)
public class FoodHandlerMixin implements FoodHandlerAccessor {
    @Shadow(remap = false)
    protected FoodData data;

    @Unique
    TFNFoodData tfnFoodData;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    void onInit(FoodData data, CallbackInfo ci) {
        if (data == FoodData.EMPTY) {
            tfnFoodData = TFNFoodData.EMPTY;
        } else {
            tfnFoodData = new TFNFoodData(data);
        }
    }

    @Override
    public TFNFoodData getTfnFoodData() {
        return tfnFoodData;
    }

    @Override
    public void setTfnFoodData(TFNFoodData data) {
        tfnFoodData = data;
    }

    @Redirect(
            method = "serializeNBT()Lnet/minecraft/nbt/CompoundTag;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/dries007/tfc/common/capabilities/food/FoodData;write()Lnet/minecraft/nbt/CompoundTag;"
            ),
            remap = false
    )
    private CompoundTag redirectWriteCall(FoodData originalData) {
        return this.tfnFoodData.write();
    }

    @Inject(
            method = "deserializeNBT(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/dries007/tfc/common/capabilities/food/FoodHandler;data:Lnet/dries007/tfc/common/capabilities/food/FoodData;",
                    opcode = org.objectweb.asm.Opcodes.PUTFIELD,
                    shift = At.Shift.AFTER
            ),
            remap = false
    )
    private void onDeserializeDataSet(CompoundTag nbt, CallbackInfo ci) {
        this.tfnFoodData = TFNFoodData.read(nbt.getCompound("foodData"));
    }
}
