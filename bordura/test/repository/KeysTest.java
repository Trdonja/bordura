package repository;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class KeysTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		Initialization.createNewDatabase();
		try {
		Initialization.createTables();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			throw e;
		}
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}
	
	@Test
	void generationOfKeys() throws SQLException, NoSuchAlgorithmException, IOException, ClassNotFoundException,
	InvalidKeyException, NoSuchProviderException, NoSuchPaddingException, IllegalBlockSizeException,
	BadPaddingException, SignatureException {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
		try (Connection dbcon = DriverManager.getConnection(Configurations.dbUrl)) {
			byte[] guid = Keys.generateNewKeyPair(dbcon, LocalDate.now().plusMonths(15));
			PublicKey publicKey = Keys.getPublicKey(dbcon, guid).get();
			PrivateKey privateKey = Keys.getPrivateKey(guid);
			
			byte[] messageBytes = "Od nekdaj lepe so ljubljanke slovele, a lepše od Urške bilo ni nobene.".getBytes(UTF_8);
			
			// If calculating hash and (en/de)-crypting manually using BouncyCastle:
			/*
			 * byte[] messageHash = Posts.calculateHashOf(messageBytes);
			 * byte[] signature = Posts.encrypt(messageHash, privateKey);
			 * assertTrue(Posts.verifySignature(messageHash, signature, publicKey));
			 */
			
			// Otherwise:
			byte[] signature = Posts.sign(messageBytes, privateKey);
			assertTrue(Posts.verifySignatureDirect(messageBytes, signature, publicKey));
		}
	}

	/*
	 * @Test void test() throws NoSuchAlgorithmException, InvalidKeyException,
	 * SignatureException { KeyPairGenerator generator =
	 * KeyPairGenerator.getInstance("EC"); generator.initialize(256); KeyPair
	 * keyPair = Keys.createNewKeyPair(); PrivateKey privateKey =
	 * keyPair.getPrivate(); PublicKey publicKey = keyPair.getPublic();
	 * 
	 * byte[] input =
	 * "Od nekdaj lepe so ljubljanke slovele, a lepše od Urške bilo ni nobene."
	 * .getBytes(UTF_8);
	 * 
	 * Signature signatureSign = Signature.getInstance("SHA3-256withECDSA");
	 * signatureSign.initSign(privateKey); signatureSign.update(input); var
	 * receivedSignature = signatureSign.sign();
	 * 
	 * Signature signatureVer = Signature.getInstance("SHA3-256withECDSA");
	 * signatureVer.initVerify(publicKey); signatureVer.update(input);
	 * assertTrue(signatureVer.verify(receivedSignature)); }
	 */

}
