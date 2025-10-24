package io.ipgeolocation.services.database;

import io.ipgeolocation.models.db.IPCityPlace;

import java.net.InetAddress;

public interface DatabaseService {
  void loadDatabases();
  IPCityPlace findIPGeolocation(InetAddress inetAddress);
}
