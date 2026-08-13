/*
 * Folder Packs is adapted from Packed Packs.
 * Copyright (c) 2025 fishstiz
 * Licensed under the MIT License; see third_party_licenses/packed_packs.txt.
 */

package dev.dediamondpro.resourcify.util

import com.google.gson.GsonBuilder
import dev.dediamondpro.resourcify.VintageResourcify
import dev.dediamondpro.resourcify.config.Config
import net.minecraft.client.Minecraft
import java.io.File
import java.io.FileFilter
import java.util.concurrent.ConcurrentHashMap

/**
 * Discovers one-level resource-pack containers and exposes stable identities
 * for their real child packs. The synthetic folder itself never enters
 * Minecraft's resource manager.
 */
object FolderPackRegistry {

    const val METADATA_FILE = "packed_packs.folderpack.json"
    private const val ID_PREFIX = "file/"

    data class Definition(
        val id: String,
        val name: String,
        val ownerRoot: File,
        val directory: File,
        val children: List<File>,
    ) {
        fun childId(file: File): String = "$id/${file.name}"
    }

    private data class Metadata(val packIds: MutableList<String> = mutableListOf())

    @Volatile private var definitions: List<Definition> = emptyList()
    @Volatile private var childDefinitions: Map<File, Definition> = emptyMap()
    private val warned = ConcurrentHashMap.newKeySet<String>()

    @JvmStatic
    fun augment(primaryFolder: File, vanillaFiles: Collection<File>, packFilter: FileFilter): List<File> {
        val mc = Minecraft.getMinecraft()
        val roots = AdditionalPackFolders.all(
            mc.mcDataDir,
            primaryFolder,
            Config.instance.additionalResourcePackFolders,
        )
        val result = mutableListOf<File>()
        val directNames = linkedSetOf<String>()
        val seenFolderIds = linkedSetOf<String>()
        val discoveredFolders = mutableListOf<Definition>()

        // Keep vanilla's ordering for the primary root, then append configured
        // roots in their declared order.
        vanillaFiles.forEach { file ->
            if (directNames.add(file.name)) result += canonical(file)
        }

        roots.forEachIndexed { rootIndex, root ->
            val files = safeList(root) ?: return@forEachIndexed
            if (rootIndex > 0) {
                files.filter(packFilter::accept).sortedBy { it.name.lowercase() }.forEach { file ->
                    if (directNames.add(file.name)) result += canonical(file)
                }
            }

            files.asSequence()
                .filter { it.isDirectory && !File(it, "pack.mcmeta").isFile }
                .sortedBy { it.name.lowercase() }
                .forEach { directory ->
                    val id = ID_PREFIX + directory.name
                    if (!seenFolderIds.add(id)) return@forEach
                    val children = safeList(directory)
                        ?.filter(packFilter::accept)
                        ?.sortedBy { it.name.lowercase() }
                        .orEmpty()
                    if (children.isEmpty()) return@forEach

                    val definition = Definition(
                        id = id,
                        name = directory.name,
                        ownerRoot = canonical(root),
                        directory = canonical(directory),
                        children = orderChildren(id, directory, children.map(::canonical)),
                    )
                    discoveredFolders += definition
                    result += definition.children
                }
        }

        definitions = discoveredFolders.toList()
        childDefinitions = buildMap {
            discoveredFolders.forEach { definition ->
                definition.children.forEach { child -> put(canonical(child), definition) }
            }
        }
        discoveredFolders.groupBy { it.ownerRoot }.forEach { (root, folders) ->
            LocalIndex.forFolder(root).reconcileFiles(folders.flatMap { it.children })
        }
        return result
    }

    @JvmStatic
    fun all(): List<Definition> = definitions

    @JvmStatic
    fun definitionFor(file: File?): Definition? = file?.let { childDefinitions[canonical(it)] }

    @JvmStatic
    fun childId(file: File?): String? {
        val child = file ?: return null
        return definitionFor(child)?.childId(child)
    }

    @JvmStatic
    fun stablePackName(file: File): String = childId(file) ?: file.name

    @JvmStatic
    @Synchronized
    fun saveOrder(definition: Definition, orderedChildren: List<File>) {
        val valid = definition.children.associateBy { canonical(it) }
        val ordered = linkedSetOf<File>()
        orderedChildren.forEach { child -> valid[canonical(child)]?.let(ordered::add) }
        definition.children.forEach(ordered::add)
        val metadata = Metadata(ordered.map { definition.childId(it) }.toMutableList())
        try {
            File(definition.directory, METADATA_FILE).bufferedWriter().use { GSON.toJson(metadata, it) }
        } catch (t: Throwable) {
            VintageResourcify.LOG.warn("Could not save Folder Pack order for {}", definition.directory, t)
        }
    }

    /** Keeps a renamed update in the same priority slot inside its Folder Pack. */
    @JvmStatic
    @Synchronized
    fun replaceChild(oldFile: File, newFile: File) {
        val definition = definitionFor(oldFile) ?: return
        val ordered = definition.children.map { if (canonical(it) == canonical(oldFile)) canonical(newFile) else it }
        val metadata = Metadata(ordered.map { definition.childId(it) }.toMutableList())
        try {
            File(definition.directory, METADATA_FILE).bufferedWriter().use { GSON.toJson(metadata, it) }
        } catch (t: Throwable) {
            VintageResourcify.LOG.warn("Could not preserve Folder Pack order after updating {}", oldFile, t)
        }
    }

    private fun orderChildren(id: String, directory: File, children: List<File>): List<File> {
        val metadataFile = File(directory, METADATA_FILE)
        val ids = if (!metadataFile.isFile) emptyList() else try {
            metadataFile.bufferedReader().use { GSON.fromJson(it, Metadata::class.java) }
                ?.packIds.orEmpty().filterNotNull()
        } catch (t: Throwable) {
            VintageResourcify.LOG.warn("Could not read Folder Pack metadata from {}", metadataFile, t)
            emptyList()
        }
        val byId = children.associateBy { "$id/${it.name}" }
        val ordered = linkedSetOf<File>()
        ids.forEach { byId[it]?.let(ordered::add) }
        children.forEach(ordered::add)
        return ordered.toList()
    }

    private fun safeList(directory: File): List<File>? = try {
        directory.listFiles()?.toList().also {
            if (it == null && warned.add(directory.absolutePath)) {
                VintageResourcify.LOG.warn("Could not list resource-pack folder {}", directory)
            }
        }
    } catch (t: Throwable) {
        if (warned.add(directory.absolutePath)) {
            VintageResourcify.LOG.warn("Could not scan resource-pack folder {}", directory, t)
        }
        null
    }

    private fun canonical(file: File): File = try {
        file.canonicalFile
    } catch (_: Throwable) {
        file.absoluteFile
    }

    private val GSON = GsonBuilder().setPrettyPrinting().create()
}
