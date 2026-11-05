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
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.*;

final class DatabaseInstaller {
  static final String DATABASE = "db-ip-location.mmdb";
  private static final long MAX_ARCHIVE = 4L << 30;
  private static final Pattern MAC_ACL = Pattern.compile("^ *[0-9]+: (.+?) (inherited )?(allow|deny) (.+)$");
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
        long received = copyLimited(in, out, limit);
        if (length >= 0 && received != length) throw new IOException("Incomplete download.");
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
      boolean linux = System.getProperty("os.name").equals("Linux");
      if (linux) {
        for (String cp : new String[]{"/usr/bin/cp", "/bin/cp"}) {
          Path command = Path.of(cp);
          if (Files.isExecutable(command) && !command.toRealPath().endsWith("busybox") && !command.toRealPath().endsWith("toybox")) {
            runTool(null, null, cp, "--attributes-only", "--preserve=mode,ownership", "--",
                target.toAbsolutePath().toString(), candidate.toAbsolutePath().toString());
            return;
          }
        }
      }
      var old = Files.readAttributes(target, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      var current = posix.readAttributes();
      if (!old.group().equals(current.group())) posix.setGroup(old.group());
      if (!old.owner().equals(current.owner())) posix.setOwner(old.owner());
      posix.setPermissions(old.permissions());
      if (linux) {
        String rules = runTool(null, null, "/usr/bin/getfacl", "-cpn", "--", target.toAbsolutePath().toString());
        runTool(null, rules, "/usr/bin/setfacl", "--set-file=-", "--", candidate.toAbsolutePath().toString());
        if (!rules.equals(runTool(null, null, "/usr/bin/getfacl", "-cpn", "--", candidate.toAbsolutePath().toString()))) {
          throw new IOException("Cannot preserve database permissions.");
        }
      } else if (System.getProperty("os.name").equals("Mac OS X")) {
        copyMacAcl(target, candidate);
      } else {
        throw new IOException("Cannot preserve database permissions on this filesystem.");
      }
    } else {
      var acl = Files.getFileAttributeView(candidate, AclFileAttributeView.class);
      if (acl == null) throw new IOException("Cannot preserve database permissions.");
      var old = Files.getFileAttributeView(target, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
      if (!old.getOwner().equals(acl.getOwner())) acl.setOwner(old.getOwner());
      acl.setAcl(old.getAcl());
    }
  }

  private static void copyMacAcl(Path source, Path candidate) throws IOException {
    List<String> entries = macAcl(source);
    if (entries.equals(macAcl(candidate))) return;
    var input = new StringBuilder();
    for (String entry : entries) {
      var rule = MAC_ACL.matcher(entry);
      if (!rule.matches()) throw new IOException("Unrecognized database access rule.");
      input.append(rule.group(1)).append(':').append(rule.group(3)).append(':').append(rule.group(4));
      if (rule.group(2) != null) input.append(",inherited");
      input.append('\n');
    }
    runTool(candidate.getParent(), input.toString(), "/bin/chmod", entries.isEmpty() ? "-N" : "-E", "./" + DATABASE);
    if (!entries.equals(macAcl(candidate))) throw new IOException("Cannot preserve database permissions.");
  }

  private static List<String> macAcl(Path path) throws IOException {
    return runTool(path.getParent(), null, "/bin/ls", "-lde", "./" + DATABASE).lines().skip(1).toList();
  }

  private static String runTool(Path directory, String input, String... command) throws IOException {
    var builder = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.DISCARD);
    if (directory != null) builder.directory(directory.toFile());
    builder.environment().put("LC_ALL", "C");
    Process child = builder.start();
    try {
      try (var stdin = child.getOutputStream()) {
        if (input != null) stdin.write(input.getBytes(StandardCharsets.UTF_8));
      }
      String output;
      try (var stdout = child.getInputStream()) {
        output = new String(stdout.readAllBytes(), StandardCharsets.UTF_8);
      }
      if (child.waitFor() != 0) throw new IOException("Cannot preserve database permissions.");
      return output;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Database update interrupted.", e);
    } finally {
      child.destroyForcibly();
    }
  }
}
