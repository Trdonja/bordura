package repository;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;


public class Keys {
	
	public static void createKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyStore ks = KeyStore.getInstance("pcks12");
		ks.load(null, Configurations.keyStorePassword);
		try (FileOutputStream fos = new FileOutputStream(Configurations.keyStorePath + "keystore.pfx")) {
		    ks.store(fos, Configurations.keyStorePassword);
		}
	}
	
	public static void createNewKeyPair() throws NoSuchAlgorithmException {
		// Edwards-Curve signature algorithm with elliptic curves as defined in RFC 8032.
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(255);
		KeyPair keyPair = generator.generateKeyPair();
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();
		// TODO: Save keys in key store
	}
	
	public static byte[] sign(byte[] input, PrivateKey privateKey)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature signature = Signature.getInstance("SHA3-256withECDSA");
		signature.initSign(privateKey);
		signature.update(input);
		return signature.sign();
	}
	
	public static boolean verifySignature(byte[] input, byte[] receivedSignature, PublicKey publicKey)
			throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		Signature signature = Signature.getInstance("SHA3-256withECDSA");
		signature.initVerify(publicKey);
		signature.update(input);
		return signature.verify(receivedSignature);
	}

}
