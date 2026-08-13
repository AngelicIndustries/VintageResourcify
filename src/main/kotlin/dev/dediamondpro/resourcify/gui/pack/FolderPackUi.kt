/*
 * Folder Packs is adapted from Packed Packs.
 * Copyright (c) 2025 fishstiz
 * Licensed under the MIT License; see third_party_licenses/packed_packs.txt.
 */

package dev.dediamondpro.resourcify.gui.pack

import dev.dediamondpro.resourcify.gui.ResourcifyStyle
import dev.dediamondpro.resourcify.mixins.early.minecraft.ResourcePackRepositoryEntryAccessor
import dev.dediamondpro.resourcify.util.FolderPackRegistry
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreenResourcePacks
import net.minecraft.client.resources.I18n
import net.minecraft.client.resources.ResourcePackListEntry
import net.minecraft.client.resources.ResourcePackListEntryDefault
import net.minecraft.client.resources.ResourcePackListEntryFound
import net.minecraft.client.resources.ResourcePackRepository
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.io.File
import kotlin.math.max
import kotlin.math.min

/** Groups nested packs and owns the lightweight, same-screen order editor. */
object FolderPackUi {
    private var screen: GuiScreenResourcePacks? = null
    private var editor: FolderPackListEntry? = null
    private var selectedRow = 0
    private var scroll = 0

    @JvmStatic
    fun initialize(
        target: GuiScreenResourcePacks,
        available: MutableList<ResourcePackListEntry>,
        selected: MutableList<ResourcePackListEntry>,
    ) {
        screen = target
        editor = null
        collapseExisting(target, available, selected)
    }

    @JvmStatic
    fun open(entry: FolderPackListEntry) {
        editor = entry
        selectedRow = 0
        scroll = 0
    }

    @JvmStatic
    fun isOpen(target: GuiScreenResourcePacks): Boolean = screen === target && editor != null

    @JvmStatic
    fun refresh(
        target: GuiScreenResourcePacks,
        available: MutableList<ResourcePackListEntry>,
        selected: MutableList<ResourcePackListEntry>,
    ) {
        val tokens = selected.mapNotNull(::token)
        val openId = editor?.definition?.id
        val repository = Minecraft.getMinecraft().resourcePackRepository
        repository.updateRepositoryEntriesAll()
        rebuild(target, available, selected, repository.repositoryEntriesAll, tokens)
        editor = (available + selected)
            .filterIsInstance<FolderPackListEntry>()
            .firstOrNull { it.definition.id == openId }
        clampEditor()
    }

    @JvmStatic
    fun expandSelected(selected: MutableList<ResourcePackListEntry>) {
        val expanded = selected.flatMap { entry ->
            if (entry is FolderPackListEntry) entry.children else listOf(entry)
        }
        selected.clear()
        selected.addAll(expanded)
    }

