/*
 * This file is part of Resourcify
 * Copyright (C) 2026
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License Version 3 as published by the Free Software Foundation.
 */

package dev.dediamondpro.resourcify.util

import dev.dediamondpro.resourcify.config.Config
import dev.dediamondpro.resourcify.services.IVersion
import dev.dediamondpro.resourcify.services.ProjectType
import net.minecraft.client.Minecraft
import java.io.File

/** Looks up managed installs across the primary and configured additional folders. */
object InstalledPackLookup {

    @JvmStatic
    fun projectEntries(
        type: ProjectType,
        primaryFolder: File,
        platform: String,
        projectId: String,
    ): List<LocalIndex.Entry> = packFolders(type, primaryFolder).flatMap { folder ->
        LocalIndex.forFolder(folder).listEntries().filter { entry ->
            entry.platform == platform && entry.projectId == projectId && File(folder, entry.fileName).exists()
        }
    }

    @JvmStatic
    fun isVersionInstalled(entries: Collection<LocalIndex.Entry>, platform: String, version: IVersion): Boolean {
        val signature = LocalIndex.versionSignature(platform, version)
        return entries.any { entry ->
            entry.installedVersion == signature ||
                (entry.installedVersion == null && entry.fileName == version.getFileName())
        }
    }

    private fun packFolders(type: ProjectType, primaryFolder: File): List<File> {
        val configured = when (type) {
            ProjectType.RESOURCE_PACK,
            ProjectType.AYCY_RESOURCE_PACK -> Config.instance.additionalResourcePackFolders
            ProjectType.IRIS_SHADER,
            ProjectType.OPTIFINE_SHADER -> Config.instance.additionalShaderPackFolders
            else -> emptyList()
        }
        return AdditionalPackFolders.all(Minecraft.getMinecraft().mcDataDir, primaryFolder, configured)
    }
}
