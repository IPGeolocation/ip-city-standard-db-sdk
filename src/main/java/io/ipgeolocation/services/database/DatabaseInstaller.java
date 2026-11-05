package io.ipgeolocation.services.database;

import com.maxmind.db.Reader;
import io.ipgeolocation.models.db.IPCityPlace;
import com.maxmind.db.InvalidNetworkException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Locale;
import java.util.zip.*;

final class DatabaseInstaller {
  static final String DATABASE = "db-ip-location.mmdb";
  private static final long MAX_ARCHIVE = 4L << 30;
  private static final int MAX_PROOF = 16 * 1024;

  private DatabaseInstaller() {}

  static void update(Path directory, String apiKey, String origin) throws IOException, GeneralSecurityException {
    String download = origin + "/v2/download/geo-std/ip-city?format=mmdb";
    Path stage = Files.createTempDirectory(directory, ".ipgeo-");
    Path archive = stage.resolve("download.zip");
    Path candidate = stage.resolve(DATABASE);
    try {
      byte[] publicKey = fetchSmall(origin + "/get-checksum", apiKey);
      byte[] signature = fetchSmall(download + "&downloadSignature=true", apiKey);
      try (OutputStream out = Files.newOutputStream(archive, StandardOpenOption.CREATE_NEW)) {
        fetch(download, apiKey, out, MAX_ARCHIVE);
      }
      verify(archive, publicKey, signature);
      extractDatabase(archive, candidate);
      validateDatabase(candidate);
      Path target = directory.resolve(DATABASE);
      preserveAccess(target, candidate);
      Files.move(candidate, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      for (Path p : new Path[]{candidate, archive, stage}) {
        try {
          Files.deleteIfExists(p);
        } catch (IOException | SecurityException e) {
          System.getLogger(DatabaseInstaller.class.getName()).log(System.Logger.Level.WARNING, "Could not remove a temporary database update file.");
        }
      }
    }
  }

  private static byte[] fetchSmall(String url, String apiKey) throws IOException {
    var out = new ByteArrayOutputStream();
    fetch(url, apiKey, out, MAX_PROOF);
    return out.toByteArray();
  }

  private static void fetch(String url, String apiKey, OutputStream out, long limit) throws IOException {
    String query = (url.contains("?") ? "&" : "?") + "apiKey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
    var connection = (HttpURLConnection) URI.create(url + query).toURL().openConnection();
    connection.setInstanceFollowRedirects(false);
    connection.setConnectTimeout(10_000);
    connection.setReadTimeout(60_000);
    try {
      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new IOException("Download request failed.");
      long length = connection.getContentLengthLong();
      if (length > limit) throw new IOException("Database download exceeds the size limit.");
      try (InputStream in = connection.getInputStream()) {
        if (copyLimited(in, out, limit) != length && length >= 0) throw new IOException("Incomplete download.");
      }
    } finally {
      connection.disconnect();
    }
  }

  static long copyLimited(InputStream in, OutputStream out, long limit) throws IOException {
    byte[] buffer = new byte[64 * 1024];
    long total = 0;
    for (int n; (n = in.read(buffer)) != -1;) {
      total += n;
      if (total > limit) throw new IOException("Database download exceeds the size limit.");
      out.write(buffer, 0, n);
    }
    return total;
  }

  private static void verify(Path archive, byte[] publicKey, byte[] signature) throws IOException, GeneralSecurityException {
    String pem = new String(publicKey, StandardCharsets.US_ASCII).strip();
    String begin = "-----BEGIN PUBLIC KEY-----", end = "-----END PUBLIC KEY-----";
    if (!pem.startsWith(begin) || !pem.endsWith(end)) throw new InvalidKeyException("Invalid database verification key.");
    byte[] der = Base64.getDecoder().decode(pem.substring(begin.length(), pem.length() - end.length()).replaceAll("\\s", ""));
    var spec = new X509EncodedKeySpec(der);
    PublicKey key;
    try {
      key = KeyFactory.getInstance("RSA").generatePublic(spec);
    } catch (InvalidKeySpecException notRsa) {
      key = KeyFactory.getInstance("EC").generatePublic(spec);
    }
    var verifier = Signature.getInstance(key.getAlgorithm().equals("RSA") ? "SHA256withRSA" : "SHA256withECDSA");
    verifier.initVerify(key);
    try (InputStream in = Files.newInputStream(archive)) {
      byte[] buffer = new byte[64 * 1024];
      for (int n; (n = in.read(buffer)) != -1;) verifier.update(buffer, 0, n);
    }
    if (!verifier.verify(signature)) throw new SignatureException("Invalid database signature.");
  }

  private static void extractDatabase(Path archive, Path candidate) throws IOException {
    try (ZipFile zip = new ZipFile(archive.toFile())) {
      if (zip.size() > 4096) throw new ZipException("Too many archive entries.");
      var entries = zip.entries();
      ZipEntry database = null;
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (!DATABASE.equals(entry.getName()) && !("./" + DATABASE).equals(entry.getName())) continue;
        if (database != null) throw new ZipException("Duplicate database entry.");
        database = entry;
      }
      if (database == null || database.getSize() <= 0 || database.getSize() > Integer.MAX_VALUE) {
        throw new ZipException("Missing or invalid database entry.");
      }
      CRC32 crc = new CRC32();
      try (InputStream in = new CheckedInputStream(zip.getInputStream(database), crc);
           OutputStream out = Files.newOutputStream(candidate, StandardOpenOption.CREATE_NEW)) {
        if (copyLimited(in, out, database.getSize()) != database.getSize() || crc.getValue() != database.getCrc()) {
          throw new ZipException("Incomplete database entry.");
        }
      }
    }
  }

  private static void validateDatabase(Path candidate) throws IOException {
    var mode = System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows")
        ? Reader.FileMode.MEMORY : Reader.FileMode.MEMORY_MAPPED;
    try (Reader reader = new Reader(candidate.toFile(), mode)) {
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

  private static void preserveAccess(Path target, Path candidate) throws IOException {
    BasicFileAttributes original;
    try {
      original = Files.readAttributes(target, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    } catch (NoSuchFileException absent) {
      return;
    }
    if (!original.isRegularFile()) throw new IOException("The database target is not a regular file.");
    var posix = Files.getFileAttributeView(candidate, PosixFileAttributeView.class);
    if (posix != null) {
      var old = Files.readAttributes(target, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      var current = posix.readAttributes();
      if (!old.group().equals(current.group())) posix.setGroup(old.group());
      if (!old.owner().equals(current.owner())) posix.setOwner(old.owner());
      posix.setPermissions(old.permissions());
    } else {
      var acl = Files.getFileAttributeView(candidate, AclFileAttributeView.class);
      if (acl != null) {
        var old = Files.getFileAttributeView(target, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (!old.getOwner().equals(acl.getOwner())) acl.setOwner(old.getOwner());
        acl.setAcl(old.getAcl());
      }
    }
  }
}
