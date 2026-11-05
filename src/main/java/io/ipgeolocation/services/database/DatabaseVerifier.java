package io.ipgeolocation.services.database;

import com.google.common.base.Strings;
import com.maxmind.db.InvalidNetworkException;
import com.maxmind.db.Reader;
import io.ipgeolocation.models.db.IPCityPlace;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;

final class DatabaseVerifier {
  private DatabaseVerifier() {}

  public static void main(String[] args) throws IOException {
    verify(Path.of(args[0]));
  }

  static void verify(Path candidate) throws IOException {
    try (Reader reader = new Reader(candidate.toFile(), Reader.FileMode.MEMORY_MAPPED)) {
      var networks = reader.networks(IPCityPlace.class);
      for (int i = 0; i < 4096 && networks.hasNext(); i++) {
        IPCityPlace place = networks.next().getData();
        if (place != null && place.getLocation() != null && place.getLocation().getCity() != null &&
            place.getLocation().getCountry() != null && place.getLocation().getCountry().getCode2() != null &&
            place.getTimeZone() != null) return;
      }
      throw new IOException("The archive does not contain a city database.");
    } catch (InvalidNetworkException e) {
      throw new IOException("Invalid database network.", e);
    }
  }

  static void verifyInChild(Path candidate) throws IOException {
    var classpath = new LinkedHashSet<String>();
    var copies = new ArrayList<Path>();
    try {
      for (Class<?> type : new Class<?>[]{DatabaseVerifier.class, Reader.class, Strings.class}) {
        var location = type.getProtectionDomain().getCodeSource();
        if (location != null && location.getLocation().getProtocol().equals("file")) {
          classpath.add(Path.of(location.getLocation().toURI()).toString());
        } else {
          var resource = type.getResource(type.getSimpleName() + ".class");
          if (resource == null || !(resource.openConnection() instanceof JarURLConnection jar))
            throw new IOException("Cannot locate a database validation library.");
          String url = jar.getJarFileURL().toString();
          if (!(url.startsWith("file:") || url.startsWith("nested:") || url.startsWith("jar:file:") || url.startsWith("jar:nested:")))
            throw new IOException("Database validation requires local libraries.");
          Path copy = Files.createTempFile(candidate.getParent(), "validation-", ".jar");
          copies.add(copy);
          try (var in = jar.getJarFileURL().openStream(); var out = Files.newOutputStream(copy)) {
            in.transferTo(out);
          }
          classpath.add(copy.toString());
        }
      }
      var java = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
      var command = new ProcessBuilder(java.toString(), "-Xmx32m", "-cp", String.join(File.pathSeparator, classpath),
          DatabaseVerifier.class.getName(), candidate.toAbsolutePath().toString());
      command.redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD);
      for (String key : new String[]{"JDK_JAVA_OPTIONS", "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS"}) command.environment().remove(key);
      Process child = command.start();
      try {
        if (child.waitFor() != 0) throw new IOException("Invalid city database.");
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Database validation interrupted.", e);
      } finally {
        if (child.isAlive()) child.destroyForcibly().onExit().join();
      }
    } catch (URISyntaxException e) {
      throw new IOException("Cannot locate a database validation library.", e);
    } finally {
      for (Path copy : copies) Files.deleteIfExists(copy);
    }
  }
}
