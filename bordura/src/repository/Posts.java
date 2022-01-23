package repository;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Posts {
	
	public static byte[] sign(byte[] input, PrivateKey privateKey)
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		Signature signature = Signature.getInstance("SHA3-256withECDSA");
		signature.initSign(privateKey);
		signature.update(input);
		return signature.sign();
	}
	
	public static boolean verifySignatureDirect(byte[] input, byte[] receivedSignature, PublicKey publicKey)
			throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
		Signature signature = Signature.getInstance("SHA3-256withECDSA");
		signature.initVerify(publicKey);
		signature.update(input);
		return signature.verify(receivedSignature);
	}
	
	public static byte[] calculateHashOf(byte[] messageBytes) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA3-256");
		return md.digest(messageBytes);
	}
	
	public static byte[] encrypt(byte[] messageHash, PrivateKey privateKey)
			throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		// WARNING: exception gets thrown here, because somehow it wants public key for encryption:
		Cipher cipher = Cipher.getInstance("ECIES", BouncyCastleProvider.PROVIDER_NAME);
		cipher.init(Cipher.ENCRYPT_MODE, privateKey);
		return cipher.doFinal(messageHash);
	}

	public static boolean verifySignature(byte[] messageHash, byte[] encryptedMessageHash, PublicKey publicKey)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
			IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("ECIES", BouncyCastleProvider.PROVIDER_NAME);
		cipher.init(Cipher.DECRYPT_MODE, publicKey);
		byte[] decryptedMessageHash = cipher.doFinal(encryptedMessageHash);
		return Arrays.equals(messageHash, decryptedMessageHash);
	}

}
