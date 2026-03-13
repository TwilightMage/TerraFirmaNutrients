package org.drakosha.terrafirmanutrients.mixin;

import net.dries007.tfc.common.capabilities.food.FoodHandler;
import net.dries007.tfc.common.capabilities.food.IFood;
import net.dries007.tfc.common.capabilities.food.NutritionData;
import net.dries007.tfc.common.capabilities.food.TFCFoodData;
import net.dries007.tfc.util.advancements.TFCAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;
import org.drakosha.terrafirmanutrients.*;
import org.drakosha.terrafirmanutrients.accessors.Accessor;
import org.drakosha.terrafirmanutrients.accessors.FoodDataAccessor;
import org.drakosha.terrafirmanutrients.accessors.NutritionDataAccessor;
import org.drakosha.terrafirmanutrients.events.AteBadNutrientEvent;
import org.drakosha.terrafirmanutrients.events.DefineNutrientsEvent;
import org.drakosha.terrafirmanutrients.network.Network;
import org.drakosha.terrafirmanutrients.network.SyncNutrientSetPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(net.dries007.tfc.common.capabilities.food.TFCFoodData.class)
public abstract class TFCFoodDataMixin implements FoodDataAccessor {
    @Shadow(remap = false)
    @Final
    private NutritionData nutritionData;

    @Unique
    private Nutrient terrafirmanutrients$thirstNutrient;

    @Shadow(remap = false)
    public abstract void addThirst(float toAdd);

    @Shadow(remap = false)
    @Final
    private Player sourcePlayer;

    @Shadow(remap = false)
    @Final
    private FoodData delegate;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/food/FoodData;Lnet/dries007/tfc/common/capabilities/food/NutritionData;)V", at = @At("TAIL"), remap = false)
    private void onInit(Player sourcePlayer, FoodData delegate, NutritionData nutritionData, CallbackInfo ci) {
        DefineNutrientsEvent defineNutrientsEvent = new DefineNutrientsEvent(sourcePlayer);
        MinecraftForge.EVENT_BUS.post(defineNutrientsEvent);

        Set<Nutrient> nutrientSet = defineNutrientsEvent.collectNutrientSet();

        NutritionDataAccessor accessor = Accessor.get(nutritionData);

        boolean hasChanged = false;

        if (!nutrientSet.equals(accessor.getNutrientSet())) {
            accessor.updateNutrientSet(nutrientSet);
            hasChanged = true;
        }

        if (terrafirmanutrients$thirstNutrient != defineNutrientsEvent.thirstNutrientType) {
            terrafirmanutrients$thirstNutrient = defineNutrientsEvent.thirstNutrientType;
            hasChanged = true;
        }

        if (hasChanged) {
            if (sourcePlayer instanceof ServerPlayer serverPlayer) {
                Network.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new SyncNutrientSetPacket(serverPlayer.getId(), defineNutrientsEvent.thirstNutrientType, nutrientSet));
            }
        }
    }

    /**
     * @author Drakosha
     * @reason Completely different behavior
     */
    @Overwrite(remap = false)
    public void eat(net.dries007.tfc.common.capabilities.food.FoodData data) {
        eatTfn(new TFNFoodData(data));
    }

    @Inject(method = "eat(Lnet/dries007/tfc/common/capabilities/food/IFood;)V", at = @At("HEAD"), cancellable = true, remap = false)
    void onEatIFood(IFood food, CallbackInfo ci) {
        if (food instanceof FoodHandler tfnFood) {
            TFNFoodData tfnFoodData = Accessor.get(tfnFood).getTfnFoodData();
            if (tfnFoodData != null) {
                ci.cancel();

                // If not rotten
                // Or can eat rotten
                // Or got in 40% success chance (would be better to instead calculate based on amount rotten)
                if (!food.isRotten() || Helpers.canEatRottenFood(sourcePlayer) || sourcePlayer.getRandom().nextFloat() < 0.4f) {
                    eatTfn(tfnFoodData);
                } else {
                    sourcePlayer.addEffect(new MobEffectInstance(MobEffects.HUNGER, 1800, 1));
                    if (sourcePlayer.getRandom().nextFloat() < 0.15f) {
                        sourcePlayer.addEffect(new MobEffectInstance(MobEffects.POISON, 1800, 0));
                    }
                }
            }
        }
    }

    @Override
    public void eatTfn(TFNFoodData data) {
        NutritionDataAccessor accessor = Accessor.get(nutritionData);

        addThirst(data.nutrients.getOrDefault(terrafirmanutrients$thirstNutrient, 0.0f) / TFCFoodData.MAX_HUNGER * TFCFoodData.MAX_THIRST);

        Set<Nutrient> nutrientsToEat = new HashSet<>(data.nutrients.keySet());
        nutrientsToEat.remove(terrafirmanutrients$thirstNutrient);

        if (nutrientsToEat.isEmpty()) return;

        accessor.addTfnNutrients(data);

        if (this.sourcePlayer instanceof ServerPlayer serverPlayer && nutritionData.getAverageNutrition() >= 0.999) {
            TFCAdvancements.FULL_NUTRITION.trigger(serverPlayer);
        }

        if (data.hunger > 0) {
            // In order to get the exact saturation we want, apply this scaling factor here
            delegate.eat(data.hunger, data.saturation / (2f * data.hunger));
        }

        Set<Nutrient> badNutrients = nutrientsToEat;
        badNutrients.removeAll(accessor.getNutrientSet());

        for (Nutrient badNutrient : badNutrients) {
            AteBadNutrientEvent badNutrientEvent = new AteBadNutrientEvent(sourcePlayer, badNutrient, data.nutrient(badNutrient));
            MinecraftForge.EVENT_BUS.post(badNutrientEvent);
        }
    }

    @Override
    public Nutrient getThirstNutrient() {
        return terrafirmanutrients$thirstNutrient;
    }

    @Override
    public void setThirstNutrient(Nutrient nutrient) {
        terrafirmanutrients$thirstNutrient = nutrient;
    }

    @Override
    public boolean canConsumeNutrientSet(Set<Nutrient> nutrients) {
        if (terrafirmanutrients$thirstNutrient != null && nutrients.contains(terrafirmanutrients$thirstNutrient))
            return true;

        var nutritionDataAccessor = (NutritionDataAccessor) nutritionData;
        for (var nutrient : nutritionDataAccessor.getNutrientSet()) {
            if (nutrients.contains(nutrient))
                return true;
        }

        return false;
    }
}
