package repository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyRep;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.bouncycastle.jce.provider.BouncyCastleProvider;


public class Keys {
	
	public static byte[] generateNewKeyPair(Connection dbcon, LocalDate validityDate)
			throws NoSuchAlgorithmException, SQLException, IOException, NoSuchProviderException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC"); // to add BC provider, add: , BouncyCastleProvider.PROVIDER_NAME);
		generator.initialize(256);
		KeyPair keyPair = generator.generateKeyPair();
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();
		byte[] guid = generateGUID();
		savePublicKey(dbcon, guid, publicKey, 1, validityDate);
		savePrivateKey(guid, privateKey);
		return guid;
	}
	
	private static byte[] generateGUID() {
		UUID guid = UUID.randomUUID();
		byte[] bytes = new byte[16];
		long l = guid.getMostSignificantBits();
		for (int i = 7; i >= 0; i--) {
	        bytes[i] = (byte)(l & 0xFF);
	        l >>= 8;
	    }
		l = guid.getLeastSignificantBits();
		for (int i = 15; i >= 8; i--) {
	        bytes[i] = (byte)(l & 0xFF);
	        l >>= 8;
	    }
		return bytes;
	}
	
	private static byte[] keyRepBytes(Key key, KeyRep.Type keyType) {
		KeyRep keyRep = new KeyRep(keyType, key.getAlgorithm(), key.getFormat(), key.getEncoded());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(keyRep);
			oos.flush();
		} catch (IOException e) {
			// TODO: Log it; but this should never happen, you are writing to an array.
		}
		return baos.toByteArray();
	}

	public static int savePublicKey(Connection dbcon, byte[] guid, PublicKey key, int ownerID,
		LocalDate validTo) throws SQLException {
		byte[] keyBytes = keyRepBytes(key, KeyRep.Type.PUBLIC);
		String sql = """
			INSERT INTO public_key(guid, key_val, owner, valid_to, obtained)
			VALUES (?, ?, ?, ?, datetime('now'));
		""";
		try (PreparedStatement stmt = dbcon.prepareStatement(sql)) {
			stmt.setBytes(1, guid);
			stmt.setBytes(2, keyBytes);
			stmt.setInt(3, ownerID);
			stmt.setString(4, validTo.toString());
			return stmt.executeUpdate();
		}
	}
	
	private static void savePrivateKey(byte[] guid, PrivateKey key) throws IOException {
		String keyFileName = HexFormat.of().formatHex(guid);
		Path keyFilePath = Path.of(Configurations.keystorePath, keyFileName);
		Files.write(keyFilePath, keyRepBytes(key, KeyRep.Type.PRIVATE));
	}

	public static Optional<PublicKey> getPublicKey(Connection dbcon, byte[] guid) throws SQLException, ClassNotFoundException, IOException {
		String sql = """
			SELECT key_val
			FROM public_key
			WHERE guid = ?;
		""";
		try (PreparedStatement stmt = dbcon.prepareStatement(sql)) {
			stmt.setBytes(1, guid);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				InputStream keyBytes = rs.getBinaryStream(1);
				return Optional.of((PublicKey) ((new ObjectInputStream(keyBytes)).readObject()));
			} else {
				return Optional.empty();
			}
		}
	}
	
	public static PrivateKey getPrivateKey(byte[] guid) throws IOException, ClassNotFoundException {
		String keyFileName = HexFormat.of().formatHex(guid);
		Path keyFilePath = Path.of(Configurations.keystorePath, keyFileName);
		try (InputStream fileStream = Files.newInputStream(keyFilePath);
				ObjectInputStream ois = new ObjectInputStream(fileStream)) {
			return (PrivateKey) ois.readObject();
		}
	}

}
