package datapacksscreen.mixin;

import datapacksscreen.AutoReloadMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(at = @At("HEAD"), method = "onWindowFocusChanged")
    private void onFocusChanged(boolean focused, CallbackInfo ci) {
        if (focused) {
            AutoReloadMod.reloadDatapacks();
        }
    }
}
