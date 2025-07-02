package dev.spagurder.modtemplate.mixin;

import dev.spagurder.modtemplate.ModTemplate;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class ExampleMixin {

    @Inject(method = "loadLevel", at = @At("RETURN"))
    private void afterLoadLevel(CallbackInfo ci) {
        ModTemplate.LOG.info("Level Loaded!");
    }

}
