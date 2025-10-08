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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import l9g.app.ldap2nextcloud.model.NextcloudGroup;
import l9g.app.ldap2nextcloud.model.NextcloudUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Slf4j
@RequiredArgsConstructor
public class NextcloudClient
{
  private final RestClient restClient;

  public List<String> listUsers()
  {
    log.debug("listUsers()");

    OcsResult result = restClient
      .get()
      .uri(uriBuilder -> uriBuilder.path("/ocs/v1.php/cloud/users").queryParam("format", "json").build())
      .retrieve()
      .body(OcsResult.class);

    if(result != null
      && result.getOcs() != null
      && result.getOcs().getMeta().getStatus() != null
      && result.getOcs().getMeta().getStatuscode() == 100)
    {
      Object users = result.getOcs().getData().get("users");
      if(users != null && users instanceof List)
      {
        return (List)users;
      }
    }
    return new ArrayList<>();
  }

  public List<String> listGroups()
  {
    log.debug("listGroups()");

    OcsResult result = restClient
      .get()
      .uri(uriBuilder -> uriBuilder.path("/ocs/v1.php/cloud/groups").queryParam("format", "json").build())
      .retrieve()
      .body(OcsResult.class);

    if(result != null
      && result.getOcs() != null
      && result.getOcs().getMeta().getStatus() != null
      && result.getOcs().getMeta().getStatuscode() == 100)
    {
      Object groups = result.getOcs().getData().get("groups");
      if(groups != null && groups instanceof List)
      {
        return (List)groups;
      }
    }

    return new ArrayList<>();
  }

  @Retryable(retryFor = HttpClientErrorException.TooManyRequests.class, maxAttempts = 5, backoff =
             @Backoff(delay = 2000, multiplier = 2))
  public int usersDelete(String user)
  {
    int statuscode = -1;

    log.debug("usersDelete({})", user);

    OcsMetaResult result = restClient
      .delete()
      .uri("/ocs/v1.php/cloud/users/{user}?format=json", user)
      .retrieve()
      .body(OcsMetaResult.class);

    if(result != null
      && result.getOcs() != null
      && result.getOcs().getMeta().getStatus() != null)
    {
      statuscode = result.getOcs().getMeta().getStatuscode(); // 100 == ok
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

    OcsMetaResult result = restClient
      .post()
      .uri("/ocs/v2.php/cloud/groups?format=json")
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(group)
      .retrieve()
      .body(OcsMetaResult.class);

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

    Map<String, String> payload = new HashMap<>();
    payload.put("key", "displayname");
    payload.put("value", displayname);

    OcsMetaResult result = restClient
      .put()
      .uri("/ocs/v2.php/cloud/groups/{groupid}", groupId)
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(payload)
      .retrieve()
      .body(OcsMetaResult.class);

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
  public int userUpdate(String userId, String key, String value)
  {
    int statuscode = -1;

    log.debug("userUpdate({},{},{})", userId, key, value);

    Map<String, String> payload = new HashMap<>();
    payload.put("key", key);
    payload.put("value", value);

    OcsMetaResult result = restClient
      .put()
      .uri("/ocs/v2.php/cloud/users/{userId}", userId)
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(payload)
      .retrieve()
      .body(OcsMetaResult.class);

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
  public int userCreate(NextcloudUser user)
  {
    int statuscode = -1;

    log.debug("userCreate({})", user);

    OcsMetaResult result = restClient
      .post()
      .uri("/ocs/v2.php/cloud/users")
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(user)
      .retrieve()
      .body(OcsMetaResult.class);

    if(result != null
      && result.getOcs() != null
      && result.getOcs().getMeta().getStatus() != null)
    {
      statuscode = result.getOcs().getMeta().getStatuscode(); // 200 == ok
    }

    log.debug("  - status code = {}", statuscode);

    return statuscode;
  }

  public NextcloudUser findUser(String userId)
  {
    return null;
  }

}
