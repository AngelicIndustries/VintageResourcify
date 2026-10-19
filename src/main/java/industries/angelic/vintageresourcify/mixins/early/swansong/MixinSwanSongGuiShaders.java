package industries.angelic.vintageresourcify.mixins.early.swansong;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ventooth.swansong.gui.GuiShaders;

import industries.angelic.vintageresourcify.gui.pack.PackOverlayRenderer;
import industries.angelic.vintageresourcify.gui.pack.PackScreensAddition;
import industries.angelic.vintageresourcify.services.ProjectType;
import industries.angelic.vintageresourcify.util.SwanSongHelper;

@Mixin(GuiShaders.class)
public class MixinSwanSongGuiShaders {

    @ModifyConstant(method = "updateScreen", constant = @Constant(intValue = 20), require = 1)
    private int resourcify$refreshShaderPacksEveryFiveSeconds(int original) {
        return 100;
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void resourcify$resetDeleteRegions(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        PackOverlayRenderer.INSTANCE.beginFrame();
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void resourcify$drawAddButton(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiScreen self = (GuiScreen) (Object) this;
        PackScreensAddition.INSTANCE
            .onRender(ProjectType.IRIS_SHADER, SwanSongHelper.INSTANCE.getShaderpacksFolder(), self, mouseX, mouseY);
        PackOverlayRenderer.INSTANCE.endFrame();
    }

    @ModifyVariable(method = "drawScreen", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int resourcify$maskMouseXWhenUpdatePanelOpen(int mouseX) {
        return this.resourcify$shouldMaskParentMouse() ? 0 : mouseX;
    }

    @ModifyVariable(method = "drawScreen", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private int resourcify$maskMouseYWhenUpdatePanelOpen(int mouseY) {
        return this.resourcify$shouldMaskParentMouse() ? 0 : mouseY;
    }

    private boolean resourcify$shouldMaskParentMouse() {
        return PackScreensAddition.INSTANCE
            .shouldMaskParentMouse(ProjectType.IRIS_SHADER, SwanSongHelper.INSTANCE.getShaderpacksFolder());
    }

}
