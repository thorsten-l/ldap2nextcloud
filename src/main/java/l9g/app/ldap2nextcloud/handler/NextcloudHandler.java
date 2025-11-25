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
import java.util.HashSet;
import java.util.Set;
import l9g.app.ldap2nextcloud.Config;
import l9g.app.ldap2nextcloud.model.NextcloudCreateUser;
import lombok.Getter;
import org.springframework.stereotype.Component;
import l9g.app.ldap2nextcloud.client.NextcloudClient;
import l9g.app.ldap2nextcloud.config.AttributesMapService;
import l9g.app.ldap2nextcloud.model.NextcloudGroup;
import l9g.app.ldap2nextcloud.model.NextcloudUpdateUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

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

  private final AttributesMapService attributesMapService;

  @Value("${nextcloud.quota-default:nolimit}")
  String quotaDefault;

  public void readNextcloudGroups()
  {
    log.debug("readNextcloudGroups");
    nextcloudGroupIds.clear();
    nextcloudGroupIds.addAll(nextcloudClient.listGroups());
    log.info("loaded {} nextcloud groups", nextcloudGroupIds.size());
  }

  public void readNextcloudUsers()
  {
    log.debug("readNextcloudUsers");
    nextcloudUserIds.clear();
    nextcloudUserIds.addAll(nextcloudClient.listUsers());
    log.info("loaded {} nextcloud users", nextcloudUserIds.size());
  }

  public NextcloudUpdateUser findUserById(String userId)
  {
    NextcloudUpdateUser user = null;

    try
    {
      user = nextcloudClient.findUserById(userId);
    }
    catch(Throwable t)
    {
      // 
    }

    return user;
  }

  public synchronized NextcloudCreateUser createUser(NextcloudCreateUser user)
  {
    long startTimestamp = System.currentTimeMillis();
    if(config.isDryRun())
    {
      log.info("CREATE DRY RUN: {}", user);
    }
    else
    {
      log.info("CREATE: {}", user);
      try
      {
        if( ! "nolimit".equals(quotaDefault))
        {
          user.setQuota(quotaDefault);
        }
        nextcloudClient.userCreate(user);
        String userId = user.getUserId();
        updateUser(userId, "displayname", user.getDisplayName());
        updateUser(userId, "address", user.getAddress());
        updateUser(userId, "website", user.getWebsite());
        updateUser(userId, "organisation", user.getOrganisation());
        updateUser(userId, "locale", user.getLocale());
        updateUser(userId, "language", user.getLanguage());
        updateUser(userId, "phone", user.getPhone());
      }
      catch(Throwable t)
      {
        delayedErrorExit("*** CREATE USER FAILED *** " + t.getMessage());
      }
    }
    log.debug("duration : {}ms", System.currentTimeMillis() - startTimestamp);
    return user;
  }

  public synchronized void updateUser(String userId, String key, String value)
  {
    if(userId != null && key != null && value != null)
    {
      if(config.isDryRun())
      {
        log.debug("UPDATE DRY RUN: {},{},{}", userId, key, value);
      }
      else
      {
        try
        {
          log.debug("UPDATE: {}", userId);
          nextcloudClient.userUpdate(userId, key, value);
        }
        catch(Throwable t)
        {
          t.printStackTrace();
          delayedErrorExit("*** UPDATE USER KEY FAILED *** " + t.getMessage());
        }
      }
    }
  }

  public synchronized void updateUser(NextcloudCreateUser user)
  {
    if(user != null)
    {
      if(config.isDryRun())
      {
        log.info("UPDATE DRY RUN: {}", user);
      }
      else
      {
        try
        {
          log.info("UPDATE: {}", user);

          String userId = user.getUserId();
          NextcloudUpdateUser nextcloudUser = nextcloudClient.findUserById(userId);
          if(nextcloudUser != null)
          {
            log.debug("update user : {}", user);
            log.debug("nextcloud user : {}", nextcloudUser);

            updateUser(userId, "displayname", user.getDisplayName());
            updateUser(userId, "address", user.getAddress());
            updateUser(userId, "email", user.getEmail());
            updateUser(userId, "phone", user.getPhone());
            updateUser(userId, "website", user.getWebsite());
            updateUser(userId, "organisation", user.getOrganisation());

            // check groups to remove from
            nextcloudUser.getGroups().forEach(group ->
            {
              if( ! user.getGroups().contains(group) 
              && attributesMapService.getGroups().containsKey(group)) // remove from configurated groups only
              {
                log.debug("REMOVE: user {} from group {}", userId, group);
                nextcloudClient.userRemoveGroup( userId, group );
              }
            });

            // check groups to add to
            user.getGroups().forEach(group -> {
              if ( ! nextcloudUser.getGroups().contains(group))
              {
                log.debug("ADD: user {} to group {}", userId, group);
                nextcloudClient.userAddGroup( userId, group );
              }
            });
          }
        }
        catch(Throwable t)
        {
          t.printStackTrace();
          delayedErrorExit("*** UPDATE USER FAILED *** " + t.getMessage());
        }
      }
    }
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
        nextcloudClient.userDelete(user);
        // nextcloudClient.usersAnonymize(user.getId(), anonymizedUser);
      }
      catch(Throwable t)
      {
        delayedErrorExit("*** DELETE user FAILED *** " + t.getMessage());
      }
    }
  }

  public synchronized NextcloudGroup createGroup(String groupId, String description)
  {
    NextcloudGroup group = new NextcloudGroup(groupId, description);

    if(config.isDryRun())
    {
      log.info("CREATE DRY RUN: {}", group);
    }
    else
    {
      log.info("CREATE: {}", group);
      try
      {
        nextcloudClient.groupCreate(group);
      }
      catch(Throwable t)
      {
        delayedErrorExit("*** CREATE GROUP FAILED *** " + t.getMessage());
      }
    }

    return group;
  }

  public synchronized void updateGroup(String groupId, String displayName)
  {
    if(config.isDryRun())
    {
      log.info("UPDATE DRY RUN: {}", groupId);
    }
    else
    {
      log.info("UPDATE: {}", groupId);
      try
      {
        nextcloudClient.groupUpdateDisplayname(groupId, displayName);
      }
      catch(Throwable t)
      {
        delayedErrorExit("*** UPDATE GROUP FAILED *** " + t.getMessage());
      }
    }
  }
  
  public synchronized void deleteGroup(String groupId)
  {
    if(config.isDryRun())
    {
      log.info("DELETE DRY RUN: {}", groupId);
    }
    else
    {
      log.info("DELETE: {}", groupId);
      try
      {
        nextcloudClient.groupDelete(groupId);
      }
      catch(Throwable t)
      {
        delayedErrorExit("*** UPDATE GROUP FAILED *** " + t.getMessage());
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
  private final Set<String> nextcloudUserIds = new HashSet<>();

  @Getter
  private final Set<String> nextcloudGroupIds = new HashSet<>();

}
