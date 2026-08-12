package dev.dediamondpro.resourcify.mixins.early.swansong;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ventooth.swansong.resources.ShaderPackManager;
import com.ventooth.swansong.resources.pack.DefaultShaderPack;

import dev.dediamondpro.resourcify.config.Config;
import dev.dediamondpro.resourcify.util.AdditionalPackFolders;

/** Discovers and loads SwanSong shader packs from configured folders. */
@Mixin(ShaderPackManager.class)
public abstract class MixinSwanSongShaderPackManager {

    @Shadow(remap = false)
    private static List<String> detectedShaderpacks;

    @Inject(method = "refreshShaderPackNames", at = @At("TAIL"), remap = false)
    private static void resourcify$addAdditionalShaderFolders(CallbackInfo ci) {
        File gameDirectory = Minecraft.getMinecraft().mcDataDir;
        File primary = new File(gameDirectory, "shaderpacks");
        List<File> folders = AdditionalPackFolders.resolve(
            gameDirectory,
            primary,
            Config.Companion.getInstance()
                .getAdditionalShaderPackFolders());

        Set<String> specialNames = new HashSet<>();
        specialNames.add(ShaderPackManager.DISABLED_SHADER_PACK_NAME);
        specialNames.add(DefaultShaderPack.NAME);
        List<String> packs = new ArrayList<>();
        for (String name : detectedShaderpacks) {
            if (!specialNames.contains(name)) packs.add(name);
        }
        packs.addAll(AdditionalPackFolders.discoverShaderPackNames(folders, detectedShaderpacks, false));
        packs = AdditionalPackFolders.sortShaderPackNames(packs);

        List<String> merged = new ArrayList<>();
        merged.add(ShaderPackManager.DISABLED_SHADER_PACK_NAME);
        if (detectedShaderpacks.contains(DefaultShaderPack.NAME)) merged.add(DefaultShaderPack.NAME);
        merged.addAll(packs);
        detectedShaderpacks = merged;
    }

    @Redirect(
        method = { "resolvePath", "createShaderPack", "saveShaderPackConfig", "readShaderPackConfig" },
        at = @At(
            value = "INVOKE",
            target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;",
            remap = false),
        remap = false)
    private static Path resourcify$resolveAdditionalShaderPath(Path primary, String requestedName) {
        if (requestedName.isEmpty()) return primary.resolve(requestedName);
        String packName = requestedName.endsWith(".txt") ? requestedName.substring(0, requestedName.length() - 4)
            : requestedName;
        return AdditionalPackFolders
            .resolveShaderPackPath(
                Minecraft.getMinecraft().mcDataDir,
                primary.toFile(),
                Config.Companion.getInstance()
                    .getAdditionalShaderPackFolders(),
                packName,
                requestedName,
                false)
            .toPath();
    }
}
