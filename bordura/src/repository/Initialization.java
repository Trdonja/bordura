package repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Initialization {
	
	public static void createNewDatabase() throws SQLException {
		try (Connection dbcon = DriverManager.getConnection(Configurations.dbUrl)) {
			// Skip. If connection gets established, a database is created.
		}
	}
	
	public static void createTables() throws SQLException {
		try (Connection dbcon = DriverManager.getConnection(Configurations.dbUrl);
			Statement stmt = dbcon.createStatement()) {
			stmt.execute("PRAGMA foreign_keys = ON;"); // enable foreign keys
			stmt.execute("""
				CREATE TABLE IF NOT EXISTS peer(
					id          INTEGER PRIMARY KEY,
					name        TEXT NOT NULL,
					added_on    TEXT NOT NULL, -- date time
					description TEXT,
					portrait    BLOB -- small picture
				);
			""");
			stmt.execute("""
				CREATE TABLE IF NOT EXISTS address(
					id           INTEGER PRIMARY KEY,
					ip           BLOB NOT NULL,
					port         INTEGER NOT NULL,
					UNIQUE (ip, port)
				);
			""");
			stmt.execute("""
				CREATE TABLE IF NOT EXISTS address_usage(
					address_id   INTEGER REFERENCES address(id),
					peer_id      INTEGER REFERENCES peer(id),
					active_from  TEXT, -- date time
					active_to    TEXT, -- date time for expired relation or string 'indefinite' for valid relation
					last_contact TEXT -- date time
				);
			""");
			stmt.execute("""
				CREATE TABLE IF NOT EXISTS public_key(
					id       INTEGER PRIMARY KEY,
					guid     BLOB UNIQUE NOT NULL,
					key_val  BLOB UNIQUE NOT NULL, -- serialized Key object bytes
					owner    INTEGER REFERENCES peer(id),
					valid_to TEXT, -- date time
					obtained TEXT -- date time
				)
			""");
			stmt.execute("""
				CREATE TABLE IF NOT EXISTS publish_list(
					id          INTEGER PRIMARY KEY,
					guid        BLOB UNIQUE NOT NULL, -- uuid
					name        TEXT NOT NULL,
					description TEXT,
					public      INTEGER CHECK(public >= 0 AND public <= 1) -- boolean, {1; public} | {0; private}
				);
			""");
			stmt.execute("""
				CREATE TABLE IF NOT EXISTS participates(
					peer          INTEGER NOT NULL REFERENCES peer(id),
					publish_list  INTEGER NOT NULL REFERENCES publish_list(id),
					participation INTEGER CHECK(participation >= 1 AND participation <= 2), -- {1; allowed} | {2; denied}
					UNIQUE (peer, publish_list)
				);
			""");
			stmt.execute("""
				CREATE TABLE IF NOT EXISTS post(
					id                 INTEGER PRIMARY KEY,
					hash               BLOB UNIQUE NOT NULL,
					signature          BLOB,
					signature_key      INTEGER REFERENCES public_key(id),
					obtained_from      INTEGER REFERENCES address(id),
					obtain_date        TEXT,
					total_size         INTEGER NOT NULL, -- sum of all corresp. content lengths
					publishable        INTEGER CHECK(publishable >= 0 AND publishable <= 1) -- boolean
				);
			""");
			stmt.execute("""
				CREATE TABLE IF NOT EXISTS content(
					id         INTEGER PRIMARY KEY,
					post_id    INTEGER NOT NULL REFERENCES post(id),
					ordinal    INTEGER NOT NULL,
					type       INTEGER NOT NULL, -- text/message | image/jpeg | video/mp4 | audio/mp3 | post_reference | datum | summarization | label | balast | ...
					storage    INTEGER NOT NULL CHECK(storage >= 0 AND storage <= 1), -- inline or in file
					value      TEXT NOT NULL, -- can be BLOB too; is a file-path, if storage == 1
					properties TEXT_JSON, -- properties for classification in json format
					UNIQUE (post_id, ordinal)
				);
			""");
			stmt.execute("""
				CREATE TABLE IF NOT EXISTS permits(
					post       INTEGER NOT NULL REFERENCES post(id),
					peer       INTEGER NOT NULL REFERENCES peer(id),
					permission INTEGER CHECK(permission >= 1 AND permission <= 2), -- {1; allow} | {2; deny}
					UNIQUE (post, peer)
				);
			""");
			stmt.execute("""
				CREATE TABLE IF NOT EXISTS included(
					post         INTEGER NOT NULL REFERENCES post(id),
					publish_list INTEGER NOT NULL REFERENCES publish_list(id),
					UNIQUE (post, publish_list)
				);
			""");
		}
	}

}
