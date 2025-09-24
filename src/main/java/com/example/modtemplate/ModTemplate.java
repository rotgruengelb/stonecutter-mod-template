package com.example.modtemplate;

import com.example.modtemplate.platform.Platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? fabric {
import com.example.modtemplate.platform.fabric.FabricPlatform;
//?} neoforge {
/*import com.example.modtemplate.platform.neoforge.NeoforgePlatform;
 *///?}

@SuppressWarnings("LoggingSimilarMessage")
public class ModTemplate {

    public static final String MOD_ID = /*$ mod_id*/ "modtemplate";
    public static final String MOD_VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MOD_FRIENDLY_NAME = /*$ mod_name*/ MOD_ID;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Platform PLATFORM = createPlatformInstance();

    public static void onInitialize() {
        LOGGER.info("Initializing {} on {}", MOD_ID, ModTemplate.xplat().loader());
    }

    public static void onInitializeClient() {
        LOGGER.info("Initializing {} Client on {}", MOD_ID, ModTemplate.xplat().loader());
        LOGGER.debug("{}: { version: {}; friendly_name: {} }", MOD_ID, MOD_VERSION, MOD_FRIENDLY_NAME);
    }

    static Platform xplat() {
        return PLATFORM;
    }

    private static Platform createPlatformInstance() {
        //? fabric {
        return new FabricPlatform();
        //?} neoforge {
        /*return new NeoforgePlatform();
         *///?}
    }
}
