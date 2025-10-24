package io.ipgeolocation.services.common.http;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import java.io.File;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.ipgeolocation.services.common.strings.Strings.checkNotEmptyOrNull;

public class HttpRequests {

  public static HttpResponse<File> getAndFileResponse(String uri, String path,
                                                      Map<String, Object> params) throws UnirestException {
    checkNotEmptyOrNull(uri, "Pre-condition violated: URI must not be empty or null.");
    checkNotEmptyOrNull(path, "Pre-condition violated: path must not be empty or null.");
    checkNotNull(params, "Pre-condition violated: query parameters must not be null.");

    return Unirest.get(uri)
            .queryString(params)
            .asFile(path);
  }

}