    @JvmStatic
    fun render(target: GuiScreenResourcePacks, ignoredMouseX: Int, ignoredMouseY: Int) {
        val folder = editor?.takeIf { screen === target } ?: return
        val mc = Minecraft.getMinecraft()
        val font = mc.fontRenderer
        val style = ResourcifyStyle.palette("default")
        val layout = layout(target)
        // drawScreen's mouse arguments are intentionally masked while a
        // modal is open. Read LWJGL directly, as the update popup does.
        val (mouseX, mouseY) = scaledMousePosition(target)

        Gui.drawRect(0, 0, target.width, target.height, style.overlayScrimStrong)
        Gui.drawRect(layout.panelX, layout.panelY, layout.panelX + layout.panelW, layout.panelY + layout.panelH, style.panelRaised)
        FolderPackIcons.bind(folder.definition)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        Gui.func_146110_a(layout.panelX + 8, layout.panelY + 5, 0f, 0f, 24, 24, 24f, 24f)
        val headingX = layout.panelX + 40
        font.drawString(folder.definition.name, headingX, layout.panelY + 4, style.textPrimary, false)
        font.drawString(
            I18n.format("resourcify.folder_pack.editor_hint"),
            headingX,
            layout.panelY + 17,
            style.textSecondary,
            false,
        )
        drawTextButton(
            layout.closeX,
            layout.closeY,
            CLOSE_SIZE,
            CLOSE_SIZE,
            "x",
            mouseX,
            mouseY,
            style,
        )

        Gui.drawRect(layout.listX, layout.listY, layout.listX + layout.listW, layout.listY + layout.listH, style.panelInset)
        val visible = max(1, layout.listH / ROW_HEIGHT)
        scroll = scroll.coerceIn(0, max(0, folder.children.size - visible))
        folder.children.drop(scroll).take(visible).forEachIndexed { visibleIndex, child ->
            val index = scroll + visibleIndex
            val y = layout.listY + visibleIndex * ROW_HEIGHT
            val hovered = inside(mouseX, mouseY, layout.listX, y, layout.listW, ROW_HEIGHT - 1)
            val colour = when {
                hovered -> style.rowHover
                index == selectedRow -> style.rowSelected
                index % 2 == 0 -> style.rowIdle
                else -> style.rowDisabled
            }
            Gui.drawRect(layout.listX, y, layout.listX + layout.listW, y + ROW_HEIGHT - 1, colour)
            if (index == selectedRow) {
                Gui.drawRect(layout.listX, y + ROW_HEIGHT - 2, layout.listX + layout.listW, y + ROW_HEIGHT - 1, style.accent)
            }
            val controls = rowControls(layout, y)
            drawChild(child, layout.listX + 5, y + 5, controls.upX - layout.listX - 44, style)
            font.drawString("${index + 1}", controls.upX - 15, y + 15, style.textSecondary, false)
            drawArrowButton(controls.upX, controls.y, false, mouseX, mouseY, index > 0, style)
            drawArrowButton(controls.downX, controls.y, true, mouseX, mouseY, index < folder.children.lastIndex, style)
        }
        drawScrollBar(layout, folder.children.size, visible, style)
        font.drawString(
            I18n.format("resourcify.folder_pack.count", folder.children.size),
            layout.panelX + PANEL_PADDING,
            layout.footerY + 7,
            style.textSecondary,
            false,
        )
        drawTextButton(
            layout.doneX,
            layout.footerY,
            DONE_WIDTH,
            BUTTON_HEIGHT,
            I18n.format("gui.done"),
            mouseX,
            mouseY,
            style,
        )
    }

    @JvmStatic
    fun mouseClicked(target: GuiScreenResourcePacks, mouseX: Int, mouseY: Int, button: Int): Boolean {
        val folder = editor?.takeIf { screen === target } ?: return false
        if (button != 0) return true
        val layout = layout(target)
        if (inside(mouseX, mouseY, layout.closeX, layout.closeY, CLOSE_SIZE, CLOSE_SIZE) ||
            inside(mouseX, mouseY, layout.doneX, layout.footerY, DONE_WIDTH, BUTTON_HEIGHT)) {
            playClick()
            closeEditor()
            return true
        }
        if (inside(mouseX, mouseY, layout.listX, layout.listY, layout.listW, layout.listH)) {
            val index = scroll + (mouseY - layout.listY) / ROW_HEIGHT
            if (index in folder.children.indices) {
                selectedRow = index
                val y = layout.listY + (index - scroll) * ROW_HEIGHT
                val controls = rowControls(layout, y)
                when {
                    inside(mouseX, mouseY, controls.upX, controls.y, CONTROL_SIZE, CONTROL_SIZE) && index > 0 -> {
                        playClick()
                        move(-1)
                    }
                    inside(mouseX, mouseY, controls.downX, controls.y, CONTROL_SIZE, CONTROL_SIZE) && index < folder.children.lastIndex -> {
                        playClick()
                        move(1)
                    }
                }
            }
        }
        return true
    }

