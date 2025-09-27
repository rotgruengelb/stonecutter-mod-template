package com.example.modtemplate.platform.fabric;

//? fabric {

import com.example.modtemplate.ModTemplate;
import net.fabricmc.api.ClientModInitializer;

public class FabricClientEntrypoint implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ModTemplate.onInitializeClient();
	}

}
//?}
