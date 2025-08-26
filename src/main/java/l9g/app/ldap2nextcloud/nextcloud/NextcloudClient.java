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
package l9g.app.ldap2nextcloud.nextcloud;

import io.netty.handler.codec.http.HttpResponse;
import java.util.List;
import l9g.app.ldap2nextcloud.model.NextcloudAnonymousUser;
import l9g.app.ldap2nextcloud.model.NextcloudGroup;
import l9g.app.ldap2nextcloud.model.NextcloudOrganization;
import l9g.app.ldap2nextcloud.model.NextcloudRole;
import l9g.app.ldap2nextcloud.model.NextcloudUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
public interface NextcloudClient
{
  @GetExchange("/api/v1/groups")
  public List<NextcloudGroup> groups();

  @GetExchange("/api/v1/organizations")
  public List<NextcloudOrganization> organizations();

  @GetExchange("/api/v1/roles?page={page}&per_page={perPage}")
  public List<NextcloudRole> roles(
    @PathVariable("page") int page,
    @PathVariable("perPage") int perPage
  );

  @GetExchange("/api/v1/users?page={page}&per_page={perPage}")
  public List<NextcloudUser> users(
    @PathVariable("page") int page,
    @PathVariable("perPage") int perPage
  );

  /*
  Query Parameters
  
  - Pagination
    - per_page
    - page
  
  - Sorting Parameter
    - sort_by = {row name}
  
  - Order Direction
    - order_by={directtion}
  
  - Resultset max size
    - limit
   */
  @GetExchange("/api/v1/users/search?query={query}")
  public List<NextcloudUser> usersSearch(
    @PathVariable("query") String query
  );

  @GetExchange("/api/v1/users/search?query={property}:{search}")
  public List<NextcloudUser> usersSearch(
    @PathVariable("property") String property,
    @PathVariable("search") String search
  );

  @GetExchange("/api/v1/users/search?query={property}:{search}&limit={limit}")
  public List<NextcloudUser> usersSearch(
    @PathVariable("property") String property,
    @PathVariable("search") String search,
    @PathVariable("limit") int limit
  );

  @DeleteExchange("/api/v1/users/{id}")
  public HttpResponse usersDelete(@PathVariable(name = "id") int id);

  @PutExchange("/api/v1/users/{id}")
  public NextcloudUser usersUpdate(@PathVariable(name = "id") int id,
    @RequestBody NextcloudUser user);

  @PutExchange("/api/v1/users/{id}")
  public NextcloudUser usersAnonymize(@PathVariable(name = "id") int id,
    @RequestBody NextcloudAnonymousUser user);

  @PostExchange("/api/v1/users")
  public NextcloudUser usersCreate(@RequestBody NextcloudUser user);

  @GetExchange("/api/v1/users/me")
  public NextcloudUser me();

}
