package dev.spagurder.modtemplate.neoforge;

//? neoforge {
/*import dev.spagurder.modtemplate.ExampleEventHandler; // sample_content
import dev.spagurder.modtemplate.ModTemplate;
import net.minecraft.server.level.ServerPlayer; // sample_content
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent; // sample_content

@Mod(ModTemplate.MOD_ID)
@EventBusSubscriber // sample_content
public class NeoforgeEntrypoint {

    public NeoforgeEntrypoint() {
        ModTemplate.init();
    }
    // sample_content
    @SubscribeEvent // sample_content
    public static void onPlayerDamage(LivingDamageEvent.Post event) { // sample_content
        if (event.getEntity() instanceof ServerPlayer player && event.getNewDamage() > 0) { // sample_content
            ExampleEventHandler.onPlayerHurt(player); // sample_content
        } // sample_content
    } // sample_content

    @EventBusSubscriber(modid = ModTemplate.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            ModTemplate.LOG.info("Initializing {} Client", ModTemplate.MOD_ID);
        }
    }

}
*///?}