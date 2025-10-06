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
package l9g.app.ldap2nextcloud.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import l9g.app.ldap2nextcloud.Config;
import l9g.app.ldap2nextcloud.model.NextcloudUser;
import lombok.Getter;
import org.springframework.stereotype.Component;
import l9g.app.ldap2nextcloud.client.NextcloudClient;
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

  public void readNextcloudGroupsAndUsers()
  {
    log.debug("readNextcloudGroupsAndUsers");

    log.debug("readNextcloudGroups");
    nextcloudGroupIdsList.clear();
    nextcloudGroupIdsList.addAll(nextcloudClient.listGroups());
    log.info("loaded {} nextcloud groups", nextcloudGroupIdsList.size());

    log.debug("readNextcloudUsers");
    nextcloudUserIdsList.clear();
    nextcloudUserIdsList.addAll(nextcloudClient.listUsers());
    log.info("loaded {} nextcloud users", nextcloudUserIdsList.size());
  }

  public NextcloudUser createUser(NextcloudUser user)
  {
    if(config.isDryRun())
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
      catch(Throwable t)
      {
        delayedErrorExit("*** CREATE FAILED *** " + t.getMessage());
      }
    }

    return user;
  }

  public NextcloudUser updateUser(NextcloudUser user)
  {
    if(config.isDryRun())
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
      catch(Throwable t)
      {
        delayedErrorExit("*** UPDATE FAILED *** " + t.getMessage());
      }
    }

    return user;
  }

  public void deleteUser(String user)
  {

    if(config.isDryRun())
    {
      log.info("DELETE user DRY RUN: {}", user);
    }
    else
    {

      log.info("DELETE user: {}", user);
      try
      {
        nextcloudClient.usersDelete(user);
        // nextcloudClient.usersAnonymize(user.getId(), anonymizedUser);
      }
      catch(Throwable t)
      {
        delayedErrorExit("*** DELETE user FAILED *** " + t.getMessage());
      }
    }
  }

  private void delayedErrorExit(String message)
  {

    log.error(message);

    try
    {
      Thread.sleep(30000); // 30s delay to send error mail
    }
    catch(InterruptedException ex)
    {
      // do nothing
    }
    System.exit(-1);
  }

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Getter
  private final List<String> nextcloudUserIdsList = new ArrayList<>();

  @Getter
  private final List<String> nextcloudGroupIdsList = new ArrayList<>();

}
