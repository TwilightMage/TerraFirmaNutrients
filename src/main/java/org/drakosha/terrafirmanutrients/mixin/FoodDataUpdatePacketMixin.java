package org.drakosha.terrafirmanutrients.mixin;

import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(net.dries007.tfc.network.FoodDataUpdatePacket.class)
public class FoodDataUpdatePacketMixin {
    @Shadow(remap = false)
    @Final
    private float[] nutrients;

    @Shadow(remap = false)
    @Final
    private float thirst;

    /**
     * @author Drakosha
     * @reason There is no good way to inject in buffer function
     */
    @Overwrite(remap = false)
    private static float[] readNutrients(FriendlyByteBuf buffer) {
        int numNutrients = buffer.readInt();
        float[] nutrients = new float[numNutrients];

        for(int i = 0; i < nutrients.length; ++i) {
            nutrients[i] = buffer.readFloat();
        }

        return nutrients;
    }

    /**
     * @author Drakosha
     * @reason There is no good way to inject in buffer function
     */
    @Overwrite(remap = false)
    void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(nutrients.length);
        for (float nutrient : nutrients) {
            buffer.writeFloat(nutrient);
        }

        buffer.writeFloat(thirst);
    }
}
