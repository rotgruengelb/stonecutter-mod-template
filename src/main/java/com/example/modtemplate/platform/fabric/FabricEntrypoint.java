package com.example.modtemplate.platform.fabric;

//? fabric {

import com.example.modtemplate.ModTemplate;
import net.fabricmc.api.ModInitializer;

public class FabricEntrypoint implements ModInitializer {

	@Override
	public void onInitialize() {
		ModTemplate.onInitialize();
		FabricEventSubscriber.registerEvents();
	}
}
//?}
