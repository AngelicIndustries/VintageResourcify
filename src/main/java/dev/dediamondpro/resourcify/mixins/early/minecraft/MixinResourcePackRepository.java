package dev.dediamondpro.resourcify.mixins.early.minecraft;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

import net.minecraft.client.resources.ResourcePackRepository;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.dediamondpro.resourcify.util.FolderPackRegistry;

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
        cir.setReturnValue(FolderPackRegistry.augment(this.dirResourcepacks, cir.getReturnValue(), resourcePackFilter));
    }
}
