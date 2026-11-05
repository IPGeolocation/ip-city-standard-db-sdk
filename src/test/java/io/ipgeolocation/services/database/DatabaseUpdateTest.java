package io.ipgeolocation.services.database;

import com.maxmind.db.Reader;
import com.sun.net.httpserver.HttpServer;
import io.ipgeolocation.models.db.IPCityPlace;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseUpdateTest {
  @TempDir Path folder;
  private static final String DB = DatabaseInstaller.DATABASE;
  private static KeyPair rsa, ec;
  private HttpServer server;
  private String origin, failure = "";
  private final String token = "+&/%=" + UUID.randomUUID();
  private final List<String> requests = new CopyOnWriteArrayList<>();
  private byte[] archive, signature, publicKey;

  @BeforeAll static void createKeys() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    rsa = generator.generateKeyPair();
    ec = KeyPairGenerator.getInstance("EC").generateKeyPair();
  }

  @BeforeEach void prepare() throws Exception {
    Files.write(folder.resolve(DB), fixture("city-old"));
    Files.writeString(folder.resolve("keep.txt"), "unchanged");
    signed(zip(Map.of(DB, fixture("city-new"))), rsa);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      try (exchange) {
        var uri = exchange.getRequestURI();
        var params = new HashMap<String, String>();
        for (String param : Objects.toString(uri.getRawQuery(), "").split("&")) {
          if (param.isEmpty()) continue;
          String[] pair = param.split("=", 2);
          params.put(pair[0], URLDecoder.decode(pair[1], UTF_8));
        }
        String part = "unknown";
        if (token.equals(params.remove("apiKey"))) {
          if (uri.getPath().equals("/get-checksum") && params.isEmpty()) part = "key";
          else if (uri.getPath().equals("/v2/download/geo-std/ip-city")) {
            if (params.equals(Map.of("format","mmdb"))) part = "archive";
            if (params.equals(Map.of("format","mmdb","downloadSignature","true"))) part = "signature";
          }
        }
        requests.add(part);
        if (failure.equals("redirect " + part)) {
          exchange.getResponseHeaders().set("Location", origin + "/unexpected");
          exchange.sendResponseHeaders(302, -1);
          return;
        }
        int status = part.equals(failure) ? 503 : part.equals("unknown") ? 400 : 200;
        byte[] body = status != 200 ? token.getBytes(UTF_8) :
            switch (part) { case "key" -> publicKey; case "signature" -> signature; default -> archive; };
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
      }
    });
    server.start();
    origin = "http://127.0.0.1:" + server.getAddress().getPort();
  }

  @AfterEach void stop() { if (server != null) server.stop(0); }

  private static byte[] fixture(String name) throws IOException {
    try (var stream = DatabaseUpdateTest.class.getResourceAsStream("/" + name + ".mmdb")) {
      return Objects.requireNonNull(stream).readAllBytes();
    }
  }

  private static byte[] zip(Map<String, byte[]> files) throws IOException {
    var bytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(bytes)) {
      for (var file : files.entrySet()) {
        var crc = new CRC32();
        crc.update(file.getValue());
        var entry = new ZipEntry(file.getKey());
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(file.getValue().length);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(file.getValue());
        zip.closeEntry();
      }
    }
    return bytes.toByteArray();
  }

  private void signed(byte[] bytes, KeyPair pair) throws Exception {
    archive = bytes;
    var signer = Signature.getInstance(pair == ec ? "SHA256withECDSA" : "SHA256withRSA");
    signer.initSign(pair.getPrivate());
    signer.update(bytes);
    signature = signer.sign();
    publicKey = ("-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder().encodeToString(pair.getPublic().getEncoded()) +
        "\n-----END PUBLIC KEY-----\n").getBytes(StandardCharsets.US_ASCII);
  }

  private void update() throws Exception { DatabaseInstaller.update(folder, token, origin); }

  private String installedCity() throws IOException {
    try (var reader = new Reader(folder.resolve(DB).toFile(), Reader.FileMode.MEMORY)) {
      return reader.get(InetAddress.getByName("8.8.8.8"), IPCityPlace.class).getLocation().getCity().getName().getNameEnglish();
    }
  }

  private void checkFiles() throws IOException {
    assertEquals("unchanged", Files.readString(folder.resolve("keep.txt")));
    try (var paths = Files.list(folder)) {
      assertEquals(Set.of(DB, "keep.txt"), new HashSet<>(paths.map(p -> p.getFileName().toString()).toList()));
    }
  }

  @ParameterizedTest @ValueSource(strings = {"replace", "fresh", "ec"})
  void installsOnlyTheVerifiedDatabase(String mode) throws Exception {
    if (mode.equals("fresh")) Files.delete(folder.resolve(DB));
    boolean posix = Files.getFileStore(folder).supportsFileAttributeView("posix") && !mode.equals("fresh");
    if (posix) Files.setPosixFilePermissions(folder.resolve(DB), PosixFilePermissions.fromString("rw-r-----"));
    String outside = UUID.randomUUID() + ".txt";
    signed(zip(Map.of("./" + DB, fixture("city-new"), "keep.txt", new byte[]{1}, "../" + outside, new byte[]{2})),
        mode.equals("ec") ? ec : rsa);
    update();
    assertEquals("Newtown", installedCity());
    assertArrayEquals(fixture("city-new"), Files.readAllBytes(folder.resolve(DB)));
    assertFalse(Files.exists(folder.resolveSibling(outside)));
    assertEquals(List.of("key", "signature", "archive"), requests);
    if (posix) assertEquals(PosixFilePermissions.fromString("rw-r-----"), Files.getPosixFilePermissions(folder.resolve(DB)));
    checkFiles();
  }

  @ParameterizedTest @ValueSource(strings = {"key", "signature", "archive", "redirect key", "redirect signature", "redirect archive", "bad key", "oversized proof", "bad signature",
      "tampered", "nonzip", "truncated zip", "corrupt entry", "no database", "duplicate", "invalid mmdb", "wrong schema"})
  void failedUpdateKeepsTheWorkingDatabase(String scenario) throws Exception {
    switch (scenario) {
      case "key", "signature", "archive", "redirect key", "redirect signature", "redirect archive" -> failure = scenario;
      case "bad key" -> publicKey = "invalid".getBytes(UTF_8);
      case "oversized proof" -> signature = new byte[17 * 1024];
      case "bad signature" -> signature = new byte[signature.length];
      case "tampered" -> archive[0] ^= 1;
      case "nonzip" -> signed("<html>Not a database</html>".getBytes(UTF_8), rsa);
      case "truncated zip" -> signed(Arrays.copyOf(archive, archive.length - 20), rsa);
      case "corrupt entry" -> {
        archive[30 + DB.length() + 1] ^= 1;
        signed(archive, rsa);
      }
      case "no database" -> signed(zip(Map.of("folder/" + DB, fixture("city-new"))), rsa);
      case "duplicate" -> signed(zip(Map.of(DB, fixture("city-new"), "./" + DB, fixture("city-old"))), rsa);
      case "invalid mmdb" -> signed(zip(Map.of(DB, "invalid".getBytes(UTF_8))), rsa);
      case "wrong schema" -> signed(zip(Map.of(DB, fixture("not-city"))), rsa);
    }
    assertThrows(Exception.class, this::update);
    assertFalse(requests.contains("unknown"));
    assertEquals("Oldtown", installedCity());
    assertArrayEquals(fixture("city-old"), Files.readAllBytes(folder.resolve(DB)));
    checkFiles();
  }

  @Test void aNonRegularTargetIsNeverReplaced() throws Exception {
    Path target = folder.resolve(DB);
    Files.delete(target);
    Files.createDirectory(target);
    Files.writeString(target.resolve("marker"), "original");
    assertThrows(IOException.class, this::update);
    assertEquals("original", Files.readString(target.resolve("marker")));
    checkFiles();
  }

  @Test void boundsStreamingWithoutWritingExcessBytes() throws Exception {
    byte[] input = new byte[65537];
    var sink = new ByteArrayOutputStream();
    assertEquals(input.length, DatabaseInstaller.copyLimited(new ByteArrayInputStream(input), sink, input.length));
    assertArrayEquals(input, sink.toByteArray());
    sink.reset();
    assertThrows(IOException.class, () -> DatabaseInstaller.copyLimited(new ByteArrayInputStream(input), sink, input.length - 1));
    assertTrue(sink.size() <= input.length - 1);
  }
}
