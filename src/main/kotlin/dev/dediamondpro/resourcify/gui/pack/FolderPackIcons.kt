/*
 * Folder Packs is adapted from Packed Packs.
 * Copyright (c) 2025 fishstiz
 * Licensed under the MIT License; see third_party_licenses/packed_packs.txt.
 */

package dev.dediamondpro.resourcify.gui.pack

import dev.dediamondpro.resourcify.util.FolderPackRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** Loads a Folder Pack's optional pack.png, with a crisp built-in fallback. */
object FolderPackIcons {
    private val icons = mutableMapOf<File, ResourceLocation>()

    @JvmStatic
    fun bind(definition: FolderPackRegistry.Definition) {
        val mc = Minecraft.getMinecraft()
        val key = definition.directory.canonicalFile
        val location = icons.getOrPut(key) {
            val custom = File(definition.directory, "pack.png")
                .takeIf(File::isFile)
                ?.let { runCatching { ImageIO.read(it) }.getOrNull() }
            mc.textureManager.getDynamicTextureLocation(
                "folderpack/${definition.id.hashCode()}",
                DynamicTexture(custom ?: fallback()),
            )
        }
        mc.textureManager.bindTexture(location)
    }

    private fun fallback(): BufferedImage {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val edge = 0xFF3B2713.toInt()
        val dark = 0xFFC98528.toInt()
        val light = 0xFFF4C45D.toInt()
        val shine = 0xFFFFDA78.toInt()
        for (y in 8..26) for (x in 3..28) image.setRGB(x, y, edge)
        for (y in 9..25) for (x in 4..27) image.setRGB(x, y, dark)
        for (y in 11..24) for (x in 4..27) image.setRGB(x, y, light)
        for (y in 5..10) for (x in 5..16) image.setRGB(x, y, edge)
        for (y in 6..10) for (x in 6..15) image.setRGB(x, y, light)
        for (x in 5..26) image.setRGB(x, 12, shine)
        return image
    }
}
