package com.example.modtemplate.platform;

public interface Platform {
	boolean isModLoaded(String modId);

	ModLoader loader();

	enum ModLoader {
		FABRIC, NEOFORGE, FORGE, QUILT
	}
}
