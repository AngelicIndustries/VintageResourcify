package industries.angelic.vintageresourcify.api;

import static org.junit.Assert.*;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import industries.angelic.vintageresourcify.services.DistributionPolicy;
import industries.angelic.vintageresourcify.services.IProject;
import industries.angelic.vintageresourcify.services.IService;
import industries.angelic.vintageresourcify.services.IVersion;
import industries.angelic.vintageresourcify.services.ProjectType;
import industries.angelic.vintageresourcify.services.ServiceRegistry;
import industries.angelic.vintageresourcify.services.curseforge.CurseForgeApiService;
import industries.angelic.vintageresourcify.util.MurmurHash2;

public class CurseForgeProviderTest {

    private static final String MIRROR_HOST = "127.0.0.4";
    private static final String KEYED_HOST = "127.0.0.5";
    private static final byte[] INSTALLED = "abc".getBytes(StandardCharsets.UTF_8);
    private static final long INSTALLED_FINGERPRINT = MurmurHash2.cfHash(INSTALLED, INSTALLED.length);
    private static final String PROJECT = project(1, "Pack");
    private static final String DEPENDENCY = project(5, "Dependency");
    private static final Set<String> SENT_KEYS = ConcurrentHashMap.newKeySet();

    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private static String project(int id, String name) {
        return "{\"id\":" + id
            + ",\"name\":\""
            + name
            + "\",\"summary\":\""
            + name
            + " summary\",\"authors\":[{\"name\":\"author\",\"url\":\"https://www.curseforge.com/members/author\"}],"
            + "\"screenshots\":[],\"categories\":[],\"allowModDistribution\":true,"
            + "\"links\":{\"websiteUrl\":\"https://www.curseforge.com/minecraft/texture-packs/pack\"}}";
    }

    private static String file(int id, long fingerprint, String date, String dependencies) {
        return "{\"id\":" + id
            + ",\"modId\":1,\"displayName\":\"Pack "
            + id
            + "\",\"fileName\":\"pack-"
            + id
            + ".zip\",\"downloadUrl\":\"https://edge.forgecdn.net/files/0/"
            + id
            + "/pack.zip\",\"hashes\":[{\"value\":\"0000000000000000000000000000000000000000\",\"algo\":1}],"
            + "\"releaseType\":1,\"gameVersions\":[\"1.7.10\"],\"downloadCount\":0,\"fileDate\":\""
            + date
            + "\",\"dependencies\":"
            + dependencies
            + ",\"fileFingerprint\":"
            + fingerprint
            + "}";
    }

    @BeforeClass
    public static void useOfflineResponses() {
        OfflineHttps.route(MIRROR_HOST, (url, body, connection) -> response(url, body, connection, "/curseforge/v1"));
        OfflineHttps.route(KEYED_HOST, (url, body, connection) -> response(url, body, connection, "/keyed/v1"));
    }

    private static String response(URL url, String body, URLConnection connection, String base) {
        String key = connection.getRequestProperty("x-api-key");
        if (key != null) SENT_KEYS.add(url.getHost() + "=" + key);
        String path = url.getPath();
        assertTrue("Unexpected endpoint: " + url, path.startsWith(base + "/"));
        switch (path.substring(base.length())) {
            case "/mods/search":
                return "{\"data\":[" + PROJECT + "],\"pagination\":{\"totalCount\":1}}";
            case "/mods/1/description":
                return "{\"data\":\"Mirror description\"}";
            case "/mods/1/files":
                return "{\"data\":[" + file(2, 2, "2026-09-24T00:00:00Z", "[{\"modId\":5,\"relationType\":3}]") + "]}";
            case "/mods/1/files/2/changelog":
                return "{\"data\":\"Mirror changelog\"}";
            case "/mods":
                return "{\"data\":[" + (body.contains("5") ? DEPENDENCY : PROJECT) + "]}";
            case "/fingerprints/432":
                return "{\"data\":{\"exactMatches\":[{\"id\":1,\"file\":"
                    + file(1, INSTALLED_FINGERPRINT, "2026-01-01T00:00:00Z", "[]")
                    + ",\"latestFiles\":["
                    + file(2, 2, "2026-09-24T00:00:00Z", "[{\"modId\":5,\"relationType\":3}]")
                    + "]}]}}";
            case "/minecraft/version":
                return "{\"data\":[{\"versionString\":\"1.7.10\"}]}";
            default:
                throw new AssertionError("Unexpected endpoint: " + url);
        }
    }

    private static IProject search(IService service) {
        return service
            .search(
                "pack",
                "2",
                Collections.singletonList("1.7.10"),
                Collections.emptyList(),
                0,
                ProjectType.RESOURCE_PACK)
            .getProjects()
            .get(0);
    }

