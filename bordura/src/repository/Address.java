package repository;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class Address {

	public static int createNew(Connection dbcon, InetSocketAddress address, boolean isActive)
	throws SQLException {
		String sql = """
			INSERT INTO address(ip, port, peer_id, active, last_contact)
			VALUES (?, ?, NULL, ?, NULL);
		""";
		try (PreparedStatement stmt = dbcon.prepareStatement(sql)) {
			stmt.setBytes(1, address.getAddress().getAddress());
			stmt.setInt(2, address.getPort());
			stmt.setBoolean(3, isActive);
			return stmt.executeUpdate();
		}
	}
	
	public static int createNew(Connection dbcon, InetSocketAddress address, int peerID,
	boolean isActive) throws SQLException {
		String sql = """
			INSERT INTO address(ip, port, peer_id, active, last_contact)
			VALUES (?, ?, ?, ?, NULL);
		""";
		try (PreparedStatement stmt = dbcon.prepareStatement(sql)) {
			stmt.setBytes(1, address.getAddress().getAddress());
			stmt.setInt(2, address.getPort());
			stmt.setInt(3, peerID);
			stmt.setBoolean(4, isActive);
			return stmt.executeUpdate();
		}
	}
	
	public static int asscoiateWithPeer(Connection dbcon, InetSocketAddress address, int peerID)
	throws SQLException {
		String sql = """
			UPDATE address
			SET peer_id = ?
			WHERE ip = ? AND port = ?;
		""";
		try (PreparedStatement stmt = dbcon.prepareStatement(sql)) {
			stmt.setInt(1, peerID);
			stmt.setBytes(2, address.getAddress().getAddress());
			stmt.setInt(3, address.getPort());
			return stmt.executeUpdate();
		}
	}
	
	public static int updateLastContact(Connection dbcon, InetSocketAddress address)
	throws SQLException {
		String sql = """
			UPDATE address
			SET last_contact = datetime('now')
			WHERE ip = ? AND port = ?;
		""";
		try (PreparedStatement stmt = dbcon.prepareStatement(sql)) {
			stmt.setBytes(1, address.getAddress().getAddress());
			stmt.setInt(2, address.getPort());
			return stmt.executeUpdate();
		}
	}
	
	public static int updateLastContacts(Connection dbcon, InetSocketAddress[] addresses)
	throws SQLException {
		String sql = """
			UPDATE address
			SET last_contact = datetime('now')
			WHERE ip = ? AND port = ?;
		""";
		try (PreparedStatement stmt = dbcon.prepareStatement(sql)) {
			int count = 0;
			for (int i = 0; i < addresses.length; i++) {
				stmt.setBytes(1, addresses[i].getAddress().getAddress());
				stmt.setInt(2, addresses[i].getPort());
				count += stmt.executeUpdate();
			}
			return count;
		}
	}
	
	public static int setActive(Connection dbcon, InetSocketAddress address, boolean isActive)
	throws SQLException {
		String sql = """
			UPDATE address
			SET active = ?
			WHERE ip = ? AND port = ?;
		""";
		try (PreparedStatement stmt = dbcon.prepareStatement(sql)) {
			stmt.setBoolean(1, isActive);
			stmt.setBytes(2, address.getAddress().getAddress());
			stmt.setInt(3, address.getPort());
			return stmt.executeUpdate();
		}
	}
	
	public static Optional<InetSocketAddress> getActiveOf(Connection dbcon, int peerID)
	throws SQLException, IOException {
		String sql = """
			SELECT ip, port
			FROM address
			WHERE peer_id = ? AND active = 1;
		""";
		try (PreparedStatement stmt = dbcon.prepareStatement(sql)) {
			stmt.setInt(1, peerID);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				byte[] ipAddress = rs.getBinaryStream(1).readAllBytes();
				int port = rs.getInt(2);
				return Optional.of(new InetSocketAddress(InetAddress.getByAddress(ipAddress), port));
			} else {
				return Optional.empty();
			}
		}
	}
}
