package org.drakosha.terrafirmanutrients;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import java.awt.*;

public class Nutrient {
    public TextColor color;
    public float defaultNutritionValue;

    public static final Codec<Nutrient> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    TextColor.CODEC.fieldOf("color").forGetter(nutrient -> nutrient.color),
                    Codec.FLOAT.fieldOf("defaultNutritionValue").forGetter(nutrient -> nutrient.defaultNutritionValue)
            ).apply(instance, Nutrient::new)
    );

    public Nutrient(TextColor color, float defaultNutritionValue) {
        this.color = color;
        this.defaultNutritionValue = defaultNutritionValue;
    }

    public Nutrient(Color color, float defaultNutritionValue) {
        this(TextColor.fromRgb(color.getRGB()), defaultNutritionValue);
    }

    public MutableComponent getDisplayName(ResourceLocation key) {
        return Component.translatable(key.toString());
    }
}
