# IP City Standard DB Java SDK

This is a guide on how to use IP City DB Java SDK to get geolocation data for an IP address.

## Requirements

- JDK 21 (This SDK is built and tested using JDK 21).
- Only for MMDB database, at least 2-4 GB RAM only.

## Installation

Java SDK can be installed by different methods given below:

### Maven

Add the following dependency in `pom.xml` file to use the IP City DB Java SDK.

```maven
<dependency>
    <groupId>io.ipgeolocation</groupId>
    <artifactId>ip-city-std-db</artifactId>
    <version>1.0.2</version>
</dependency>
```

### Gradle
```gradle
implementation 'io.ipgeolocation:ip-city-std-db:1.0.2'
```

### Ivy

Add the following dependency code in `ivy.xml` file to use the IP City DB Java SDK.

```ivy
<dependency org="io.ipgeolocation" name="ip-city-std-db" rev="1.0.2"/>
```

## Basic Usage

### For Linux

Follow the steps below to consume the **IP City database**:

- Create `~/conf/ipgeolocation` directory in the `home` directory.
  ```bash
  mkdir -p ~/conf/ipgeolocation
  ```

- Create `database-config.json` file in the `~/conf/ipgeolocation` directory.
  ```bash
  vi ~/conf/ipgeolocation/database-config.json
  ```

* Write the following JSON config values in it (You can use any editor of your choice like `nano` or `vim`.):
  ```json 
  {"workingDirectory": "/etc/ipgeolocation","apiKey": "YOUR_API_KEY"}
  ```
* Against `workingDirectory` key, replace `/etc/ipgeolocation` value with the directory path where you want to place the
  database files.
* Against `apiKey` key, replace `YOUR_API_KEY` value with the API key from your database subscription.

### For Windows

In PowerShell, create the settings folder:
```powershell
New-Item -ItemType Directory -Force "$HOME\conf\ipgeolocation"
```

Save `database-config.json` in that folder. For example, if your home folder is `C:\Users\Alex`:
```json
{"workingDirectory": "C:/Users/Alex/conf/ipgeolocation", "apiKey": "YOUR_API_KEY"}
```

Use your own home folder and your IPGeolocation.io database subscription key. To store the database elsewhere, set `workingDirectory` to a folder that already exists.

### Response in Multiple Languages

The geolocation information for an IP address can be retrieved in the following languages:

- English (en)
- German (de)
- Russian (ru)
- Japanese (ja)
- French (fr)
- Chinese Simplified (cn)
- Spanish (es)
- Czech (cs)
- Italian (it)
- persian (fa)
- korean (ko)
- Portuguese (pt)

### Download Database Files

You can either:

- **Manually Download and Extract Files**:
  Download, extract, and place the database files in the working directory specified in your JSON configuration file.

- **Automatically Download Files Using SDK**:
  The SDK checks the download before replacing `db-ip-location.mmdb` in your working directory. If the update fails, it keeps your existing database. Other files in that folder stay as they are.

  ```java
  DatabaseUpdateService databaseUpdateService = DatabaseUpdateService.getInstance();
  databaseUpdateService.downloadDatabaseFromDatabaseDownloadAPI();
  ```

## How to Get IP Geolocation

### A. Single IP Geolocation Lookup

To get IP Geolocation, follow these steps:

#### 1. Configure Geolocation Parameters

Build the geolocation parameters with the necessary options, such as IP address and language:

 ```java
  IPGeolocationParams geolocationParams =
        IPGeolocationParams
                .builder()
                .withIPAddress("1.0.0.0") // Replace with your desired IPV4 or IPV6 address
                .withLang("en") // Optional: Specify language (en, de, ru, ja, fr, cn, es, cs, it, fa, ko, pt) (en is default)
                .build();
```

#### 2. Perform the Lookup

* Initialize the IP Geolocation Lookup Service and call the required method to get the geolocation data.

``` java
IPGeolocationLookupService ipGeolocationLookupService = IPGeolocationLookupService.getInstance();
IPGeolocation geolocation = ipGeolocationLookupService.getIPCityGeolocation(geolocationParams);
System.out.println(geolocation);
```

