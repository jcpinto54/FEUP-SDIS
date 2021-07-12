package application;

public interface Application {
    static void setKeys(String fileName) {
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        //set the password with which the truststore is encripted
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
        //set the name of the trust store containing the client public key and certificate
        System.setProperty("javax.net.ssl.trustStore", "./resources/keys/trust.jks");
        //set the password with which the server keystore is encripted
        System.setProperty("javax.net.ssl.keyStorePassword", "123456");
        //set the name of the keystore containing the server's private and public keys
        System.setProperty("javax.net.ssl.keyStore", "./resources/keys/" + fileName + ".jks");
    }
}