    @JvmStatic
    fun keyTyped(target: GuiScreenResourcePacks, keyCode: Int): Boolean {
        val folder = editor?.takeIf { screen === target } ?: return false
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                playClick()
                closeEditor()
            }
            Keyboard.KEY_UP -> if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) move(-1)
                else selectedRow = max(0, selectedRow - 1)
            Keyboard.KEY_DOWN -> if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) move(1)
                else selectedRow = min(folder.children.lastIndex, selectedRow + 1)
            else -> return true
        }
        ensureVisible(target)
        return true
    }

    @JvmStatic
    fun mouseInput(target: GuiScreenResourcePacks): Boolean {
        val folder = editor?.takeIf { screen === target } ?: return false
        val wheel = Mouse.getEventDWheel()
        if (wheel != 0) {
            scroll += if (wheel < 0) 1 else -1
            val visible = max(1, layout(target).listH / ROW_HEIGHT)
            scroll = scroll.coerceIn(0, max(0, folder.children.size - visible))
        }
        // As with the update popup, ordinary mouse press/release events must
        // continue to mouseClicked. Only consume a real wheel event here.
        return wheel != 0
    }

    private fun collapseExisting(
        target: GuiScreenResourcePacks,
        available: MutableList<ResourcePackListEntry>,
        selected: MutableList<ResourcePackListEntry>,
    ) {
        val selectedFiles = selected.mapNotNull(::backingFile).map(::canonical).toSet()
        val entries = (available + selected).filterIsInstance<ResourcePackListEntryFound>()
        val byFile = entries.associateBy { canonical(backingFile(it)!!) }
        val wrappers = FolderPackRegistry.all().mapNotNull { definition ->
            val children = definition.children.mapNotNull { byFile[canonical(it)] }
            if (children.isEmpty()) null else definition to FolderPackListEntry(target, definition, children)
        }.toMap()
        rewriteCollapsed(available, wrappers, selectedFiles, false)
        rewriteCollapsed(selected, wrappers, selectedFiles, true)
    }

    private fun rewriteCollapsed(
        list: MutableList<ResourcePackListEntry>,
        wrappers: Map<FolderPackRegistry.Definition, FolderPackListEntry>,
        selectedFiles: Set<File>,
        selectedSide: Boolean,
    ) {
        val result = mutableListOf<ResourcePackListEntry>()
        val emitted = mutableSetOf<String>()
        list.forEach { entry ->
            val definition = backingFile(entry)?.let(FolderPackRegistry::definitionFor)
            if (definition == null) {
                result += entry
            } else {
                val belongsSelected = definition.children.any { canonical(it) in selectedFiles }
                if (belongsSelected == selectedSide && emitted.add(definition.id)) wrappers[definition]?.let(result::add)
            }
        }
        // A partially selected folder may first occur on the opposite side.
        wrappers.forEach { (definition, wrapper) ->
            val belongsSelected = definition.children.any { canonical(it) in selectedFiles }
            if (belongsSelected == selectedSide && emitted.add(definition.id)) {
                val defaultIndex = result.indexOfFirst { it is ResourcePackListEntryDefault }
                if (defaultIndex >= 0) result.add(defaultIndex, wrapper) else result += wrapper
            }
        }
        list.clear()
        list.addAll(result)
    }

    private fun rebuild(
        target: GuiScreenResourcePacks,
        available: MutableList<ResourcePackListEntry>,
        selected: MutableList<ResourcePackListEntry>,
        repositoryEntries: List<ResourcePackRepository.Entry>,
        selectedTokens: List<String>,
    ) {
        val found = repositoryEntries.map { ResourcePackListEntryFound(target, it) }
        val byName = found.associateBy { it.func_148318_i().resourcePackName }
        val byFile = found.associateBy { canonical(backingFile(it)!!) }
        val wrappers = FolderPackRegistry.all().associate { definition ->
            definition.id to FolderPackListEntry(
                target,
                definition,
                definition.children.mapNotNull { byFile[canonical(it)] },
            )
        }.filterValues { it.children.isNotEmpty() }
        val chosen = selectedTokens.mapNotNull { wrappers[it] ?: byName[it] }.distinct()
        selected.clear()
        selected.addAll(chosen)
        selected += ResourcePackListEntryDefault(target)

        val chosenTokens = selectedTokens.toSet()
        available.clear()
        val emittedFolders = mutableSetOf<String>()
        found.forEach { entry ->
            val definition = backingFile(entry)?.let(FolderPackRegistry::definitionFor)
            if (definition != null) {
                if (definition.id !in chosenTokens && emittedFolders.add(definition.id)) wrappers[definition.id]?.let(available::add)
            } else if (entry.func_148318_i().resourcePackName !in chosenTokens) {
                available += entry
            }
        }
    }

    private fun token(entry: ResourcePackListEntry): String? = when (entry) {
        is FolderPackListEntry -> entry.definition.id
        is ResourcePackListEntryFound -> entry.func_148318_i().resourcePackName
        else -> null
    }

    private fun backingFile(entry: ResourcePackListEntry): File? =
        (entry as? ResourcePackListEntryFound)?.func_148318_i()?.let {
            (it as ResourcePackRepositoryEntryAccessor).resourcePackFile
        }

    private fun move(delta: Int) {
        val folder = editor ?: return
        val destination = selectedRow + delta
        if (selectedRow !in folder.children.indices || destination !in folder.children.indices) return
        val moved = folder.children.removeAt(selectedRow)
        folder.children.add(destination, moved)
        selectedRow = destination
        FolderPackRegistry.saveOrder(folder.definition, folder.children.mapNotNull(::backingFile))
    }

    private fun closeEditor() {
        editor = null
    }

    private fun ensureVisible(target: GuiScreenResourcePacks) {
        val visible = max(1, layout(target).listH / ROW_HEIGHT)
        if (selectedRow < scroll) scroll = selectedRow
        if (selectedRow >= scroll + visible) scroll = selectedRow - visible + 1
    }

    private fun clampEditor() {
        val last = editor?.children?.lastIndex ?: -1
        selectedRow = selectedRow.coerceIn(0, max(0, last))
    }

    private fun layout(target: GuiScreenResourcePacks): Layout {
        val panelW = min(PANEL_WIDTH, target.width - PANEL_MARGIN * 2).coerceAtLeast(240)
        val preferredRows = editor?.children?.size?.coerceIn(3, MAX_VISIBLE_ROWS) ?: 3
        val wantedH = HEADER_HEIGHT + preferredRows * ROW_HEIGHT + FOOTER_HEIGHT
        val panelH = min(wantedH, target.height - PANEL_MARGIN * 2).coerceAtLeast(190)
        val panelX = (target.width - panelW) / 2
        val panelY = (target.height - panelH) / 2
        val listX = panelX + PANEL_PADDING
        val listY = panelY + HEADER_HEIGHT
        val listW = panelW - PANEL_PADDING * 2
        val footerY = panelY + panelH - FOOTER_HEIGHT
        val listH = (footerY - listY).coerceAtLeast(ROW_HEIGHT)
        return Layout(
            panelX, panelY, panelW, panelH,
            listX, listY, listW, listH,
            panelX + panelW - PANEL_PADDING - CLOSE_SIZE, panelY + 6,
            panelX + panelW - PANEL_PADDING - DONE_WIDTH, footerY + 4,
        )
    }

    private fun drawTextButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        text: String,
        mouseX: Int,
        mouseY: Int,
        style: ResourcifyStyle.Palette,
    ) {
        val hovered = inside(mouseX, mouseY, x, y, width, height)
        Gui.drawRect(x, y, x + width, y + height, if (hovered) style.buttonHover else style.buttonIdle)
        if (hovered) Gui.drawRect(x, y + height - 1, x + width, y + height, style.accent)
        val font = Minecraft.getMinecraft().fontRenderer
        font.drawString(text, x + (width - font.getStringWidth(text)) / 2, y + (height - font.FONT_HEIGHT) / 2, style.textPrimary, false)
    }

    private fun drawArrowButton(
        x: Int,
        y: Int,
        down: Boolean,
        mouseX: Int,
        mouseY: Int,
        enabled: Boolean,
        style: ResourcifyStyle.Palette,
    ) {
        val hovered = enabled && inside(mouseX, mouseY, x, y, CONTROL_SIZE, CONTROL_SIZE)
        val fill = when {
            !enabled -> style.buttonDisabled
            hovered -> style.buttonHover
            else -> style.buttonIdle
        }
        Gui.drawRect(x, y, x + CONTROL_SIZE, y + CONTROL_SIZE, fill)
        if (hovered) Gui.drawRect(x, y + CONTROL_SIZE - 1, x + CONTROL_SIZE, y + CONTROL_SIZE, style.accent)
        val color = if (enabled) style.textPrimary else style.textSecondary
        val centerX = x + CONTROL_SIZE / 2
        val centerY = y + CONTROL_SIZE / 2 + if (down) 1 else -1
        for (offset in 0..3) {
            val yy = if (down) centerY + 2 - offset else centerY - 2 + offset
            Gui.drawRect(centerX - offset, yy, centerX + offset + 1, yy + 1, color)
        }
    }

    private fun drawChild(child: ResourcePackListEntryFound, x: Int, y: Int, textWidth: Int, style: ResourcifyStyle.Palette) {
        val mc = Minecraft.getMinecraft()
        val entry = child.func_148318_i()
        entry.bindTexturePackIcon(mc.textureManager)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        Gui.func_146110_a(x, y, 0f, 0f, PACK_ICON_SIZE, PACK_ICON_SIZE, PACK_ICON_SIZE.toFloat(), PACK_ICON_SIZE.toFloat())
        val file = backingFile(child)
        val rawName = file?.name ?: entry.resourcePackName
        val name = mc.fontRenderer.trimStringToWidth(rawName, textWidth)
        mc.fontRenderer.drawString(name, x + PACK_ICON_SIZE + 6, y + 1, style.textPrimary, false)
        val description = mc.fontRenderer.trimStringToWidth(entry.texturePackDescription, textWidth)
        mc.fontRenderer.drawString(description, x + PACK_ICON_SIZE + 6, y + 15, style.textSecondary, false)
    }

    private fun drawScrollBar(layout: Layout, count: Int, visible: Int, style: ResourcifyStyle.Palette) {
        if (count <= visible) return
        val barX = layout.listX + layout.listW - ResourcifyStyle.SODIUM_SCROLLBAR_WIDTH
        val maxScroll = count - visible
        val thumbH = (layout.listH * visible.toFloat() / count).toInt().coerceIn(18, layout.listH)
        val thumbY = layout.listY + ((layout.listH - thumbH) * scroll.toFloat() / maxScroll).toInt()
        Gui.drawRect(barX, layout.listY, barX + ResourcifyStyle.SODIUM_SCROLLBAR_WIDTH, layout.listY + layout.listH, ResourcifyStyle.SODIUM_SCROLLBAR_TRACK)
        Gui.drawRect(barX, thumbY, barX + ResourcifyStyle.SODIUM_SCROLLBAR_WIDTH, thumbY + thumbH, ResourcifyStyle.SODIUM_SCROLLBAR_THUMB)
    }

    private fun rowControls(layout: Layout, rowY: Int): RowControls {
        val downX = layout.listX + layout.listW - CONTROL_SIZE - 7
        return RowControls(downX - CONTROL_SIZE - 4, downX, rowY + (ROW_HEIGHT - CONTROL_SIZE) / 2)
    }

    private fun scaledMousePosition(target: GuiScreenResourcePacks): Pair<Int, Int> {
        val mc = Minecraft.getMinecraft()
        if (mc.displayWidth <= 0 || mc.displayHeight <= 0) return 0 to 0
        return Mouse.getX() * target.width / mc.displayWidth to
            target.height - Mouse.getY() * target.height / mc.displayHeight - 1
    }

    private fun playClick() {
        Minecraft.getMinecraft().soundHandler.playSound(
            PositionedSoundRecord.func_147674_a(BUTTON_SOUND, 1f),
        )
    }

    private fun inside(mx: Int, my: Int, x: Int, y: Int, width: Int, height: Int) =
        mx >= x && mx < x + width && my >= y && my < y + height

    private fun canonical(file: File): File = runCatching { file.canonicalFile }.getOrDefault(file.absoluteFile)

    private data class Layout(
        val panelX: Int,
        val panelY: Int,
        val panelW: Int,
        val panelH: Int,
        val listX: Int,
        val listY: Int,
        val listW: Int,
        val listH: Int,
        val closeX: Int,
        val closeY: Int,
        val doneX: Int,
        val footerY: Int,
    )

    private data class RowControls(val upX: Int, val downX: Int, val y: Int)

    private const val PANEL_WIDTH = 390
    private const val PANEL_MARGIN = 8
    private const val PANEL_PADDING = 8
    private const val HEADER_HEIGHT = 34
    private const val FOOTER_HEIGHT = 31
    private const val ROW_HEIGHT = 38
    private const val MAX_VISIBLE_ROWS = 6
    private const val CONTROL_SIZE = 18
    private const val CLOSE_SIZE = 18
    private const val DONE_WIDTH = 58
    private const val BUTTON_HEIGHT = 22
    private const val PACK_ICON_SIZE = 28
    private val BUTTON_SOUND = ResourceLocation("gui.button.press")
}
