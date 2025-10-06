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
package l9g.app.ldap2nextcloud.commands;

import ch.qos.logback.classic.Level;
import java.util.List;
import l9g.app.ldap2nextcloud.Config;
import l9g.app.ldap2nextcloud.LogbackConfig;
import l9g.app.ldap2nextcloud.TimestampUtil;
import l9g.app.ldap2nextcloud.handler.LdapHandler;
import l9g.app.ldap2nextcloud.handler.NextcloudHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Command(group = "Application")
@RequiredArgsConstructor
@Slf4j
public class ApplicationCommands
{
  private final static Logger LOGGER
    = LoggerFactory.getLogger(ApplicationCommands.class);

  private final Config config;

  private final LdapHandler ldapHandler;

  private final NextcloudHandler nextcloudHandler;

  private final LogbackConfig logbackConfig;

  @Value("${sync.protected-users}")
  private List<String> protectedUsers;
  
  @Value("${sync.protected-groups}")
  private List<String> protectedGroups;
  
  @Command(description = "sync users from LDAP to Nextcloud")
  public void sync(
    @Option(longNames = "full-sync", defaultValue = "false") boolean fullSync,
    @Option(longNames = "dry-run", defaultValue = "false") boolean dryRun,
    @Option(longNames = "debug", defaultValue = "false") boolean debug,
    @Option(longNames = "trace", defaultValue = "false") boolean trace
  ) throws Throwable
  {
    // debug = true;
    
    logbackConfig.getRootLogger().setLevel(Level.INFO);
    logbackConfig.getL9gLogger().setLevel(Level.INFO);

    if (debug)
    {
      logbackConfig.getL9gLogger().setLevel(Level.DEBUG);
    }

    if (trace)
    {
      debug = true;
      logbackConfig.getRootLogger().setLevel(Level.TRACE);
      logbackConfig.getL9gLogger().setLevel(Level.TRACE);
    }

    LOGGER.info("dry-run = '{}', full-sync = '{}', debug = '{}', trace = '{}'", dryRun, fullSync, debug, trace);
    LOGGER.info("nextcloud server: '{}'", config.getZammadBaseUrl());
    LOGGER.info("ldap server: 'ldap{}://{}:{}'",
      (config.isLdapSslEnabled())?"s":"",
      config.getLdapHostname(),
      config.getLdapPort());
    
    config.setDebug(debug);
    config.setDryRun(dryRun);

    int updateCounter = 0;
    int createCounter = 0;
    int deleteCounter = 0;
    int ignoreCounter = 0;
    
    TimestampUtil timestampUtil = new TimestampUtil("zammad-users");

    nextcloudHandler.readNextcloudGroupsAndUsers();

    ///////////////////////////////////////////////////////////////////////////
    // DELETE
    LOGGER.info( "looking for users to delete");
    ldapHandler.readAllLdapEntryUIDs();
    
    for (String user : nextcloudHandler.getNextcloudUserIdsList())
    {
      if (!ldapHandler.getLdapEntryMap().containsKey(user))
      {
        if (protectedUsers.contains(user))
        {
          // IGNORE protected Users
          LOGGER.warn("IGNORE DELETE PROTECTED USER: {}", user);
          ignoreCounter++;
        }
        else
        {
          // DELETE
          nextcloudHandler.deleteUser(user);
          deleteCounter++;
        }
      }
    }

/*   
    ///////////////////////////////////////////////////////////////////////////
    ASN1GeneralizedTime timestamp;

    if (fullSync)
    {
      timestamp = new ASN1GeneralizedTime(0l); // 01.01.1970, unix time 0
    }
    else
    {
      timestamp = timestampUtil.getLastSyncTimestamp();
    }

    LOGGER.info( "looking for users to update or create since last sync ({})", timestamp.getStringRepresentation());
    ldapHandler.readLdapEntries(timestamp, true);

    try (JavaScriptEngine js = new JavaScriptEngine())
    {
      int noEntries = ldapHandler.getLdapEntryMap().size();
      int entryCounter = 0;

      for (Entry entry : ldapHandler.getLdapEntryMap().values())
      {
        entryCounter++;
        LOGGER.debug("{}/{}", entryCounter, noEntries);
        String login = entry.getAttributeValue(config.getLdapUserId());
        NextcloudUser zammadUser = nextcloudHandler.getNextcloudUsersMap().get(login);
        ArrayList<String> roles = new ArrayList<>();
        NextcloudUser updateUser = new NextcloudUser();
        updateUser.setLogin(login);
        updateUser.setRoles(roles);

        if (config.getSyncDefaultRoleId() != null)
        {
          String defaultRoleName = 
            nextcloudHandler.getNextcloudRoleMap().get(config.getSyncDefaultRoleId()).getName();
          
          roles.add(defaultRoleName);
        }

        if (zammadUser != null)
        {
          updateUser.setId(zammadUser.getId());

          if (zammadUser.hasAnyRoles(config.getSyncProtectedRoleIds()))
          {
            // IGNORE protected Users
            LOGGER.warn("IGNORE UPDATE PROTECTED USER: {})", zammadUser.toStringShort());
            ignoreCounter++;
          }
          else
          {
            // UPDATE
            if (config.isSyncTagSyncerRolesEnabled()
              && config.isSyncRemoveTaggedRolesBeforUpdateUser())
            {
              zammadUser.getRole_ids().forEach(roleId ->
              {
                String roleName
                  = nextcloudHandler.getNextcloudRoleMap().get(roleId).getName();
                
                if (!roleId.equals(config.getSyncDefaultRoleId())
                  && !roleName.startsWith(config.getSyncRolesTag()))
                {
                  roles.add(roleName);
                }
              });
            }

            js.getValue().executeVoid("update", updateUser, entry, config);
            nextcloudHandler.updateUser(updateUser);
            updateCounter++;
          }
        }
        else
        {
          // CREATE
          js.getValue().executeVoid("create", updateUser, entry, config);
          nextcloudHandler.createUser(updateUser);
          createCounter++;
        }
      }
    }
    */
    LOGGER.info("sync done\nSummary:"
      + "\n  updated {} user(s)"
      + "\n  created {} user(s)"
      + "\n  deleted {} user(s)"
      + "\n  ignored {} user(s)",
      updateCounter, createCounter, deleteCounter, ignoreCounter);
    
    ///////////////////////////////////////////////////////////////////////////
    if (!dryRun)
    {
      timestampUtil.writeCurrentTimestamp();
    }

    logbackConfig.getRootLogger().setLevel(Level.INFO);
    logbackConfig.getL9gLogger().setLevel(Level.INFO);
  }
}
