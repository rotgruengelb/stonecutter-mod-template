package com.example.modtemplate.platform.fabric.datagen;

//? fabric {
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;

public class FabricDataGeneratorEntrypoint implements DataGeneratorEntrypoint {

	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		final FabricDataGenerator.Pack pack = generator.createPack();
		pack.addProvider((FabricDataOutput output) -> new ModRecipeProvider(output, generator.getRegistries()));
	}

}
//?}
