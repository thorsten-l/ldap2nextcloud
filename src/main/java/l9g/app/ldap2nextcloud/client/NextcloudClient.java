/*
 * Copyright 2023 Thorsten Ludewig (t.ludewig@gmail.com).
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

import io.netty.handler.codec.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import l9g.app.ldap2nextcloud.model.NextcloudAnonymousUser;
import l9g.app.ldap2nextcloud.model.NextcloudGroup;
import l9g.app.ldap2nextcloud.model.NextcloudOrganization;
import l9g.app.ldap2nextcloud.model.NextcloudRole;
import l9g.app.ldap2nextcloud.model.NextcloudUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestClient;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Slf4j
@RequiredArgsConstructor
public class NextcloudClient
{
  private final RestClient restClient;

  public List<NextcloudGroup> groups()
  {
    return null;
  }

  public List<NextcloudOrganization> organizations()
  {
    return null;
  }

  public List<NextcloudRole> roles(
    @PathVariable("page") int page,
    @PathVariable("perPage") int perPage
  )
  {
    return null;
  }

  public List<String> listAllUserIds()
  {
    log.debug("listAllUserIds()");

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

  public List<NextcloudUser> users(
    @PathVariable("page") int page,
    @PathVariable("perPage") int perPage
  )
  {
    return null;
  }

  public List<NextcloudUser> usersSearch(
    @PathVariable("query") String query
  )
  {
    return null;
  }

  public List<NextcloudUser> usersSearch(
    @PathVariable("property") String property,
    @PathVariable("search") String search
  )
  {
    return null;
  }

  public List<NextcloudUser> usersSearch(
    @PathVariable("property") String property,
    @PathVariable("search") String search,
    @PathVariable("limit") int limit
  )
  {
    return null;
  }

  public HttpResponse usersDelete(@PathVariable(name = "id") int id)
  {
    return null;
  }

  public NextcloudUser usersUpdate(@PathVariable(name = "id") int id,
    @RequestBody NextcloudUser user)
  {
    return null;
  }

  public NextcloudUser usersAnonymize(@PathVariable(name = "id") int id,
    @RequestBody NextcloudAnonymousUser user)
  {
    return null;
  }

  public NextcloudUser usersCreate(@RequestBody NextcloudUser user)
  {
    return null;
  }

  public NextcloudUser me()
  {
    return null;
  }

}
