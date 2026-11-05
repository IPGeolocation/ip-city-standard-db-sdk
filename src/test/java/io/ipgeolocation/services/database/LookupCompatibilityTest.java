package io.ipgeolocation.services.database;

import io.ipgeolocation.exceptions.BogonIPException;
import io.ipgeolocation.exceptions.IPNotFoundException;
import io.ipgeolocation.exceptions.InvalidIPAddressException;
import io.ipgeolocation.services.IPGeolocationLookupService;
import io.ipgeolocation.services.params.BulkGeolocationParams;
import io.ipgeolocation.services.params.IPGeolocationParams;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LookupCompatibilityTest {
  private IPGeolocationLookupService service;

  @ParameterizedTest @ValueSource(booleans = {true, false})
  void runsExistingLookupsWithoutLoggingTheConfiguredKey(boolean shortKey, @TempDir Path home) throws Exception {
    Path config = Files.createDirectories(home.resolve("conf/ipgeolocation"));
    try (var in = LookupCompatibilityTest.class.getResourceAsStream("/city-old.mmdb")) {
      Files.copy(Objects.requireNonNull(in), home.resolve(DatabaseInstaller.DATABASE));
    }
    String key = UUID.randomUUID().toString();
    if (shortKey) key = "~!" + key.substring(0, 2);
    Files.writeString(config.resolve("database-config.json"), new org.json.JSONObject()
        .put("workingDirectory", home.toString()).put("apiKey", key).toString());
    boolean windows = System.getProperty("os.name").startsWith("Windows");
    Path java = Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
    var process = new ProcessBuilder(java.toString(), "-Duser.home=" + home, "-cp",
        System.getProperty("java.class.path"), LookupCompatibilityTest.class.getName())
        .redirectErrorStream(true).start();
    String output;
    try (var in = process.getInputStream()) {
      output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertEquals(0, process.waitFor(), output);
    assertFalse(output.contains(key));
    if (!shortKey) assertFalse(output.contains(key.substring(0, 2) + "*"));
  }

  public static void main(String[] args) {
    var probe = new LookupCompatibilityTest();
    probe.service = IPGeolocationLookupService.getInstance();
    probe.singleLookupsKeepTheirNamesAndErrors();
    probe.bulkLookupsKeepOrderNullRowsAndTheOriginalLimit();
  }

  private IPGeolocationParams single(String ip, String lang) {
    return IPGeolocationParams.builder().withIPAddress(ip).withLang(lang).build();
  }

  private BulkGeolocationParams bulk(String... ips) {
    return BulkGeolocationParams.builder().withIPAddresses(ips).withLang("de").build();
  }

  private void singleLookupsKeepTheirNamesAndErrors() {
    var ipv4 = service.getIPCityGeolocation(single("8.8.8.8", null));
    assertEquals("Oldtown", ipv4.getLocation().getCity().getName());
    assertEquals("GB", ipv4.getLocation().getCountry().getCode2());
    assertEquals("Europe/London", ipv4.getTimeZone());
    assertEquals("Altstadt", service.getIPCityGeolocation(single("2001:4860::8", "de-DE")).getLocation().getCity().getName());
    assertEquals("测试", service.getIPCityGeolocation(single("8.8.8.8", "cn")).getLocation().getCity().getName());
    assertThrows(InvalidIPAddressException.class, () -> service.getIPCityGeolocation(single("not-an-ip", "en")));
    assertThrows(BogonIPException.class, () -> service.getIPCityGeolocation(single("10.0.0.1", "en")));
    assertThrows(IPNotFoundException.class, () -> service.getIPCityGeolocation(single("1.1.1.1", "en")));
  }

  private void bulkLookupsKeepOrderNullRowsAndTheOriginalLimit() {
    var rows = service.getIPCityGeolocation(bulk("not-an-ip","8.8.8.8","10.0.0.1","1.1.1.1","2001:4860::8"));
    assertEquals(5, rows.size());
    for (int i : new int[]{0,2,3}) assertNull(rows.get(i));
    for (int i : new int[]{1,4}) assertEquals("Altstadt", rows.get(i).getLocation().getCity().getName());
    String[] limit = new String[1000];
    Arrays.fill(limit, "8.8.8.8");
    assertEquals(1000, service.getIPCityGeolocation(bulk(limit)).size());
    assertThrows(IllegalArgumentException.class, () -> service.getIPCityGeolocation(bulk(Arrays.copyOf(limit, 1001))));
    assertThrows(IllegalArgumentException.class, () -> service.getIPCityGeolocation(bulk("8.8.8.8"," ")));
    assertThrows(IllegalArgumentException.class, () -> service.getIPCityGeolocation(bulk("8.8.8.8",null)));
  }
}
