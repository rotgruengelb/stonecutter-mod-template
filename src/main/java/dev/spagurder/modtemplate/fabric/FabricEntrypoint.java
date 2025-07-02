package dev.spagurder.modtemplate.fabric;

//? fabric {
import dev.spagurder.modtemplate.ExampleEventHandler; // sample_content
import dev.spagurder.modtemplate.ModTemplate;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents; // sample_content
import net.minecraft.server.level.ServerPlayer; // sample_content

public class FabricEntrypoint implements ModInitializer {

    @Override
    public void onInitialize() {
        ModTemplate.init();
        // sample_content
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (entity instanceof ServerPlayer && damageTaken > 0) { // sample_content
                ExampleEventHandler.onPlayerHurt((ServerPlayer) entity); // sample_content
            } // sample_content
        }); // sample_content
    }

}
//?}