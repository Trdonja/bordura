package repository;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class KeysTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@Test
	void test() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(256);
		KeyPair keyPair = generator.generateKeyPair();
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();
		
		byte[] input = "Od nekdaj lepe so ljubljanke slovele, a lepše od Urške bilo ni nobene.".getBytes(UTF_8);
		
		Signature signatureSign = Signature.getInstance("SHA3-256withECDSA");
		signatureSign.initSign(privateKey);
		signatureSign.update(input);
		var receivedSignature = signatureSign.sign();

		Signature signatureVer = Signature.getInstance("SHA3-256withECDSA");
		signatureVer.initVerify(publicKey);
		signatureVer.update(input);
		assertTrue(signatureVer.verify(receivedSignature));
	}

}
