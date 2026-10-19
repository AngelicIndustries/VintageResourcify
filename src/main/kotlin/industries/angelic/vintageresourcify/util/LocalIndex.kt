/*
 * This file is part of Resourcify
 * Copyright (C) 2024 DeDiamondPro
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package industries.angelic.vintageresourcify.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import industries.angelic.vintageresourcify.VintageResourcify
import industries.angelic.vintageresourcify.services.IVersion
import industries.angelic.vintageresourcify.api.IdentifiedVersion
import java.io.File

/**
 * Per-folder install registry persisted to `.resourcify-index.json`.
 *
 * Tracks which platform + project a managed pack file came from so we don't
 * have to re-derive that information by hashing/searching every time the user
 * opens the resource-pack screen. Files placed manually by the user are not
 * tracked and are deliberately ignored by the update flow.
 *
 * Entries are matched first by [fileName] (the usual case), then by [sha1] as
 * a fallback when the file has been renamed by the user.
 */
class LocalIndex private constructor(private val folder: File) {

    data class IgnoredUpdate(
        val signature: String,
        val name: String,
        val versionNumber: String?,
        val fileName: String,
        val minecraftVersions: List<String>,
        val releaseDate: String,
    )

    data class Entry(
        val fileName: String,
        val sha1: String?,
        val platform: String,
        val projectId: String,
        /** Stable description of the installed remote file, when known. */
        val installedVersion: String? = null,
        /** Compatibility claimed by the installed file at download time. */
        val minecraftVersions: List<String>? = null,
        /** Minecraft branch to follow; null means the latest release on any branch. */
        val updateTrack: String? = null,
        val installedReleaseDate: String? = null,
        /** Candidate-specific ignore token. A different future release is visible again. */
        val ignoredVersion: String? = null,
        val ignoredUpdates: List<IgnoredUpdate>? = null,
        val remoteFileId: String? = null,
    )

    private data class IndexFile(val entries: MutableList<Entry> = mutableListOf())

    private val file: File = File(folder, INDEX_FILENAME)
    private var data: IndexFile = load()

    /**
     * Fast lookup for render paths. This must remain free of filesystem I/O:
     * pack and shader rows call it every frame.
     */
    fun lookupByFileName(target: File): Entry? {
        val key = keyFor(target)
        return data.entries.firstOrNull { it.fileName == key }
            // Compatibility with indexes written before nested Folder Packs.
            ?: data.entries.firstOrNull { it.fileName == target.name }
    }

    /**
     * Resolve an entry by filename, falling back to hashing a renamed file.
     * The fallback reads the whole file and must not be called from rendering.
     */
    fun lookupByFile(target: File): Entry? {
        lookupByFileName(target)?.let { return it }
        // Fallback: file may have been renamed manually. Hash-match if we
        // have a recorded sha1. Guard against a non-existent file: vanilla
        // can still hold a list entry for a file that's been deleted
        // out-of-band (e.g. active pack just deleted by the user), and
        // calling getSha1 on it would produce a FileNotFoundException.
        if (!target.exists() || !target.isFile || data.entries.none { it.sha1 != null }) return null
        val sha = Utils.getSha1(target) ?: return null
        return data.entries.firstOrNull { it.sha1 == sha }
    }

    @Synchronized
    fun listEntries(): List<Entry> = data.entries.toList()

    /** A UI rename keeps every update preference and remote identity unchanged. */
    @Synchronized
    fun rename(old: File, new: File) {
        val entry = lookupByFileName(old) ?: return
        val index = data.entries.indexOf(entry)
        data.entries[index] = entry.copy(fileName = keyFor(new))
        save()
    }

    /** Insert or replace the entry for [file]. Persists synchronously. */
    @Synchronized
    fun record(
        file: File,
        platform: String,
        projectId: String,
        version: IVersion? = null,
        updateTrack: String? = null,
        previous: Entry? = null,
    ) {
        val sha = Utils.getSha1(file)
        val key = keyFor(file)
        data.entries.removeAll { it.fileName == key || (key != file.name && it.fileName == file.name) }
        data.entries.add(
            Entry(
                fileName = key,
                sha1 = sha,
                platform = platform,
                projectId = projectId,
                installedVersion = version?.let { versionSignature(platform, it) } ?: previous?.installedVersion,
                minecraftVersions = version?.getMinecraftVersions() ?: previous?.minecraftVersions,
                updateTrack = updateTrack ?: previous?.updateTrack,
                installedReleaseDate = version?.getReleaseDate() ?: previous?.installedReleaseDate,
                ignoredVersion = null,
                ignoredUpdates = previous?.ignoredUpdates,
                remoteFileId = (version as? IdentifiedVersion)?.getFileId()?.takeIf { it.isNotBlank() }
                    ?: if (version == null) previous?.remoteFileId else null,
            )
        )
        save()
    }

