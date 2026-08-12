/*
 * Additional Folders is adapted from Packed Packs.
 * Copyright (c) 2025 fishstiz
 * Licensed under the MIT License; see third_party_licenses/packed_packs.txt.
 */

package dev.dediamondpro.resourcify.util

import dev.dediamondpro.resourcify.VintageResourcify
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

/** Resolves configured pack folders relative to the game directory. */
object AdditionalPackFolders {

    private data class ResolutionKey(
        val gameDirectory: String,
        val primaryFolder: String,
        val configuredPaths: List<String>,
    )

    private data class Resolution(val primary: File, val additional: List<File>)

    private data class ShaderResolutionKey(
        val folders: ResolutionKey,
        val packName: String,
        val allowNestedZipRoot: Boolean,
    )

    private val resolutionCache = ConcurrentHashMap<ResolutionKey, Resolution>()
    private val shaderResolutionCache = ConcurrentHashMap<ShaderResolutionKey, File>()
    private val warnedPaths = ConcurrentHashMap.newKeySet<String>()

    @JvmStatic
    fun resolve(gameDirectory: File, primaryFolder: File, configuredPaths: Collection<String>?): List<File> =
        resolveFolders(gameDirectory, primaryFolder, configuredPaths).additional

    @JvmStatic
    fun all(gameDirectory: File, primaryFolder: File, configuredPaths: Collection<String>?): List<File> {
        val resolution = resolveFolders(gameDirectory, primaryFolder, configuredPaths)
        return listOf(resolution.primary) + resolution.additional
    }

    private fun resolveFolders(
        gameDirectory: File,
        primaryFolder: File,
        configuredPaths: Collection<String>?,
    ): Resolution {
        val paths = configuredPaths?.toList().orEmpty()
        val key = ResolutionKey(gameDirectory.absolutePath, primaryFolder.absolutePath, paths)
        resolutionCache[key]?.takeIf { cached -> cached.additional.all { it.isDirectory } }?.let { return it }

        val primary = canonicalFile(primaryFolder)
        val seen = linkedSetOf(primary)
        val resolvedFolders = mutableListOf<File>()
        var cacheable = true

        for (configuredPath in paths) {
            if (configuredPath.isBlank()) continue

            try {
                val path = File(configuredPath)
                val resolved = canonicalFile(if (path.isAbsolute) path else File(gameDirectory, configuredPath))
                if (!resolved.isDirectory) {
                    cacheable = false
                    if (warnedPaths.add("invalid:${key.gameDirectory}:$configuredPath")) {
                        VintageResourcify.LOG.warn(
                            "Additional pack folder is not a valid directory: {}, ignoring it",
                            configuredPath,
                        )
                    }
                    continue
                }
                if (seen.add(resolved)) resolvedFolders.add(resolved)
            } catch (t: Throwable) {
                cacheable = false
                if (warnedPaths.add("error:${key.gameDirectory}:$configuredPath")) {
                    VintageResourcify.LOG.warn(
                        "Failed to resolve additional pack folder: {}, ignoring it",
                        configuredPath,
                        t,
                    )
                }
            }
        }

        val resolution = Resolution(primary, resolvedFolders)
        if (cacheable) {
            if (resolutionCache.size > 64) resolutionCache.clear()
            resolutionCache[key] = resolution
        }
        return resolution
    }

    @JvmStatic
    fun containingFolder(file: File, folders: Collection<File>): File? {
        val parent = canonicalFile(file).parentFile ?: return null
        return folders.firstOrNull { canonicalFile(it) == parent }
    }

