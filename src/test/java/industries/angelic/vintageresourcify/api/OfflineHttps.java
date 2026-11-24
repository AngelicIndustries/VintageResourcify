package industries.angelic.vintageresourcify.api;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HttpsURLConnection;

/**
 * The URL handler factory can only be set once per JVM, so provider tests share this one and route their own hosts.
 * Requests to any other host fail. DownloadManagerTest supplies its own URL handler and doesn't use this.
 */
final class OfflineHttps {

    interface Responder {

        String respond(URL url, String body, URLConnection connection);
    }

    private static final Map<String, Responder> ROUTES = new ConcurrentHashMap<>();
    private static boolean installed;

    private OfflineHttps() {}

    static synchronized void route(String host, Responder responder) {
        if (!installed) {
            URL.setURLStreamHandlerFactory(protocol -> "https".equals(protocol) ? new Handler() : null);
            installed = true;
        }
        ROUTES.put(host, responder);
    }

    private static final class Handler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(URL url) {
            Responder responder = ROUTES.get(url.getHost());
            assertNotNull("Unexpected host: " + url, responder);
            ByteArrayOutputStream body = new ByteArrayOutputStream();
            return new HttpsURLConnection(url) {

                @Override
                public InputStream getInputStream() {
                    String response = responder
                        .respond(url, new String(body.toByteArray(), StandardCharsets.UTF_8), this);
                    return new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8));
                }

                @Override
                public OutputStream getOutputStream() {
                    return body;
                }

                @Override
                public String getCipherSuite() {
                    return "test";
                }

                @Override
                public Certificate[] getLocalCertificates() {
                    return new Certificate[0];
                }

                @Override
                public Certificate[] getServerCertificates() {
                    return new Certificate[0];
                }

                @Override
                public void connect() {}

                @Override
                public void disconnect() {}

                @Override
                public boolean usingProxy() {
                    return false;
                }
            };
        }
    }
}
