package repository;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;

public class STSKeyAgreement {

	private static final int DH_KEY_LENGTH = 2048;

	public static void initiateKeyAgreement() throws NoSuchAlgorithmException, InvalidKeyException,
	InvalidKeySpecException, SignatureException {

		/******** STAGE 1 ********/

		/* Create own key-pair for Diffie-Hellman key agreement protocol */
		KeyPairGenerator dhKeyPairGenerator = KeyPairGenerator.getInstance("DH");
		dhKeyPairGenerator.initialize(DH_KEY_LENGTH);
		KeyPair ownDHKeyPair = dhKeyPairGenerator.generateKeyPair();

		/* Create and initialize own KeyAgreement object with generated private key. */
		KeyAgreement dhKeyAgreement = KeyAgreement.getInstance("DH");
		dhKeyAgreement.init(ownDHKeyPair.getPrivate());

		/* Encode own public key and send it over to peer, together with own ID. */
		byte[] ownDHPublicKeyEncoded = ownDHKeyPair.getPublic().getEncoded();
		byte[] ownID = new byte[16]; // FIXME: get it from database
		// TODO: Send over the wire to peer: [ownID, ownDHPublicKeyEncoded]

		/******** STAGE 3 ********/

		byte[] peerID = new byte[16]; // FIXME: Receive it from peer over the wire
		// TODO: Verify peer's ID and obtain its public signature key for signature verification
		PublicKey peerSigVerifyKey = null; // FIXME: obtain it from own database - obtainSigVerifyKey(peerID);

		/* Obtain peer's public key for Diffie-Hellman key agreement in encoded format and
		 * instantiate DH public key from encoded format. */
		byte[] peerDHPublicKeyEncoded = new byte[2048]; // FIXME: Receive it from peer over the wire
		X509EncodedKeySpec peerDHPublicKeySpec = new X509EncodedKeySpec(peerDHPublicKeyEncoded);
		PublicKey peerDHPublicKey = KeyFactory.getInstance("DH").generatePublic(peerDHPublicKeySpec);

		/* Receive peer's signature over the wire and verify it using his public signature verification key. */
		Signature signatureAlgorithm = Signature.getInstance("EdDSA");
		signatureAlgorithm.initVerify(peerSigVerifyKey);
		signatureAlgorithm.update(ownID);
		signatureAlgorithm.update(peerDHPublicKeyEncoded);
		signatureAlgorithm.update(ownDHPublicKeyEncoded);
		byte[] peerSignature = new byte[128]; // FIXME: Receive it from peer over the wire
		boolean validSignature = signatureAlgorithm.verify(peerSignature);
		if (!validSignature) {
			// TODO: Must fail. Notify peer over the wire
			//       and then throw some STSKeyAgreementFailiureException or something.
		}

		/* Sign concatenation of peer's ID, own DH public key and peer's DH public key. */
		PrivateKey ownSignatureKey = null; // FIXME: get it from database or keystore
		signatureAlgorithm.initSign(ownSignatureKey);
		signatureAlgorithm.update(peerID);
		signatureAlgorithm.update(ownDHPublicKeyEncoded);
		signatureAlgorithm.update(peerDHPublicKeyEncoded);
		byte[] ownSignature = signatureAlgorithm.sign();

		// TODO: Send own signature over the wire to peer.

		/* Use peer's public key for the first (and only) phase on own version of
		 * Diffie-Hellman key-agreement instance and obtain shared secret. */
		dhKeyAgreement.doPhase(peerDHPublicKey, true);
		byte[] sharedSecret = dhKeyAgreement.generateSecret();

	}

	public static void replyKeyAgreement() throws InvalidKeySpecException, NoSuchAlgorithmException,
	InvalidAlgorithmParameterException, InvalidKeyException, SignatureException {

		/******** STAGE 2 ********/

		byte[] peerID = new byte[16]; // FIXME: Receive it from peer over the wire
		// TODO: Verify peer's ID and obtain its public signature key for signature verification
		PublicKey peerSigVerifyKey = null; // FIXME: obtain it from own database - obtainSigVerifyKey(peerID);

		/* Obtain peer's public key for Diffie-Hellman key agreement in encoded format and
		 * instantiate DH public key from encoded format. */
		byte[] peerDHPublicKeyEncoded = new byte[2048]; // FIXME: Receive it from peer over the wire
		X509EncodedKeySpec peerDHPublicKeySpec = new X509EncodedKeySpec(peerDHPublicKeyEncoded);
		PublicKey peerDHPublicKey = KeyFactory.getInstance("DH").generatePublic(peerDHPublicKeySpec);

		/* Create own key-pair for Diffie-Hellman key agreement protocol,
		 * using the same public domain parameters as peer. */
		DHParameterSpec dhParamFromPeerPublicKey = ((DHPublicKey)peerDHPublicKey).getParams();
		if (dhParamFromPeerPublicKey.getL() != DH_KEY_LENGTH) {
			// TODO: Fail - security issue: Received key is not of appropriate length.
			//       Throw some STSKeyAgreementFailiureException or something.
		}
		KeyPairGenerator dhKeyPairGenerator = KeyPairGenerator.getInstance("DH");
		dhKeyPairGenerator.initialize(dhParamFromPeerPublicKey);
		KeyPair ownDHKeyPair = dhKeyPairGenerator.generateKeyPair();

		/* Create and initialize own KeyAgreement object with generated private key. */
		KeyAgreement dhKeyAgreement = KeyAgreement.getInstance("DH");
		dhKeyAgreement.init(ownDHKeyPair.getPrivate());

		/* Encode own public key. */
		byte[] ownDHPublicKeyEncoded = ownDHKeyPair.getPublic().getEncoded();

		/* Obtain own ID. */
		byte[] ownID = new byte[16]; // FIXME: get it from database

		/* Sign concatenation of peer's ID, own DH public key and peer's DH public key. */
		Signature signatureAlgorithm = Signature.getInstance("EdDSA");
		PrivateKey ownSignatureKey = null; // FIXME: get it from database or keystore
		signatureAlgorithm.initSign(ownSignatureKey);
		signatureAlgorithm.update(peerID);
		signatureAlgorithm.update(ownDHPublicKeyEncoded);
		signatureAlgorithm.update(peerDHPublicKeyEncoded);
		byte[] ownSignature = signatureAlgorithm.sign();

		// TODO: Send over wire to peer: [ownID, ownDHPublicKeyEncoded, ownSignature]

		/* Use peer's public key for the first (and only) phase on own version of
		 * Diffie-Hellman key-agreement instance and obtain shared secret. */
		dhKeyAgreement.doPhase(peerDHPublicKey, true);
		byte[] sharedSecret = dhKeyAgreement.generateSecret();

		/******** STAGE 4 ********/

		/* Receive peer's signature over the wire and verify it, using his public signature verification key. */
		signatureAlgorithm.initVerify(peerSigVerifyKey);
		signatureAlgorithm.update(ownID);
		signatureAlgorithm.update(peerDHPublicKeyEncoded);
		signatureAlgorithm.update(ownDHPublicKeyEncoded);
		byte[] peerSignature = new byte[128]; // FIXME: Receive it from peer over the wire
		boolean validSignature = signatureAlgorithm.verify(peerSignature);
		if (!validSignature) {
			// TODO: Must fail. Notify peer over the wire
			//       and then throw some STSKeyAgreementFailiureException or something.
		}
	}

}