    @Synchronized
    fun ignoreVersion(fileName: String, platform: String, version: IVersion) {
        val ignored = IgnoredUpdate(
            signature = versionSignature(platform, version),
            name = version.getName(),
            versionNumber = version.getVersionNumber(),
            fileName = version.getFileName(),
            minecraftVersions = version.getMinecraftVersions(),
            releaseDate = version.getReleaseDate(),
        )
        replace(fileName) { entry ->
            entry.copy(
                ignoredVersion = null,
                ignoredUpdates = (entry.ignoredUpdates.orEmpty().filterNot { it.signature == ignored.signature } + ignored),
            )
        }
    }

    fun ignoreVersion(file: File, platform: String, version: IVersion) =
        ignoreVersion(keyFor(file), platform, version)

    @Synchronized
    fun restoreVersion(fileName: String, signature: String) {
        replace(fileName) { entry ->
            entry.copy(
                ignoredVersion = entry.ignoredVersion.takeUnless { it == signature },
                ignoredUpdates = entry.ignoredUpdates.orEmpty().filterNot { it.signature == signature },
            )
        }
    }

    fun restoreVersion(file: File, signature: String) = restoreVersion(keyFor(file), signature)

    @Synchronized
    fun setUpdateTrack(fileName: String, track: String?) {
        replace(fileName) { it.copy(updateTrack = track) }
    }

    fun setUpdateTrack(file: File, track: String?) = setUpdateTrack(keyFor(file), track)

    private fun replace(fileName: String, transform: (Entry) -> Entry) {
        val index = data.entries.indexOfFirst { it.fileName == fileName }
        if (index < 0) return
        data.entries[index] = transform(data.entries[index])
        save()
    }

    @Synchronized
    fun remove(fileName: String) {
        val changed = data.entries.removeAll { it.fileName == fileName }
        if (changed) save()
    }

    @Synchronized
    fun remove(target: File) {
        val key = keyFor(target)
        val changed = data.entries.removeAll { it.fileName == key || (key != target.name && it.fileName == target.name) }
        if (changed) save()
    }

    /**
     * Drop entries whose file no longer exists. Returns the number of stale
     * entries removed. Called when the user opens a pack screen.
     */
    @Synchronized
    fun prune(): Int {
        val before = data.entries.size
        data.entries.removeAll { !File(folder, it.fileName).exists() }
        val removed = before - data.entries.size
        if (removed > 0) save()
        return removed
    }

    /** Migrates legacy filename-only records after a managed pack is moved into a Folder Pack. */
    @Synchronized
    fun reconcileFiles(candidates: Collection<File>) {
        val missing = data.entries.filter { !File(folder, it.fileName).exists() && it.sha1 != null }
        if (missing.isEmpty()) return
        val candidateHashes = candidates.asSequence()
            .filter { it.isFile }
            .mapNotNull { candidate -> Utils.getSha1(candidate)?.let { it to candidate } }
            .toMap()
        var changed = false
        missing.forEach { old ->
            val match = candidateHashes[old.sha1] ?: return@forEach
            val index = data.entries.indexOf(old)
            if (index >= 0) {
                data.entries[index] = old.copy(fileName = keyFor(match))
                changed = true
            }
        }
        if (changed) save()
    }

    private fun keyFor(target: File): String = try {
        val root = folder.canonicalFile.toPath()
        val path = target.canonicalFile.toPath()
        if (path.startsWith(root)) root.relativize(path).toString().replace(File.separatorChar, '/') else target.name
    } catch (_: Throwable) {
        target.name
    }

    private fun load(): IndexFile {
        if (!file.exists()) return IndexFile()
        return try {
            file.bufferedReader().use { GSON.fromJson(it, IndexFile::class.java) } ?: IndexFile()
        } catch (e: Throwable) {
            VintageResourcify.LOG.warn("Could not read {}; starting fresh", file, e)
            IndexFile()
        }
    }

    private fun save() {
        try {
            if (!folder.exists()) folder.mkdirs()
            file.bufferedWriter().use { GSON.toJson(data, it) }
        } catch (e: Throwable) {
            VintageResourcify.LOG.warn("Could not write {}", file, e)
        }
    }

    companion object {
        const val INDEX_FILENAME = ".resourcify-index.json"
        private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
        private val cache = mutableMapOf<File, LocalIndex>()

        @Synchronized
        fun forFolder(folder: File): LocalIndex =
            cache.getOrPut(folder.canonicalFile) { LocalIndex(folder.canonicalFile) }

        /** Human-readable version retained inside the stable install signature. */
        fun installedVersionLabel(entry: Entry): String? {
            val parts = entry.installedVersion?.split('\u001F') ?: return null
            if (parts.size < 6) return null
            return parts[4].takeIf { it.isNotBlank() }
                ?: parts[5].takeIf { it.isNotBlank() }
                ?: parts[2].takeIf { it.isNotBlank() }
        }

        fun versionSignature(platform: String, version: IVersion): String {
            val fileId = (version as? IdentifiedVersion)?.getFileId()?.takeIf { it.isNotBlank() }
            if (fileId != null) return listOf(platform, version.getProjectId(), "file-id", fileId).joinToString("\u001F")
            return listOf(
                platform,
                version.getProjectId(),
                version.getFileName(),
                version.getSha1(),
                version.getVersionNumber().orEmpty(),
                version.getName(),
            ).joinToString("\u001F")
        }
    }
}
