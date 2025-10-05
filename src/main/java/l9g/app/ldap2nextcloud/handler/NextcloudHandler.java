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
package l9g.app.ldap2nextcloud.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import l9g.app.ldap2nextcloud.Config;
import l9g.app.ldap2nextcloud.model.NextcloudAnonymousUser;
import l9g.app.ldap2nextcloud.model.NextcloudRole;
import l9g.app.ldap2nextcloud.model.NextcloudUser;
import lombok.Getter;
import org.springframework.stereotype.Component;
import l9g.app.ldap2nextcloud.nextcloud.NextcloudClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NextcloudHandler
{
  private final Config config;

  private final NextcloudClient nextcloudClient;

  public void readNextcloudRolesAndUsers()
  { 
    log.debug("readNextcloudRoles {}", config.getZammadBaseUrl());
   
    nextcloudRoleList = new ArrayList<>();
    List<NextcloudRole> rolesResult;
    int page = 1;
    while((rolesResult = nextcloudClient.roles(page,100)) != null 
      && !rolesResult.isEmpty())
    {
      nextcloudRoleList.addAll(rolesResult);
      page++;
    }
    nextcloudRoleMap.clear();
    nextcloudRoleList.forEach(role -> nextcloudRoleMap.put(role.getId(), role));
    log.info("loaded {} nextcloud roles", nextcloudRoleList.size());
    
    log.debug("readNextcloudUsers");
    nextcloudUsersList = new ArrayList<>();
    List<NextcloudUser> usersResult;
    page = 1;
    while((usersResult = nextcloudClient.users(page, 100)) != null
      && !usersResult.isEmpty())
    {
      nextcloudUsersList.addAll(usersResult);
      page++;
    }
    
    nextcloudUsersMap.clear();
    nextcloudUsersList.forEach(user -> nextcloudUsersMap.put(user.getLogin(), user));

    log.info("loaded {} nextcloud users", nextcloudUsersList.size());
  }

  public NextcloudUser createUser(NextcloudUser user)
  {
    if (config.isDryRun())
    {
      log.info("CREATE DRY RUN: {}", user);
    }
    else
    {
      log.info("CREATE: {}", user);
      try
      {
        user = nextcloudClient.usersCreate(user);
      }
      catch (Throwable t)
      {
        delayedErrorExit("*** CREATE FAILED *** " + t.getMessage());
      }
    }

    return user;
  }

  public NextcloudUser updateUser(NextcloudUser user)
  {
    if (config.isDryRun())
    {
      log.info("UPDATE DRY RUN: {}", user);
    }
    else
    {
      try
      {
        log.info("UPDATE: {}", objectMapper.writeValueAsString(user));
        user = nextcloudClient.usersUpdate(user.getId(), user);
      }
      catch (Throwable t)
      {
        delayedErrorExit("*** UPDATE FAILED *** " + t.getMessage());
      }
    }

    return user;
  }

  public void deleteUser(NextcloudUser user)
  {
    NextcloudAnonymousUser anonymizedUser = new NextcloudAnonymousUser(user.getLogin());
    
    if (config.isDryRun())
    {
      log.info("DELETE (anonymize) DRY RUN: {}", anonymizedUser);
    }
    else
    {
      
      
      log.info("DELETE (anonymize): {}", anonymizedUser);
      try
      {
        // nextcloudClient.usersDelete(user.getId());
        nextcloudClient.usersAnonymize(user.getId(), anonymizedUser);
      }
      catch (Throwable t)
      {
        delayedErrorExit("*** DELETE (anonymize) FAILED *** " + t.getMessage());
      }
    }
  }
  
  private void delayedErrorExit( String message )
  {
    
    log.error(message);
    
    try
    {
      Thread.sleep(30000); // 30s delay to send error mail
    }
    catch (InterruptedException ex)
    {
      // do nothing
    }
    System.exit(-1);
  }

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Getter
  private final Map<String, NextcloudUser> nextcloudUsersMap = new HashMap<>();

  @Getter
  private List<NextcloudUser> nextcloudUsersList;
  
  @Getter
  private final Map<Integer, NextcloudRole> nextcloudRoleMap = new HashMap<>();

  @Getter
  private List<NextcloudRole> nextcloudRoleList;
}
