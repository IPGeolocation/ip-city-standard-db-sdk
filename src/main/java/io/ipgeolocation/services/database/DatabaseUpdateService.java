package io.ipgeolocation.services.database;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUpdateService {
  private static final System.Logger LOG = System.getLogger(DatabaseUpdateService.class.getName());
  private final String apiKey;
  private final String workingDirectory;

  private DatabaseUpdateService() {
    Path path = Path.of(System.getProperty("user.home"), "conf", "ipgeolocation", "database-config.json");
    JSONObject config;
    try {
      config = new JSONObject(Files.readString(path));
    } catch (IOException e) {
      throw new IllegalStateException("Could not read the database configuration at " + path);
    } catch (JSONException e) {
      throw new IllegalArgumentException("Invalid JSON in the database configuration.");
    }
    List<String> missing = new ArrayList<>();
    for (String name : List.of("workingDirectory", "apiKey")) {
      if (!(config.opt(name) instanceof String value) || value.isBlank()) missing.add(name);
    }
    if (!missing.isEmpty()) {
      throw new IllegalArgumentException("Missing or invalid database configuration: " + String.join(", ", missing));
    }
    apiKey = config.getString("apiKey");
    workingDirectory = config.getString("workingDirectory");
  }

  private static final class Holder {
    private static final DatabaseUpdateService INSTANCE = new DatabaseUpdateService();
  }

  public static DatabaseUpdateService getInstance() {
    return Holder.INSTANCE;
  }

  public synchronized void downloadDatabaseFromDatabaseDownloadAPI() {
    try {
      Path directory = Path.of(workingDirectory);
      if (!Files.isDirectory(directory)) {
        LOG.log(System.Logger.Level.ERROR, "The database working directory does not exist.");
        return;
      }
      LOG.log(System.Logger.Level.INFO, "Downloading database...");
      DatabaseInstaller.update(directory, apiKey, "https://database.ipgeolocation.io");
      LOG.log(System.Logger.Level.INFO, "Download completed.");
    } catch (Exception e) {
      LOG.log(System.Logger.Level.ERROR, "Database update failed. The database was not replaced.");
    }
  }

  public String getWorkingDirectory() {
    return workingDirectory;
  }
}
