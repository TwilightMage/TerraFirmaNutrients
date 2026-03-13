# The Problem
The current TFC approach to a nutrition system implies a hardcoded set of nutrients. This disables compatibility with other mods that allow to play as different species with different diets.

# Example
In [Dragon Survival](https://www.curseforge.com/minecraft/mc-mods/desert-claw-dragon-survival-addon) players can play as different dragon species. Each dragon type has their own diet.

Cave dragon diet is about coal, redstone, lava, and other minerals. They also take damage when touching water and thus drinking water for them makes no logical sense.

Forest dragon is about raw meat and poisonous food, such as spider eye or rotten flesh.

When dragons eat something outside their diet, they get debuffs.

Currently, DS have diets disabled when TFC is present in the same modpack because otherwise, dragons will not be able to eat anything at all.

Writing a compatibility patch for DS and TFC will require creating custom nutrition sets for dragons and their food, which is not possible in vanilla TFC.

# The Solution
Implement `IForgeRegistry` for new `Nutrient` class.

Player diet is defined by set of `Nutrient` objects under `Player`->`(TFCFoodData) foodData`->`(NutritionData) nutritionData`.

Thirst is also defined by `Nutrient` so different species required to drink different liquids, if any.

Standard TFC nutrients are defined in `NutrientRegistry` class.

Player nutrient set is defined in `DefineNutrientsEvent`. This event is emitted on player spawn or respawn.

On consuming nutrient missing from the player nutrient set, `AteBadNutrientEvent` is emitted. This is a good place to inject bad effects for incompatible nutrients.

Since listed changes touch a big part of TFC food system, they should be implemented in a separate addon, until they will be merged into TFC at some point.

Usage example is provided in `example` package, which is excluded from jar builds.

# Changes
- **Nutrient Registry** - Nutrients are now defined in `Nutrients` class. Registry is defined in `NutrientRegistry` class.
- **Nutrient Set** - Required elements are defined via `NutritionDataMixin`. Thirst nutrient is defined in `TFCFoodDataMixin`.
- **Drinkables** - What nutrients liquid contains is defined using `DrinkableMixin`.
- **Food Nutrition** - What nutrients food contains is defined using `FoodDefinitionMixin` and `FoodHandlerMixin`. Also look at `FoodHandlerDynamicMixin` and `IFoodMixin`.
- **Soups** - Soup nutrition composition is defined using `SoupPotRecipeMixin`.

# Events
- `AteBadNutrientEvent` - player consumed nutrient missing in its nutrient set. Provide player, nutrient and amount. Emitted separately for each bad nutrient in food eaten or liquid drank.
- `DefineNutrientsEvent` - player needs a nutrient set to be defined. Provide interface to patch a set and define thirst nutrient.