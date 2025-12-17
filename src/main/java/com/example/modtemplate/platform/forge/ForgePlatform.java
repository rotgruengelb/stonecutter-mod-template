package com.example.modtemplate.platform.forge;

//? forge {

/*import com.example.modtemplate.platform.Platform;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

public class ForgePlatform implements Platform {

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public ModLoader loader() {
		return ModLoader.FORGE;
	}

	@Override
	public String mcVersion() {
		return "";
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return !FMLLoader.isProduction();
	}
}
*///?}