    @Test
    public void registeredMirrorKeepsRequestsOnItsPlatformWithoutAKey() throws Exception {
        assertEquals(
            RegistrationResult.REGISTERED,
            ProviderApi.registerCurseForge(
                "curseforge_mirror_test",
                " Mirror ",
                " https://" + MIRROR_HOST + "/curseforge/v1/ "));
        IService service = ProviderApi.getProvider("curseforge_mirror_test");
        assertEquals("Mirror", service.getName());
        assertTrue(DistributionPolicy.canDownloadFrom("curseforge_mirror_test"));
        assertTrue(ServiceRegistry.INSTANCE.isAddonProvider("curseforge_mirror_test"));

        assertTrue(
            service.getMinecraftVersions()
                .get(5, TimeUnit.SECONDS)
                .containsKey("1.7.10"));

        IProject project = search(service);
        assertEquals(
            "Mirror description",
            project.getDescription()
                .get(5, TimeUnit.SECONDS));
        IVersion version = project.getVersions()
            .get(5, TimeUnit.SECONDS)
            .get(0);
        assertEquals(
            "Mirror changelog",
            version.getChangeLog()
                .get(5, TimeUnit.SECONDS));
        IProject dependency = version.getDependencies()
            .get(5, TimeUnit.SECONDS)
            .get(0)
            .getProject();
        assertEquals("Dependency", dependency.getName());
        assertEquals(
            "Mirror description",
            service.getProjectsFromIds(Collections.singletonList("1"))
                .get("1")
                .getDescription()
                .get(5, TimeUnit.SECONDS));

        File installed = temporary.newFile("pack.zip");
        Files.write(installed.toPath(), INSTALLED);
        IVersion update = service.getUpdates(Collections.singletonList(installed), ProjectType.RESOURCE_PACK, "1.7.10")
            .get(5, TimeUnit.SECONDS)
            .get(installed);
        assertEquals("Pack 2", update.getName());
        assertEquals(
            "Mirror changelog",
            update.getChangeLog()
                .get(5, TimeUnit.SECONDS));

        assertFalse(
            SENT_KEYS.stream()
                .anyMatch(sent -> sent.startsWith(MIRROR_HOST + "=")));
        assertEquals(
            RegistrationResult.DUPLICATE_ID,
            ProviderApi.registerCurseForge("curseforge_mirror_test", "Duplicate", "https://" + MIRROR_HOST + "/v1"));
    }

    @Test
    public void subclassesReplaceMissingEndpointsAndAddHeaders() throws Exception {
        IService service = new CurseForgeApiService(
            "Keyed",
            "https://" + KEYED_HOST + "/keyed/v1",
            "keyed_mirror_test") {

            @Override
            public Map<String, String> requestHeaders() {
                return Collections.singletonMap("x-api-key", "mirror-key");
            }

            @Override
            public List<String> fetchMinecraftVersions() {
                return Collections.singletonList("1.6.4");
            }

            @Override
            public String fetchDescription(IProject project) {
                return project.getSummary();
            }

            @Override
            public String fetchChangelog(String projectId, String fileId) {
                return "";
            }
        };
        assertEquals(RegistrationResult.REGISTERED, ProviderApi.register(service));
        assertEquals(
            Collections.singleton("1.6.4"),
            service.getMinecraftVersions()
                .get(5, TimeUnit.SECONDS)
                .keySet());

        IProject project = search(service);
        assertEquals(
            "Pack summary",
            project.getDescription()
                .get(5, TimeUnit.SECONDS));
        assertEquals(
            "",
            project.getVersions()
                .get(5, TimeUnit.SECONDS)
                .get(0)
                .getChangeLog()
                .get(5, TimeUnit.SECONDS));
        assertTrue(SENT_KEYS.contains(KEYED_HOST + "=mirror-key"));
    }

    @Test
    public void rejectsMalformedEndpointsBeforeRegistering() {
        for (String invalid : new String[] { "http://example.org/v1", "https://user:pass@example.org/v1",
            "https://example.org/v1?key=secret", "not a URL" }) {
            assertThrows(
                IllegalArgumentException.class,
                () -> ProviderApi.registerCurseForge("bad_curseforge", "Example", invalid));
        }
        assertNull(ProviderApi.getProvider("bad_curseforge"));
        assertThrows(
            IllegalArgumentException.class,
            () -> ProviderApi.registerCurseForge("bad_curseforge", " ", "https://example.org/v1"));
        assertEquals(
            RegistrationResult.RESERVED_ID,
            ProviderApi.registerCurseForge("curseforge", "Example", "https://example.org/v1"));
    }
}