* By providing the IP address, you get the following output:

```
IPGeolocation{connectionType='Broadband', location=Location{city=City{name='Maidenhead'}, coordinates=Coordinates{latitude='51.52256', longitude='-0.72431'}, country=Country{capital='London', code2='GB', code3='GBR', codeIoc='GBR', continent=Continent{code='EU', name='Europe'}, currency=Currency{code='GBP', name='Pound Sterling', symbol='£'}, metadata=CountryMetadata{callingCode='+44', languages='en-GB,cy-GB,gd', tld='.uk'}, name='United Kingdom', nameOfficial='United Kingdom of Great Britain and Northern Ireland'}, district=District{name='Royal Borough of Windsor and Maidenhead'}, geonameId='2643186', state=State{code='GB-ENG', name='England'}, zipcode='SL6'}, timeZone='Europe/London'}
```

* You can get the desired fields from the geolocation object by calling the respective getter methods.

```java
System.out.println(geolocation.getLocation()); // location Object has further details (city, country, state, district, zipcode, coordinates, geonameId)
System.out.println(geolocation.getConnectionType());
System.out.println(geolocation.getTimeZone());
```

### 3. Exceptions

The SDK throws the following exceptions:

**1. IllegalArgumentException**:

- Thrown when one or more arguments provided in json configuration file or to a method are invalid. This could occur if
  required parameters are missing or improperly formatted.

**2. IllegalStateException**:
- Thrown when JSON configuration file is not found in the specified directory.

**3. InvalidIPAddressException**:

- Thrown when the provided IP address is invalid, such as when the IP address format does not follow standard IPv4 or
  IPv6 notation.

**4. IPNotFoundException**:

- Thrown when the IP address cannot be found in the database. This could happen if the IP address is not included in the
  loaded geolocation data.

**5. BogonIPException**:

- Thrown when the provided IP address is a bogon IP, which refers to IP addresses that are reserved or not routable on
  the public internet. These addresses cannot be geolocated.

### B. Bulk IP Geolocation Lookup

To get Bulk IP Geolocation, follow these steps:

#### 1. Configure Bulk Geolocation Parameters

Build the bulk geolocation parameters with the necessary options, such as IP addresses and language:

```java
String[] ips = new String[]{"1.0.0.0", "1.0.0.1", "1.0.0.2"};
BulkGeolocationParams bulkGeolocationParams = BulkGeolocationParams.builder()
        .withIPAddresses(ips) // Replace with your desired IPV4, IPV6 addresses or both
        .withLang("en")  // Optional: Specify language (en, de, ru, ja, fr, cn, es, cs, it, fa, ko, pt) (en is default)
        .build();
```

#### 2. Perform the Lookup

* Initialize the IP Geolocation Lookup Service and call the required method to get the geolocation data.

``` java
IPGeolocationLookupService ipGeolocationLookupService = IPGeolocationLookupService.getInstance();
List<IPGeolocation> geolocationList = ipGeolocationLookupService.getIPCityGeolocation(bulkGeolocationParams);
System.out.println(geolocationList);
```

* You can get the desired fields from the geolocation object by calling the respective getter methods.

```java
IPGeolocation ipGeolocation = geolocationList.getFirst();
System.out.println(ipGeolocation.getLocation()); // location Object has further details (city, country, state, district, zipcode, coordinates, geonameId)
System.out.println(ipGeolocation.getConnectionType());
System.out.println(ipGeolocation.getTimeZone());
```

### 3. Exceptions

The SDK throws the following exceptions:

**1. IllegalArgumentException**:

- Thrown when one or more arguments provided in json configuration file or to a method are invalid. This could occur if
  required parameters are missing or improperly formatted.
- Thrown if provided ip array is empty or it's length is greater than 1000.

**2. IllegalStateException**:
- Thrown when JSON configuration file is not found in the specified directory.

**3. InvalidIPAddressException, IPNotFoundException, BogonIPException**:

- Bulk results stay in the same order as the input. If an IP is invalid, reserved, or not found, its place in the list is `null`. For example, three IPs still give three entries. A blank or `null` input throws `IllegalArgumentException`.


