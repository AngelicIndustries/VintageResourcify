package dev.dediamondpro.resourcify.mixins.early.angelica;

import java.io.File;
import java.nio.file.Path;

import net.coderbot.iris.Iris;
import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.dediamondpro.resourcify.config.Config;
import dev.dediamondpro.resourcify.util.AdditionalPackFolders;

/** Resolves Angelica's selected shader and its settings beside the real pack. */
@Mixin(Iris.class)
public class MixinIrisAdditionalShaderFolders {

    @Unique
    private static final ThreadLocal<String> resourcify$loadingPack = new ThreadLocal<>();

    @Inject(method = "loadExternalShaderpack", at = @At("HEAD"), remap = false)
    private static void resourcify$capturePackName(String name, CallbackInfoReturnable<Boolean> cir) {
        resourcify$loadingPack.set(name);
    }

    @Inject(method = "loadExternalShaderpack", at = @At("RETURN"), remap = false)
    private static void resourcify$clearPackName(String name, CallbackInfoReturnable<Boolean> cir) {
        resourcify$loadingPack.remove();
    }

    @Redirect(
        method = "loadExternalShaderpack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/coderbot/iris/Iris;getShaderpacksDirectory()Ljava/nio/file/Path;",
            ordinal = 0),
        remap = false)
    private static Path resourcify$resolveShaderPackFolder() {
        return resourcify$selectedFolder();
    }

    @Redirect(
        method = "loadExternalShaderpack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/coderbot/iris/Iris;getShaderpacksDirectory()Ljava/nio/file/Path;",
            ordinal = 1),
        remap = false)
    private static Path resourcify$resolveShaderSettingsFolder() {
        return resourcify$selectedFolder();
    }

    @Unique
    private static Path resourcify$selectedFolder() {
        File primary = Iris.getShaderpacksDirectory()
            .toFile();
        String packName = resourcify$loadingPack.get();
        if (packName == null) return primary.toPath();
        File pack = AdditionalPackFolders.findLoadableShaderPackFile(
            Minecraft.getMinecraft().mcDataDir,
            primary,
            Config.Companion.getInstance()
                .getAdditionalShaderPackFolders(),
            packName,
            true);
        return (pack == null ? primary : pack.getParentFile()).toPath();
    }
}
