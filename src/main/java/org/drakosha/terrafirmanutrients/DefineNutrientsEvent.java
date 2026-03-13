package org.drakosha.terrafirmanutrients;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Event;

import java.util.*;
import java.util.function.Consumer;

public class DefineNutrientsEvent extends Event {
    public static class Patch {
        public ResourceLocation name;
        public float priority;
        public ResourceLocation before;
        public ResourceLocation after;
        public boolean clearPrevious = false;
        public final Set<Nutrient> add = new HashSet<>();
        public final Set<Nutrient> remove = new HashSet<>();
        public final Set<String> removeByNamespace = new HashSet<>();

        public PriorityQueue<Patch> patchesBefore = new PriorityQueue<>(Comparator
                .comparingDouble(patch -> ((Patch)patch).priority)
                .thenComparing(patch -> ((Patch)patch).name));

        public PriorityQueue<Patch> patchesAfter = new PriorityQueue<>(Comparator
                .comparingDouble(patch -> ((Patch)patch).priority)
                .thenComparing(patch -> ((Patch)patch).name));

        public Consumer<Patch> onBuild;

        public Patch(Consumer<Patch> onBuild) {
            this.onBuild = onBuild;
        }

        public Patch withName(ResourceLocation name) {
            this.name = name;
            return this;
        }

        /**
         * Higher = later within one patch list
         */
        public Patch withPriority(float priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Use this if you want to apply your patch after some other patch.
         */
        public Patch before(ResourceLocation name) {
            before = name;
            return this;
        }

        /**
         * Use this if you want to apply your patch before some other patch.
         */
        public Patch after(ResourceLocation name) {
            after = name;
            return this;
        }

        /**
         * Whether to clear nutrition set.
         * Use this with caution.
         */
        public Patch clearPrevious() {
            this.clearPrevious = true;
            return this;
        }

        /**
         * Add specific nutrient to set.
         */
        public Patch add(Nutrient nutrient) {
            add.add(nutrient);
            return this;
        }

        /**
         * Remove specific nutrient from set.
         */
        public Patch remove(Nutrient nutrient) {
            remove.add(nutrient);
            return this;
        }

        /**
         * Remove all nutrients by namespace, e.g. remove vanilla nutrients.
         */
        public Patch removeByNamespace(String namespace) {
            removeByNamespace.add(namespace);
            return this;
        }

        /**
         * Must be called at the end.
         */
        public void build() {
            onBuild.accept(this);
        }

        private Patch findPatch(ResourceLocation name) {
            if (name == this.name) return this;

            Patch result;
            for (var patch : patchesBefore) {
                result = patch.findPatch(name);
                if (result != null) return result;
            }
            for (var patch : patchesAfter) {
                result = patch.findPatch(name);
                if (result != null) return result;
            }
            return null;
        }

        private void forEachPatch(Consumer<Patch> consumer) {
            for (var patch : patchesBefore) {
                patch.forEachPatch(consumer);
            }

            consumer.accept(this);

            for (var patch : patchesAfter) {
                patch.forEachPatch(consumer);
            }
        }
    }

    private final PriorityQueue<Patch> patches = new PriorityQueue<>(Comparator
            .comparingDouble(patch -> ((Patch)patch).priority)
            .thenComparing(patch -> ((Patch)patch).name));

    public final Player player;
    public Nutrient thirstNutrientType = null;

    public DefineNutrientsEvent(Player player) {
        this.player = player;
    }

    /**
     * Use this to create modification for nutrient set. By default, patches are stored within a root list and ordered by creation time.
     * If a patch is marked as <code>before</code> or <code>after</code>, it will be added to another patch-dependent sub-list and will be ordered by priority and then by name.
     */
    public Patch patch() {

        return new Patch((newPatch) -> {
            if (newPatch.name == null) {
                TerraFirmaNutrients.LOGGER.error("Nutrient patch name missing, skipping");
                return;
            }
            if (patches.stream().anyMatch(existingPatch -> existingPatch.name.equals(newPatch.name))) {
                TerraFirmaNutrients.LOGGER.error("Nutrient patch with name {} has already beed registered, skipping", newPatch.name);
                return;
            }
            if (newPatch.after != null && newPatch.before != null) {
                TerraFirmaNutrients.LOGGER.error("Nutrient patch {} have both before and after fields set, which is not allowed, skipping", newPatch.name);
                return;
            }

            if (newPatch.before != null) {
                var parentPatch = findPatch(newPatch.before);
                if (parentPatch != null) {
                    parentPatch.patchesBefore.add(newPatch);
                } else {
                    TerraFirmaNutrients.LOGGER.error("Nutrient patch {} want to be before patch {}, which is missing, skipping", newPatch.name, newPatch.before);
                    return;
                }
            } else if (newPatch.after != null) {
                var parentPatch = findPatch(newPatch.after);
                if (parentPatch != null) {
                    parentPatch.patchesAfter.add(newPatch);
                } else {
                    TerraFirmaNutrients.LOGGER.error("Nutrient patch {} want to be after patch {}, which is missing, skipping", newPatch.name, newPatch.after);
                    return;
                }
            } else {
                patches.add(newPatch);
            }
        });
    }

    private Patch findPatch(ResourceLocation name) {
        for (var patch : patches) {
            Patch result = patch.findPatch(name);
            if (result != null) return result;
        }

        return null;
    }

    public void forEachPatch(Consumer<Patch> consumer) {
        for (var patch : patches) {
            patch.forEachPatch(consumer);
        }
    }

    public Set<Nutrient> collectNutrientSet() {
        Set<Nutrient> result = new HashSet<>();

        forEachPatch(patch -> {
            if (patch.clearPrevious) {
                result.clear();
            }

            if (!patch.removeByNamespace.isEmpty()) {
                result.removeIf(nutrient -> patch.removeByNamespace.contains(NutrientRegistry.getKeyNotNull(nutrient).getNamespace()));
            }

            result.addAll(patch.add);
            result.removeAll(patch.remove);
        });

        return result;
    }
}
