package com.example.modtemplate.platform.fabric.datagen;

//? fabric {

import com.example.modtemplate.ModTemplate;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends FabricRecipeProvider {

	public ModRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, registriesFuture);
	}

	@Override
	protected RecipeProvider createRecipeProvider(HolderLookup.Provider registryLookup, RecipeOutput exporter) {
		return new RecipeProvider(registryLookup, exporter) {
			@Override
			public void buildRecipes() {
				final var itemLookup = registryLookup.lookupOrThrow(Registries.ITEM);
				ShapelessRecipeBuilder.shapeless(itemLookup, RecipeCategory.FOOD, Items.COOKED_CHICKEN).requires(Items.LAVA_BUCKET).requires(Items.CHICKEN).unlockedBy("has_lava_bucket", has(Items.LAVA_BUCKET)).unlockedBy("has_chicken", has(Items.CHICKEN)).save(exporter, "lava_chicken_recipe");
			}
		};
	}

	@Override
	public String getName() {
		return ModTemplate.MOD_ID + "_recipe_provider";
	}

}
//?}
