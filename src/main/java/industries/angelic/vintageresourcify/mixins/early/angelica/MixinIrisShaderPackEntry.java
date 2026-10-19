package industries.angelic.vintageresourcify.mixins.early.angelica;

import net.coderbot.iris.gui.element.shaderselection.ShaderPackEntry;
import net.coderbot.iris.gui.screen.ShaderPackScreen;
import net.minecraft.client.renderer.Tessellator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.vintageresourcify.gui.pack.PackOverlayRenderer;
import industries.angelic.vintageresourcify.services.ProjectType;

/**
 * Draw the platform badge near the right edge of an Iris shader pack entry.
 * Shaders have no thumbnail to anchor to, so we put the badge inside the row
 * just inside the right padding.
 */
@Mixin(ShaderPackEntry.class)
public class MixinIrisShaderPackEntry {

    @Inject(method = "drawEntry", at = @At("TAIL"), remap = false)
    private void resourcify$drawPlatformBadge(ShaderPackScreen screen, int index, int x, int y, int listWidth,
        Tessellator tessellator, int mouseX, int mouseY, boolean isMouseOver, CallbackInfo ci) {
        String packName = ((ShaderPackEntry) (Object) this).getPackName();
        if (isMouseOver) {
            PackOverlayRenderer.INSTANCE.markShaderEntryHovered(screen, packName);
        }
        java.io.File folder = PackOverlayRenderer.INSTANCE.shaderpacksFolder();
        java.io.File file = PackOverlayRenderer.INSTANCE.shaderPackFile(folder, packName);
        if (file == null) return;
        folder = file.getParentFile();
        int cross = 16;
        int badge = 10;
        // Always reserve cross-width on the right so badge position stays
        // stable whether or not the row is hovered.
        int xCross = x + listWidth - cross - 10;
        int xGear = xCross - cross - 4;
        boolean hasUpdate = PackOverlayRenderer.INSTANCE.hasAvailableUpdate(folder, file);
        int xUpdate = xGear - cross - 4;
        // Keep the source badge aligned with the other rows while idle. It
        // only needs to move left when the hovered row exposes the update
        // button alongside the settings and delete controls.
        int xBadge = (hasUpdate && isMouseOver ? xUpdate : xGear) - badge - 4;
        int yMid = y - 1;
        String platform = PackOverlayRenderer.INSTANCE.lookupPlatform(folder, file);
        if (platform != null) {
            PackOverlayRenderer.INSTANCE.drawBadge(platform, xBadge, yMid, badge, mouseX, mouseY);
        }
        if (hasUpdate && !isMouseOver) {
            PackOverlayRenderer.INSTANCE.drawUpdateDot(xBadge, yMid, badge);
        }
        if (isMouseOver) {
            // Iris draws the row label at y; the 16px cross visually
            // centers on the ~9px text band when offset upward by half
            // the diff.
            int yCross = y - 4;
            if (hasUpdate) {
                PackOverlayRenderer.INSTANCE
                    .drawUpdateButton(folder, file, ProjectType.IRIS_SHADER, xUpdate, yCross, cross, mouseX, mouseY);
            }
            PackOverlayRenderer.INSTANCE
                .drawSettingsButton(folder, file, ProjectType.IRIS_SHADER, xGear, yCross, cross, mouseX, mouseY);
            PackOverlayRenderer.INSTANCE
                .drawDeleteButton(folder, file, packName, xCross, yCross, cross, mouseX, mouseY);
        }
    }
}
