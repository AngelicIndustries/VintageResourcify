package dev.dediamondpro.resourcify.mixins.early.minecraft;

import java.io.File;

import net.minecraft.client.resources.ResourcePackRepository;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.dediamondpro.resourcify.util.FolderPackRegistry;

/** Gives nested packs collision-free names while leaving normal packs untouched. */
@Mixin(ResourcePackRepository.Entry.class)
public class MixinResourcePackRepositoryEntry {

    @Shadow
    private File resourcePackFile;

    @Inject(method = "getResourcePackName", at = @At("HEAD"), cancellable = true)
    private void resourcify$stableNestedPackName(CallbackInfoReturnable<String> cir) {
        String id = FolderPackRegistry.childId(this.resourcePackFile);
        if (id != null) cir.setReturnValue(id);
    }

    @Inject(method = "toString", at = @At("HEAD"), cancellable = true)
    private void resourcify$stableNestedPackIdentity(CallbackInfoReturnable<String> cir) {
        String id = FolderPackRegistry.childId(this.resourcePackFile);
        if (id != null) {
            cir.setReturnValue(
                String.format(
                    "%s:%s:%d",
                    id,
                    this.resourcePackFile.isDirectory() ? "folder" : "zip",
                    this.resourcePackFile.lastModified()));
        }
    }
}
