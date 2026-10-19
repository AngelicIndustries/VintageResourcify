package industries.angelic.vintageresourcify.mixins.early.angelica;

import net.coderbot.iris.gui.element.ShaderPackSelectionList;
import net.coderbot.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.vintageresourcify.gui.pack.PackOverlayRenderer;
import industries.angelic.vintageresourcify.gui.pack.PackScreensAddition;
import industries.angelic.vintageresourcify.services.ProjectType;
import industries.angelic.vintageresourcify.util.IrisHelper;

/**
 * Adds the Resourcify "+" overlay button to Angelica's Iris shader pack
 * selection screen. Registered only when Angelica is loaded.
 */
@Mixin(ShaderPackScreen.class)
public class MixinShaderPackScreen extends GuiScreen {

    @Shadow(remap = false)
    private ShaderPackSelectionList shaderPackList;

    @Unique
    private int resourcify$refreshTimer;

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (++this.resourcify$refreshTimer >= 100) {
            this.resourcify$refreshTimer = 0;
            if (this.shaderPackList != null) this.shaderPackList.refresh();
        }
    }

    @Inject(method = "drawScreen", at = @At("HEAD"))
    private void resourcify$resetDeleteRegions(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        PackOverlayRenderer.INSTANCE.beginFrame();
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void resourcify$drawAddButton(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        PackScreensAddition.INSTANCE.onRender(
            ProjectType.IRIS_SHADER,
            IrisHelper.INSTANCE.getShaderpacksFolder(),
            (GuiScreen) (Object) this,
            mouseX,
            mouseY);
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

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void resourcify$clickAddOrDelete(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        GuiScreen self = (GuiScreen) (Object) this;
        GuiScreen shaderParent = IrisHelper.INSTANCE.getShaderScreenParent(self);
        boolean deleted = PackOverlayRenderer.INSTANCE.handleDeleteClick(mouseX, mouseY, mouseButton, () -> {
            // Re-open Iris's ShaderPackScreen so the deleted pack vanishes
            // from the list and any "applied" highlight is recomputed.
            IrisHelper.INSTANCE.openShaderPackScreen(shaderParent);
            return kotlin.Unit.INSTANCE;
        });
        if (deleted) {
            ci.cancel();
            return;
        }
        if (PackScreensAddition.INSTANCE.onMouseClick(
            mouseX,
            mouseY,
            mouseButton,
            ProjectType.IRIS_SHADER,
            IrisHelper.INSTANCE.getShaderpacksFolder(),
            self)) {
            ci.cancel();
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void resourcify$keyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (PackScreensAddition.INSTANCE.onKeyTyped(keyCode)) {
            ci.cancel();
        }
    }

    private boolean resourcify$shouldMaskParentMouse() {
        return PackScreensAddition.INSTANCE
            .shouldMaskParentMouse(ProjectType.IRIS_SHADER, IrisHelper.INSTANCE.getShaderpacksFolder());
    }
}
