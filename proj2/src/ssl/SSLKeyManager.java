package ssl;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

public class SSLKeyManager {
    private final static String PROTOCOL = "TLS";
    private final static String RESOURCES_FOLDER = "../resources/keys";
    private final static String RESOURCES_PASSWORD = "123456";

    public static SSLContext createSecureContext(String keyFile, String trustFile) throws UnrecoverableKeyException, CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        KeyManager[] keyManagers = createKeyManagers(getPath(keyFile), RESOURCES_PASSWORD, RESOURCES_PASSWORD);
        TrustManager[] trustManagers = createTrustManagers(getPath(trustFile), RESOURCES_PASSWORD);
        SSLContext context = SSLContext.getInstance(PROTOCOL);
        context.init(keyManagers, trustManagers, new SecureRandom());
        return context;
    }

    private static KeyManager[] createKeyManagers(String filePath, String keyPassword, String keyStorePassword) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore store = getKeyStore(filePath, keyStorePassword);
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store, keyPassword.toCharArray());
        return factory.getKeyManagers();
    }

    private static TrustManager[] createTrustManagers(String filePath, String keyStorePassword) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
        KeyStore store = getKeyStore(filePath, keyStorePassword);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        return factory.getTrustManagers();
    }

    private static KeyStore getKeyStore(String filePath, String keyStorePassword) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore store = KeyStore.getInstance("JKS");
        try (InputStream stream = new FileInputStream(filePath)) {
            store.load(stream, keyStorePassword.toCharArray());
        }
        return store;
    }

    private static String getPath(String fileName) {
        return RESOURCES_FOLDER + "/" + fileName + ".jks";
    }
}
