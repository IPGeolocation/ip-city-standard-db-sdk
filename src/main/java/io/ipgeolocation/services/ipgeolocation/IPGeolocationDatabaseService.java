package io.ipgeolocation.services.ipgeolocation;

import com.google.common.net.InetAddresses;
import io.ipgeolocation.exceptions.BogonIPException;
import io.ipgeolocation.exceptions.IPNotFoundException;
import io.ipgeolocation.exceptions.InvalidIPAddressException;
import io.ipgeolocation.models.IPGeolocation;
import io.ipgeolocation.models.db.IPCityPlace;
import io.ipgeolocation.services.database.DatabaseService;
import io.ipgeolocation.services.database.MMDBDatabaseService;
import io.ipgeolocation.services.params.BulkGeolocationParams;
import io.ipgeolocation.services.params.IPGeolocationParams;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IPGeolocationDatabaseService {
  private final MMDBDatabaseService mmdbDatabaseService = MMDBDatabaseService.getInstance();

  private DatabaseService databaseService;
  private boolean initialized = false;

  private static final IPGeolocationDatabaseService ipGeolocationDatabaseServiceInstance = new IPGeolocationDatabaseService();

  public static IPGeolocationDatabaseService getInstance() {
    return ipGeolocationDatabaseServiceInstance;
  }

  private IPGeolocationDatabaseService() {
    initDatabase();
  }

  public void initDatabase() {
    if (initialized) return;
    initialized = true;
    databaseService = mmdbDatabaseService;
    databaseService.loadDatabases();
  }

  public List<IPGeolocation> lookupIPGeolocationBulk(BulkGeolocationParams params) {
    String[] ips = params.getIPAddresses();
    String lang = params.getLang();

    if (Objects.isNull(ips) || ips.length == 0) {
      throw new IllegalArgumentException("'ips' must not be empty.");
    }

    if (ips.length > 1000) {
      throw new IllegalArgumentException("Maximum 1000 IP addresses are allowed in a single request.");
    }

    if (Objects.isNull(lang)) {
      throw new NullPointerException("'lang' must not be null.");
    }

    List<IPGeolocation> ipGeolocationResponseList = new ArrayList<>();

    for (String ip : ips) {
      ipGeolocationResponseList.add(lookup(ip, lang, true));
    }

    return ipGeolocationResponseList;
  }

  public IPGeolocation lookupIPGeolocation(IPGeolocationParams params) {
    String ip = params.getIPAddress();

    String lang = params.getLang();

    return lookup(ip, lang, false);
  }

  private IPGeolocation lookup(String ip, String lang, boolean isBulk) {

    return lookupIPGeolocation(ip, lang, isBulk);
  }

  private IPGeolocation lookupIPGeolocation(String ip, String lang, boolean isBulk) {

    if (Objects.isNull(ip) || ip.trim().isEmpty()) {
      throw new IllegalArgumentException("'ip' must not be empty or null.");
    }

    if (Objects.isNull(lang)) {
      throw new NullPointerException("'lang' must not be null.");
    }

    InetAddress inetAddress = null;

    if (InetAddresses.isInetAddress(ip)) {
      inetAddress = InetAddresses.forString(ip);
    } else {
      if (!isBulk) {
        throw new InvalidIPAddressException("Invalid IP address: " + ip);
      }
      return null;
    }

    IPCityPlace ipCity = databaseService.findIPGeolocation(inetAddress);

    if (ipCity == null) {
      if (!isBulk) {
        throw new IPNotFoundException(String.format("Provided IP address '%s' doesn't exist in IPGeolocation database.", inetAddress.getHostAddress()));
      }
      return null;
    } else if ("ZZ".equals(ipCity.getLocation().getCountry().getCode2())) {
      if (!isBulk) {
        throw new BogonIPException(String.format("%s: Bogon IP address. Bogon IP addresses are reserved like private, multicast, etc.", inetAddress.getHostAddress()));
      }
      return null;
    } else {

      return IPGeolocation.from(ipCity, lang);
    }
  }
}
