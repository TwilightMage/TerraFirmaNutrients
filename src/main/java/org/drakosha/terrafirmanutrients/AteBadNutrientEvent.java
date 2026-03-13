package org.drakosha.terrafirmanutrients;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

public class AteBadNutrientEvent extends Event {
    public Player player;
    public Nutrient nutrient;
    public float amount;

    public AteBadNutrientEvent(Player player, Nutrient nutrient, float amount) {
        this.player = player;
        this.nutrient = nutrient;
        this.amount = amount;
    }
}
