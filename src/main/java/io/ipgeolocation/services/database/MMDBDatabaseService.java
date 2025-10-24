package io.ipgeolocation.services.database;

import com.maxmind.db.NoCache;
import com.maxmind.db.NodeCache;
import com.maxmind.db.Reader;
import io.ipgeolocation.models.db.IPCityPlace;
import io.ipgeolocation.services.path.PathsService;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MMDBDatabaseService implements DatabaseService {
  private static final System.Logger LOG = System.getLogger(MMDBDatabaseService.class.getName());

  private final PathsService pathsService = PathsService.getInstance();

  private Reader ipGeolocationMMDBReader;
  private static final MMDBDatabaseService mMDBDatabaseServiceInstance = new MMDBDatabaseService();

  public static MMDBDatabaseService getInstance() {
    return mMDBDatabaseServiceInstance;
  }

  private MMDBDatabaseService() {
  }

  @Override
  public void loadDatabases() {
    Path ipGeolocationMMDBPath = Paths.get(pathsService.getIPGeolocationMMDBDatabaseFilePath());

    if (!(Files.isRegularFile(ipGeolocationMMDBPath) && Files.exists(ipGeolocationMMDBPath))) {
      throw new IllegalStateException(pathsService.getIPGeolocationMMDBDatabaseFilePath() + " is missing.");
    }

    NodeCache noCache = NoCache.getInstance();

    LOG.log(System.Logger.Level.INFO, "Loading IP Geolocation MMDB database...");
    try {
      ipGeolocationMMDBReader = new Reader(ipGeolocationMMDBPath.toFile(), noCache);
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException("Failed to initialize ip-geolocation MMDB reader.", e);
    }

  }

  public IPCityPlace findIPGeolocation(InetAddress inetAddress) {
    IPCityPlace ipCity = null;
    try {

        ipCity = ipGeolocationMMDBReader.get(inetAddress, IPCityPlace.class);

    } catch (IOException e) {
      e.printStackTrace();
    }

    return ipCity;
  }
}
