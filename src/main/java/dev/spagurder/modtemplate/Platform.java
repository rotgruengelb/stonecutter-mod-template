package dev.spagurder.modtemplate;

//? fabric {
import dev.spagurder.modtemplate.fabric.FabricPlatformImpl;
//?}
//? neoforge {
/*import dev.spagurder.modtemplate.neoforge.NeoforgePlatformImpl;
*///?}

public interface Platform {

    //? fabric {
    Platform INSTANCE = new FabricPlatformImpl();
    //?}
    //? neoforge {
    /*Platform INSTANCE = new NeoforgePlatformImpl();
    *///?}


    boolean isModLoaded(String modid);
    String loader();

}
