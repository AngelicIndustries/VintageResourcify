package dev.dediamondpro.resourcify.mixins.early.angelica;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.coderbot.iris.Iris;
import net.coderbot.iris.shaderpack.discovery.ShaderpackDirectoryManager;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.dediamondpro.resourcify.config.Config;
import dev.dediamondpro.resourcify.util.AdditionalPackFolders;

/** Merges configured shader folders into Angelica's selection list. */
@Mixin(ShaderpackDirectoryManager.class)
public class MixinIrisShaderpackDirectoryManager {

    @Inject(method = "enumerate", at = @At("RETURN"), cancellable = true, remap = false)
    private void resourcify$addAdditionalShaderFolders(CallbackInfoReturnable<Collection<String>> cir) {
        Collection<String> original = cir.getReturnValue();
        File primary = Iris.getShaderpacksDirectory()
            .toFile();
        List<File> folders = AdditionalPackFolders.resolve(
            Minecraft.getMinecraft().mcDataDir,
            primary,
            Config.Companion.getInstance()
                .getAdditionalShaderPackFolders());
        List<String> merged = new ArrayList<>(original);
        merged.addAll(AdditionalPackFolders.discoverShaderPackNames(folders, original, true));
        cir.setReturnValue(AdditionalPackFolders.sortShaderPackNames(merged));
    }
}