    @JvmStatic
    fun discoverShaderPackNames(
        folders: Collection<File>,
        existingNames: Collection<String>,
        allowNestedZipRoot: Boolean,
    ): List<String> {
        val names = existingNames.toMutableSet()
        val discovered = mutableListOf<String>()
        for (folder in folders) {
            val files = try {
                folder.listFiles()
            } catch (t: Throwable) {
                VintageResourcify.LOG.warn("Failed to scan additional shader pack folder {}", folder, t)
                null
            }
            if (files == null) {
                VintageResourcify.LOG.warn("Failed to list additional shader pack folder {}", folder)
                continue
            }
            for (file in files) {
                if (isShaderPack(file, allowNestedZipRoot) && names.add(file.name)) discovered.add(file.name)
            }
        }
        return sortShaderPackNames(discovered)
    }

    @JvmStatic
    fun sortShaderPackNames(names: Collection<String>): List<String> = names.sortedWith { first, second ->
        val a = removeFormatting(first)
        val b = removeFormatting(second)
        val insensitive = a.lowercase(Locale.ROOT).compareTo(b.lowercase(Locale.ROOT))
        if (insensitive != 0) insensitive else a.compareTo(b)
    }

    @JvmStatic
    fun findShaderPackFile(
        gameDirectory: File,
        primaryFolder: File,
        configuredPaths: Collection<String>?,
        packName: String,
    ): File? = findLoadableShaderPackFile(gameDirectory, primaryFolder, configuredPaths, packName, true)

    @JvmStatic
    fun findLoadableShaderPackFile(
        gameDirectory: File,
        primaryFolder: File,
        configuredPaths: Collection<String>?,
        packName: String,
        allowNestedZipRoot: Boolean,
    ): File? {
        val paths = configuredPaths?.toList().orEmpty()
        val foldersKey = ResolutionKey(gameDirectory.absolutePath, primaryFolder.absolutePath, paths)
        val key = ShaderResolutionKey(foldersKey, packName, allowNestedZipRoot)
        shaderResolutionCache[key]?.takeIf { it.exists() }?.let { return it }

        val resolution = resolveFolders(gameDirectory, primaryFolder, paths)
        val primary = File(resolution.primary, packName)
        if (isShaderPack(primary, allowNestedZipRoot)) {
            return canonicalFile(primary).also { cacheShaderResolution(key, it) }
        }
        for (folder in resolution.additional) {
            val candidate = File(folder, packName)
            if (isShaderPack(candidate, allowNestedZipRoot)) {
                return canonicalFile(candidate).also { cacheShaderResolution(key, it) }
            }
        }
        return null
    }

    @JvmStatic
    fun resolveShaderPackPath(
        gameDirectory: File,
        primaryFolder: File,
        configuredPaths: Collection<String>?,
        packName: String,
        requestedName: String,
        allowNestedZipRoot: Boolean,
    ): File {
        val pack = findLoadableShaderPackFile(
            gameDirectory,
            primaryFolder,
            configuredPaths,
            packName,
            allowNestedZipRoot,
        )
        return File(pack?.parentFile ?: canonicalFile(primaryFolder), requestedName)
    }

    private fun isShaderPack(file: File, allowNestedZipRoot: Boolean): Boolean {
        if (file.isDirectory) {
            return File(file, "shaders").isDirectory
        }
        if (!file.isFile || !file.name.endsWith(".zip")) return false
        return try {
            ZipFile(file).use { zip ->
                zip.entries().asSequence().any { entry ->
                    val name = entry.name.replace('\\', '/').trimEnd('/')
                    name == "shaders" || name.startsWith("shaders/") ||
                        (allowNestedZipRoot && name.contains("/shaders/"))
                }
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun removeFormatting(value: String): String {
        val clean = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            if (value[index] == '\u00a7' && index + 1 < value.length) index += 2
            else clean.append(value[index++])
        }
        return clean.toString()
    }

    private fun cacheShaderResolution(key: ShaderResolutionKey, file: File) {
        if (shaderResolutionCache.size > 512) shaderResolutionCache.clear()
        shaderResolutionCache[key] = file
    }

    private fun canonicalFile(file: File): File = try {
        file.canonicalFile
    } catch (_: Throwable) {
        file.absoluteFile
    }
}
