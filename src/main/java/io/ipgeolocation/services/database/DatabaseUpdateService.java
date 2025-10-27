package io.ipgeolocation.services.database;

import io.ipgeolocation.services.common.http.HttpRequests;
import kong.unirest.HttpResponse;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class DatabaseUpdateService {
  private static final System.Logger LOG = System.getLogger(DatabaseUpdateService.class.getName());
  private boolean initialized = false;
  private static final DatabaseUpdateService databaseUpdateServiceInstance = new DatabaseUpdateService();
  private String apiKey;
  private final String dbDownloadURL = "https://database.ipgeolocation.io/v2/download/geo-std/ip-city?format=mmdb";

  private String workingDirectory;

  private DatabaseUpdateService() {
    init();
  }

  public static DatabaseUpdateService getInstance() {
    return databaseUpdateServiceInstance;
  }

  private void init() {
    if (initialized) return;
    initialized = true;

    String jsonFileName = "database-config.json";
    String configFolderName = "ipgeolocation";
    String homeDir = System.getProperty("user.home");
    String jsonFilePath = homeDir + File.separator + "conf" + File.separator + configFolderName + File.separator + jsonFileName;

    File jsonConfigFile = new File(jsonFilePath);

    if (!(jsonConfigFile.isFile() && jsonConfigFile.exists())) {
      throw new IllegalStateException("Couldn't find the database configuration at " + jsonFilePath + " path.");
    }

    JSONObject databaseConfigJson = null;

    StringBuilder jsonContent = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new FileReader(jsonFilePath))) {
      String line;
      while ((line = reader.readLine()) != null) {
        jsonContent.append(line);
      }
      databaseConfigJson = new JSONObject(jsonContent.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (Objects.isNull(databaseConfigJson)) {
      throw new NullPointerException("No database configuration found at " + jsonFilePath + " path.");
    }

    String[] required = {"workingDirectory", "apiKey"};
    List<String> missing = new ArrayList<>();

    for (String key : required) {
      if (!databaseConfigJson.has(key) || databaseConfigJson.isNull(key)
              || (databaseConfigJson.opt(key) instanceof String
              && ((String) databaseConfigJson.opt(key)).trim().isEmpty())) {
        missing.add(key);
      }
    }

    if (!missing.isEmpty()) {
      throw new IllegalArgumentException(
              "Missing required database configuration(s): "
                      + String.join(", ", missing)
                      + " at " + jsonFilePath + "."
      );
    }


    apiKey = databaseConfigJson.getString("apiKey");
    workingDirectory = databaseConfigJson.getString("workingDirectory");

    if (Objects.isNull(apiKey) || apiKey.isEmpty() ||
            Objects.isNull(workingDirectory) || workingDirectory.isEmpty()) {
      throw new IllegalStateException("Invalid database configuration: {\"apiKey\": \"" + maskMiddle(apiKey) + "\"," + "\", \"workingDirectory\": \"" + workingDirectory + "\"}");
    }

    LOG.log(System.Logger.Level.INFO,
            "Database config (JSON) is: {0}",
            "{\"apiKey\":\"" + maskMiddle(apiKey) + "\"}");
  }

  public void downloadDatabaseFromDatabaseDownloadAPI() {

    if (!Files.exists(Path.of(workingDirectory))) {
      LOG.log(System.Logger.Level.ERROR,
              "{0} directory does not exist, Please create it.",
              workingDirectory);
      return;
    }
    LOG.log(System.Logger.Level.INFO, "Downloading database...");
    try {
      HttpResponse<File> downloadDatabaseFileResponse = HttpRequests.getAndFileResponse(
              dbDownloadURL,
              Paths.get(workingDirectory, UUID.randomUUID() + ".zip").toString(),
              Map.of("apiKey", apiKey));

      if (downloadDatabaseFileResponse != null && downloadDatabaseFileResponse.getStatus() == 200) {
        File downloadedDatabaseFile = downloadDatabaseFileResponse.getBody();
        ZipInputStream zis = new ZipInputStream(new FileInputStream(downloadedDatabaseFile));
        File destDir = new File(workingDirectory);
        byte[] buffer = new byte[1024];
        ZipEntry zipEntry;

        while (!Objects.isNull(zipEntry = zis.getNextEntry())) {
          File newFile = newFile(destDir, zipEntry);

          if (zipEntry.isDirectory()) {
            if (!newFile.isDirectory() && !newFile.mkdirs()) {
              throw new IOException("Failed to create directory: " + newFile);
            }
          } else {
            File parent = newFile.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
              throw new IOException("Failed to create directory: " + parent);
            }

            try (FileOutputStream fos = new FileOutputStream(newFile)) {
              int len;
              while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
              }
            }
          }
        }

        Files.delete(downloadedDatabaseFile.toPath());
      } else {
        LOG.log(System.Logger.Level.ERROR,
                "Either your database subscription or the API key ({0}) is not valid. "
                        + "Please contact ipgeolocation.io support at support@ipgeolocation.io.",
                maskMiddle(apiKey));
        return;

      }
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    LOG.log(System.Logger.Level.INFO, "Download completed.");
  }

  private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
    File destinationFile = new File(destinationDir, zipEntry.getName());
    String destDirPath = destinationDir.getCanonicalPath();
    String destFilePath = destinationFile.getCanonicalPath();

    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw new IOException("Entry is outside of the target directory: " + zipEntry.getName());
    }

    return destinationFile;
  }

  public String getWorkingDirectory() {
    return workingDirectory;
  }

  private String maskMiddle(String input) {
    if (input == null || input.length() <= 4) return input;

    int len = input.length();
    String start = input.substring(0, 2);
    String end = input.substring(len - 2);

    return start + "*".repeat(len - 4) + end;
  }

}
