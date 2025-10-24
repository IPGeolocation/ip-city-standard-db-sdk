package io.ipgeolocation.services.path;

import io.ipgeolocation.services.database.DatabaseUpdateService;

public class PathsService {

  private static final PathsService pathsServiceInstance = new PathsService();
  private final DatabaseUpdateService databaseUpdateService = DatabaseUpdateService.getInstance();

  private boolean initialized = false;

  private PathsService() {
    init();
  }

  public static PathsService getInstance() {
    return pathsServiceInstance;
  }

  private String ipGeolocationMMDBDatabaseFilePath;

  private void init() {
    if (initialized) return;
    initialized = true;

    String osName = System.getProperty("os.name").toLowerCase();
    String homeDir;
    if (osName.contains("win")) {
      homeDir = databaseUpdateService.getWorkingDirectory();

      ipGeolocationMMDBDatabaseFilePath = String.format("%s\\db-ip-location.mmdb", homeDir);
    } else {
      homeDir = databaseUpdateService.getWorkingDirectory();

      ipGeolocationMMDBDatabaseFilePath = String.format("%s/db-ip-location.mmdb", homeDir);
    }
  }

  public String getIPGeolocationMMDBDatabaseFilePath() {
    return ipGeolocationMMDBDatabaseFilePath;
  }

}
