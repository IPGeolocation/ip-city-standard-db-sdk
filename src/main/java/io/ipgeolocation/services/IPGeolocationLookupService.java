package io.ipgeolocation.services;

import io.ipgeolocation.models.IPGeolocation;
import io.ipgeolocation.services.ipgeolocation.IPGeolocationDatabaseService;
import io.ipgeolocation.services.params.BulkGeolocationParams;
import io.ipgeolocation.services.params.IPGeolocationParams;

import java.util.List;

public class IPGeolocationLookupService {

  private final IPGeolocationDatabaseService databaseService = IPGeolocationDatabaseService.getInstance();
  private static final IPGeolocationLookupService ipGeolocationLookupServiceInstance = new IPGeolocationLookupService();

  public static IPGeolocationLookupService getInstance() {
    return ipGeolocationLookupServiceInstance;
  }

  private IPGeolocationLookupService() {
  }

  public IPGeolocation getIPCityGeolocation(IPGeolocationParams params) {
    IPGeolocation response = databaseService.lookupIPGeolocation(params);
    return response;
  }

  public List<IPGeolocation> getIPCityGeolocation(BulkGeolocationParams params) {
    List<IPGeolocation> responses = databaseService.lookupIPGeolocationBulk(params);
    return responses;
  }
}
