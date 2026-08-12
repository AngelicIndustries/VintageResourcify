package dev.dediamondpro.resourcify.mixins.early.minecraft;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.dediamondpro.resourcify.VintageResourcify;
import dev.dediamondpro.resourcify.config.Config;
import dev.dediamondpro.resourcify.util.AdditionalPackFolders;

/** Adds packs from configured folders to vanilla's normal repository scan. */
@Mixin(ResourcePackRepository.class)
public abstract class MixinResourcePackRepository {

    @Shadow
    @Final
    protected static FileFilter resourcePackFilter;

    @Shadow
    @Final
    private File dirResourcepacks;

    @Inject(method = "getResourcePackFiles", at = @At("RETURN"), cancellable = true)
    private void resourcify$addAdditionalFolders(CallbackInfoReturnable<List<File>> cir) {
        List<File> files = new ArrayList<>(cir.getReturnValue());
        Set<String> names = new HashSet<>();
        for (File file : files) names.add(file.getName());

        List<File> folders = AdditionalPackFolders.resolve(
            Minecraft.getMinecraft().mcDataDir,
            this.dirResourcepacks,
            Config.Companion.getInstance()
                .getAdditionalResourcePackFolders());

        for (File folder : folders) {
            File[] discovered;
            try {
                discovered = folder.listFiles(resourcePackFilter);
            } catch (Throwable t) {
                VintageResourcify.LOG.warn("Failed to scan additional resource pack folder {}", folder, t);
                continue;
            }
            if (discovered == null) {
                VintageResourcify.LOG.warn("Failed to list additional resource pack folder {}", folder);
                continue;
            }
            for (File file : discovered) {
                // 1.7.10 persists enabled packs by filename, so duplicate
                // names cannot be selected reliably. Prefer the primary
                // folder, then the first configured folder.
                if (names.add(file.getName())) files.add(file);
            }
        }

        cir.setReturnValue(files);
    }
}
