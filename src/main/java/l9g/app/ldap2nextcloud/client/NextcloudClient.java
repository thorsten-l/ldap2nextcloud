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

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import l9g.app.ldap2nextcloud.model.NextcloudGroup;
import l9g.app.ldap2nextcloud.model.NextcloudCreateUser;
import l9g.app.ldap2nextcloud.model.NextcloudUpdateUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class NextcloudClient
{
  private final RestTemplate restTemplate;

  @Value("${nextcloud.base-url:}")
  private String nextcloudBaseUrl;

  public List<String> listUsers()
  {
    log.debug("listUsers()");

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs", "v1.php", "cloud", "users")
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    ResponseEntity<OcsResult> response =
      restTemplate.getForEntity(uri, OcsResult.class);

    if(response.getStatusCode() == HttpStatus.OK
      && response.getBody() != null
      && response.getBody().getOcs().getMeta().getStatuscode() == 100)
    {
      Object users = response.getBody().getOcs().getData().get("users");
      if(users != null && users instanceof List)
      {
        return (List)users;
      }
    }
    
    log.error("ERROR: response = {}", response);
    return new ArrayList<>();
  }

  public List<String> listGroups()
  {
    log.debug("listGroups()");

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs", "v1.php", "cloud", "groups")
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    ResponseEntity<OcsResult> response =
      restTemplate.getForEntity(uri, OcsResult.class);

    if(response.getStatusCode() == HttpStatus.OK
      && response.getBody() != null
      && response.getBody().getOcs().getMeta().getStatuscode() == 100)
    {
      Object groups = response.getBody().getOcs().getData().get("groups");
      if(groups != null && groups instanceof List)
      {
        return (List)groups;
      }
    }

    log.error("ERROR: response = {}", response);
    return new ArrayList<>();
  }

  @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 5, backoff =
             @Backoff(delay = 2000, multiplier = 2))
  public int userDelete(String user)
  {
    int statuscode = -1;

    log.debug("usersDelete({})", user);

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs","v1.php","cloud","users", user)
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    ResponseEntity<OcsMetaResult> response = restTemplate.exchange(uri, 
      HttpMethod.DELETE, null, OcsMetaResult.class);

    if(response.getStatusCode() == HttpStatus.OK && response.getBody() != null)
    {
      statuscode = response.getBody().getOcs().getMeta().getStatuscode();
    }

    log.debug("  - status code = {}", statuscode);

    return statuscode;
  }
  
  @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 5, backoff =
             @Backoff(delay = 2000, multiplier = 2))
  public int groupDelete(String groupId)
  {
    int statuscode = -1;

    log.debug("groupDelete({})", groupId);

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs","v1.php","cloud","groups", groupId)
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    ResponseEntity<OcsMetaResult> response = restTemplate.exchange(uri, 
      HttpMethod.DELETE, null, OcsMetaResult.class);

    if(response.getStatusCode() == HttpStatus.OK && response.getBody() != null)
    {
      statuscode = response.getBody().getOcs().getMeta().getStatuscode();
    }

    log.debug("  - status code = {}", statuscode);

    return statuscode;
  }

  @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 5, backoff =
             @Backoff(delay = 2000, multiplier = 2))
  public int groupCreate(NextcloudGroup group)
  {
    int statuscode = -1;

    log.debug("groupCreate({})", group.getDisplayName());

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs","v2.php","cloud","groups")
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    OcsMetaResult result = restTemplate.postForObject(uri, group, OcsMetaResult.class);

    if(result != null
      && result.getOcs() != null
      && result.getOcs().getMeta().getStatus() != null)
    {
      statuscode = result.getOcs().getMeta().getStatuscode(); // 200 == ok
    }

    log.debug("  - status code = {}", statuscode);

    return statuscode;
  }

  @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 5, backoff =
             @Backoff(delay = 2000, multiplier = 2))
  public int groupUpdateDisplayname(String groupId, String displayname)
  {
    int statuscode = -1;

    log.debug("groupUpdate({},{})", groupId, displayname);

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs", "v2.php", "cloud", "groups", groupId)
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> payload = new HashMap<>();
    payload.put("key", "displayname");
    payload.put("value", displayname);

    HttpEntity<Map<String, String>> requestEntity =
      new HttpEntity<>(payload, headers);

    ResponseEntity<OcsMetaResult> response = restTemplate.exchange(uri,
      HttpMethod.PUT, requestEntity, OcsMetaResult.class);

    if(response.getStatusCode() == HttpStatus.OK && response.getBody() != null)
    {
      statuscode = response.getBody().getOcs().getMeta().getStatuscode();
    }

    log.debug("  - status code = {}", statuscode);

    return statuscode;
  }

  @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 5, backoff =
             @Backoff(delay = 2000, multiplier = 2))
  public int userUpdate(String userId, String key, String value)
  {
    int statuscode = -1;

    log.debug("userUpdate({},{},{})", userId, key, value);

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs", "v2.php", "cloud", "users", userId)
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> payload = new HashMap<>();
    payload.put("key", key);
    payload.put("value", value);

    HttpEntity<Map<String, String>> requestEntity =
      new HttpEntity<>(payload, headers);

    ResponseEntity<OcsMetaResult> response = restTemplate.exchange(uri,
      HttpMethod.PUT, requestEntity, OcsMetaResult.class);

    if(response.getStatusCode() == HttpStatus.OK && response.getBody() != null)
    {
      statuscode = response.getBody().getOcs().getMeta().getStatuscode();
    }

    log.debug("  - status code = {}", statuscode);

    return statuscode;
  }

  @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 5, backoff =
             @Backoff(delay = 2000, multiplier = 2))
  public int userCreate(NextcloudCreateUser user)
  {
    int statuscode = -1;

    log.debug("userCreate({})", user);

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs", "v2.php", "cloud", "users")
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<NextcloudCreateUser> requestEntity =
      new HttpEntity<>(user, headers);

    ResponseEntity<OcsMetaResult> response = restTemplate.exchange(
      uri, HttpMethod.POST,
      requestEntity, OcsMetaResult.class);

    if(response.getStatusCode() == HttpStatus.OK && response.getBody() != null)
    {
      statuscode = response.getBody().getOcs().getMeta().getStatuscode();
    }

    log.debug("  - status code = {}", statuscode);

    return statuscode;
  }

  public NextcloudUpdateUser findUserById(String userId)
  {
    log.debug("findUser {}", userId);
    NextcloudUpdateUser user = null;
    int statuscode = -1;

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs/v2.php/cloud/users", userId)
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    ResponseEntity<OcsUpdateUserResult> response =
      restTemplate.getForEntity(uri, OcsUpdateUserResult.class);

    if(response.getStatusCode() == HttpStatus.OK && response.getBody() != null)
    {
      statuscode = response.getBody().getOcs().getMeta().getStatuscode();
      user = response.getBody().getOcs().getData();
    }

    log.debug("  - status code = {}", statuscode);

    return user;
  }

  @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 5, backoff =
             @Backoff(delay = 2000, multiplier = 2))
  public int userRemoveGroup(String userId, String group)
  {
    int statuscode = -1;

    log.debug("userRemoveGroup({},{})", userId, group);

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs", "v2.php", "cloud", "users", userId, "groups")
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> payload = new HashMap<>();
    payload.put("groupid", group);

    HttpEntity<Map<String, String>> requestEntity =
      new HttpEntity<>(payload, headers);

    ResponseEntity<OcsMetaResult> response = restTemplate.exchange(
      uri, HttpMethod.DELETE,
      requestEntity, OcsMetaResult.class);

    if(response.getStatusCode() == HttpStatus.OK && response.getBody() != null)
    {
      statuscode = response.getBody().getOcs().getMeta().getStatuscode();
    }

    log.debug("  - status code = {}", statuscode);

    return statuscode;
  }

  @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 5, backoff =
             @Backoff(delay = 2000, multiplier = 2))
  public int userAddGroup(String userId, String group)
  {
    int statuscode = -1;

    log.debug("userAddGroup({},{})", userId, group);

    URI uri = UriComponentsBuilder
      .fromUriString(nextcloudBaseUrl)
      .pathSegment("ocs", "v2.php", "cloud", "users", userId, "groups")
      .queryParam("format", "json").build().toUri();

    log.debug("uri={}", uri);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, String> payload = new HashMap<>();
    payload.put("groupid", group);

    HttpEntity<Map<String, String>> requestEntity =
      new HttpEntity<>(payload, headers);

    ResponseEntity<OcsMetaResult> response = restTemplate.exchange(
      uri, HttpMethod.POST,
      requestEntity, OcsMetaResult.class);

    if(response.getStatusCode() == HttpStatus.OK && response.getBody() != null)
    {
      statuscode = response.getBody().getOcs().getMeta().getStatuscode();
    }

    log.debug("  - status code = {}", statuscode);

    return statuscode;
  }

}
