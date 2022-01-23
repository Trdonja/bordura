package repository;

class Configurations {
	
	
	static final String repositoryRootPath = "C:\\Users\\Domen\\Vsebina\\Bordura\\";
	static final String contentPath = repositoryRootPath + "content\\";
	static final String databasePath = repositoryRootPath + "database\\";
	static final String keystorePath = repositoryRootPath + "keystore\\";
	
	static final String dbUrl = "jdbc:sqlite:" + databasePath + "bordura.db";

}
