/*
 * Folder Packs is adapted from Packed Packs.
 * Copyright (c) 2025 fishstiz
 * Licensed under the MIT License; see third_party_licenses/packed_packs.txt.
 */

package dev.dediamondpro.resourcify.gui.pack;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreenResourcePacks;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackListEntry;
import net.minecraft.client.resources.ResourcePackListEntryFound;

import dev.dediamondpro.resourcify.util.FolderPackRegistry;

/** UI-only group; the child entries are expanded before Minecraft applies packs. */
public final class FolderPackListEntry extends ResourcePackListEntry {

    private final FolderPackRegistry.Definition definition;
    private final List<ResourcePackListEntryFound> children;

    public FolderPackListEntry(GuiScreenResourcePacks screen, FolderPackRegistry.Definition definition,
        List<ResourcePackListEntryFound> children) {
        super(screen);
        this.definition = definition;
        this.children = new ArrayList<>(children);
    }

    public FolderPackRegistry.Definition getDefinition() {
        return this.definition;
    }

    public List<ResourcePackListEntryFound> getChildren() {
        return this.children;
    }

    @Override
    protected String func_148311_a() {
        return I18n.format("resourcify.folder_pack.description", this.children.size());
    }

    @Override
    protected String func_148312_b() {
        return this.definition.getName();
    }

    @Override
    protected void func_148313_c() {
        FolderPackIcons.bind(this.definition);
    }

    @Override
    public boolean mousePressed(int slot, int x, int y, int button, int relativeX, int relativeY) {
        if (button == 0 && relativeX > 32) {
            FolderPackUi.open(this);
            return true;
        }
        return super.mousePressed(slot, x, y, button, relativeX, relativeY);
    }
}
