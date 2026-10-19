package industries.angelic.vintageresourcify.gui.pack

import industries.angelic.vintageresourcify.util.LocalIndex
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class LocalIndexRenameTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun renameKeepsEveryUpdatePreferenceAndIdentity() {
        val root = temporary.newFolder("packs")
        val folder = File(root, "Folder").apply { mkdir() }
        val old = File(folder, "old.zip").apply { writeText("fixture") }
        File(root, ".resourcify-index.json").writeText("""{"entries":[{
            "fileName":"Folder/old.zip", "sha1":"fixture-hash", "platform":"modrinth",
            "projectId":"project", "installedVersion":"v1", "minecraftVersions":["1.7.10"],
            "updateTrack":"1.7.10", "installedReleaseDate":"2025-01-01", "ignoredVersion":"v2",
            "ignoredUpdates":[], "remoteFileId":"remote-file"
        }]}""")
        val index = LocalIndex.forFolder(root)
        val before = index.lookupByFileName(old)!!
        val next = File(folder, "renamed.zip")
        Files.move(old.toPath(), next.toPath())
        index.rename(old, next)
        assertNull(index.lookupByFileName(old))
        assertEquals(before.copy(fileName = "Folder/renamed.zip"), index.lookupByFileName(next))
        val saved = File(root, ".resourcify-index.json").readText()
        assertTrue(saved.contains("Folder/renamed.zip"))
        assertFalse(saved.contains("Folder/old.zip"))
        assertEquals("v2", index.lookupByFileName(next)?.ignoredVersion)
    }
}
