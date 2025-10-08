/*
 * Copyright 2025 Thorsten Ludewig (t.ludewig@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package l9g.app.ldap2nextcloud.client;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import l9g.app.ldap2nextcloud.crypto.EncryptedValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jline.utils.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NextcloudClientFactory
{
  @Value("${nextcloud.base-url:}")
  private String nextcloudBaseUrl;

  @Value("${nextcloud.ocs.user:}")
  private String nextcloudOcsUser;

  @EncryptedValue("${nextcloud.ocs.password:}")
  private String nextcloudOcsPassword;

  @Value("${nextcloud.trust-all-certificates:}")
  boolean nextcloudTrustAllCertificates;

  private final RestClient.Builder restClientBuilder;

  @Bean
  public NextcloudClient createRestNextcloudClient()
    throws SSLException
  {
    RestClient.Builder builder = restClientBuilder;
    
    log.debug("createRestNextcloudClient");
    log.debug("  base-url = {}", nextcloudBaseUrl);
    log.debug("  ocs user = {}", nextcloudOcsUser);
    log.trace("  ocs password = {}", nextcloudOcsPassword);

    if(nextcloudTrustAllCertificates)
    {
      log.warn("TRUSTING ALL CERTIFICATES.");
      HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .version(HttpClient.Version.HTTP_1_1);

      try
      {
        var trustAll = new X509TrustManager()
        {
          @Override
          public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
          {
          }

          @Override
          public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
          {
          }

          @Override
          public java.security.cert.X509Certificate[] getAcceptedIssuers()
          {
            return new java.security.cert.X509Certificate[0];
          }

        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]
        {
          trustAll
        }, new SecureRandom());
        httpClientBuilder.sslContext(sslContext);
      }
      catch(NoSuchAlgorithmException | KeyManagementException e)
      {
        throw new SSLException("Failed to initialize trust-all SSL context", e);
      }

      HttpClient jdkClient = httpClientBuilder.build();
      builder = restClientBuilder.requestFactory(new JdkClientHttpRequestFactory(jdkClient));
    }

    String basicAuthValue = "Basic " + Base64.getEncoder().encodeToString(
      (nextcloudOcsUser + ":" + nextcloudOcsPassword).getBytes(StandardCharsets.UTF_8)
    );

    
    RestClient restClient = builder
      .baseUrl(nextcloudBaseUrl)
      .defaultHeaders(headers ->
      {
        headers.add("Authorization", basicAuthValue);
        headers.add("OCS-APIRequest", "true");
        headers.add("Accept", "application/json");
      })
      .build();

    return new NextcloudClient(restClient);
  }

}
