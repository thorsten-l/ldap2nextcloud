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
package l9g.app.ldap2nextcloud.commands;

import ch.qos.logback.classic.Level;
import com.unboundid.asn1.ASN1GeneralizedTime;
import com.unboundid.ldap.sdk.Entry;
import java.util.ArrayList;
import java.util.List;
import l9g.app.ldap2nextcloud.Config;
import l9g.app.ldap2nextcloud.LogbackConfig;
import l9g.app.ldap2nextcloud.TimestampUtil;
import l9g.app.ldap2nextcloud.config.AttributesMapService;
import l9g.app.ldap2nextcloud.engine.JavaScriptEngine;
import l9g.app.ldap2nextcloud.handler.LdapHandler;
import l9g.app.ldap2nextcloud.handler.NextcloudHandler;
import l9g.app.ldap2nextcloud.model.NextcloudCreateUser;
import l9g.app.ldap2nextcloud.model.NextcloudGroup;
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
  private final static Logger LOGGER =
    LoggerFactory.getLogger(ApplicationCommands.class);
  
  private final Config config;
  
  private final LdapHandler ldapHandler;
  
  private final NextcloudHandler nextcloudHandler;
  
  private final LogbackConfig logbackConfig;
  
  private final AttributesMapService attributesMapService;
  
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
  )
    throws Throwable
  {
    // debug = true;

    logbackConfig.getRootLogger().setLevel(Level.INFO);
    logbackConfig.getL9gLogger().setLevel(Level.INFO);
    
    if(debug)
    {
      logbackConfig.getL9gLogger().setLevel(Level.DEBUG);
    }
    
    if(trace)
    {
      debug = true;
      logbackConfig.getRootLogger().setLevel(Level.TRACE);
      logbackConfig.getL9gLogger().setLevel(Level.TRACE);
    }
    
    LOGGER.info("dry-run = '{}', full-sync = '{}', debug = '{}', trace = '{}'", dryRun, fullSync, debug, trace);
    
    config.setDebug(debug);
    config.setDryRun(dryRun);
    
    int updateCounter = 0;
    int createCounter = 0;
    int deleteCounter = 0;
    int ignoreCounter = 0;
    
    createGroupCounter = 0;
    
    TimestampUtil timestampUtil = new TimestampUtil("nextcloud-users");
    
    nextcloudHandler.readNextcloudGroups();
    nextcloudHandler.readNextcloudUsers();
    
    ///////////////////////////////////////////////////////////////////////////
    // DELETE
    LOGGER.info("looking for users to delete");
    ldapHandler.readAllLdapEntryUIDs();
    
    for(String user : nextcloudHandler.getNextcloudUserIds())
    {
      if( ! ldapHandler.getLdapEntryMap().containsKey(user))
      {
        if(protectedUsers.contains(user))
        {
          // IGNORE protected Users
          LOGGER.warn("IGNORE DELETE PROTECTED USER: {}", user);
          ignoreCounter ++;
        }
        else
        {
          // DELETE
          nextcloudHandler.deleteUser(user);
          deleteCounter ++;
        }
      }
    }
    
    ///////////////////////////////////////////////////////////////////////////
    ASN1GeneralizedTime timestamp;
    
    if(fullSync)
    {
      timestamp = new ASN1GeneralizedTime(0l); // 01.01.1970, unix time 0
    }
    else
    {
      timestamp = timestampUtil.getLastSyncTimestamp();
    }
    
    LOGGER.info("looking for users to update or create since last sync ({})", timestamp.getStringRepresentation());
    ldapHandler.readLdapEntries(timestamp, true);
    
    try(JavaScriptEngine js = new JavaScriptEngine())
    {
      int noEntries = ldapHandler.getLdapEntryMap().size();
      int entryCounter = 0;
      
      for(Entry entry : ldapHandler.getLdapEntryMap().values())
      {
        entryCounter ++;
        LOGGER.debug("{}/{}", entryCounter, noEntries);
        
        String userId = entry.getAttributeValue(ldapHandler.getLdapUserId());
        ArrayList<String> groups = new ArrayList<>();
        NextcloudCreateUser updateUser = new NextcloudCreateUser();
        updateUser.setUserId(userId);
        updateUser.setGroups(groups);
        
        if(nextcloudHandler.getNextcloudUserIds().contains(userId))
        {
          js.getValue().executeVoid("update", updateUser, entry);
          checkGroups(updateUser);
          nextcloudHandler.updateUser(updateUser);
          updateCounter ++;
        }
        else
        {
          // CREATE
          js.getValue().executeVoid("create", updateUser, entry);
          checkGroups(updateUser);
          nextcloudHandler.createUser(updateUser);
          createCounter ++;
        }
      }
    }
    
    LOGGER.info("sync done\nSummary:"
      + "\n  updated {} user(s)"
      + "\n  created {} user(s)"
      + "\n  deleted {} user(s)"
      + "\n  ignored {} user(s)"
      + "\n  created {} group(s)",
      updateCounter, createCounter, deleteCounter, ignoreCounter, createGroupCounter);
    
    ///////////////////////////////////////////////////////////////////////////
    if( ! dryRun)
    {
      timestampUtil.writeCurrentTimestamp();
    }
    
    logbackConfig.getRootLogger().setLevel(Level.INFO);
    logbackConfig.getL9gLogger().setLevel(Level.INFO);
  }
  
  private void checkGroups(NextcloudCreateUser user)
    throws Throwable
  {
    if( ! config.isDryRun())
    {
      user.getGroups().forEach(group ->
      {
        if( ! attributesMapService.getGroups().containsKey(group))
        {
          LOGGER.error("ERROR: Group '{}' not found in map!", group);
          throw new RuntimeException("ERROR: Group not found in map! : " + group);
        }
        
        if( ! nextcloudHandler.getNextcloudGroupIds().contains(group))
        {
          String displayName = attributesMapService.getGroups().get(group);
          LOGGER.info("Creating group {}, {} = {}", group, displayName,
            nextcloudHandler.createGroup(group, displayName));
          nextcloudHandler.getNextcloudGroupIds().add(group);
          createGroupCounter ++;
        }
        
      });
    }
  }
  
  @Command(description = "update group displaynames from config to Nextcloud")
  public void updateGroupDisplaynames()
    throws Throwable
  {
    LOGGER.debug("updateGroupDisplaynames");
    nextcloudHandler.readNextcloudGroups();
    int updateCounter = 0;
    LOGGER.debug("{} groups in config", attributesMapService.getGroups().size());
    
    for(String groupId : nextcloudHandler.getNextcloudGroupIds())
    {
      String displayname = attributesMapService.getGroups().get(groupId);
      if(displayname != null)
      {
        LOGGER.debug("Updating group: {}, {}", groupId, displayname);
        nextcloudHandler.updateGroup(groupId, displayname);
        updateCounter ++;
      }
    }
    
    LOGGER.info("{} groups updated", updateCounter);
  }
  
  @Command(description = "update user phonenumbers from LDAP to Nextcloud")
  public void updatePhoneNumbers()
    throws Throwable
  {
    nextcloudHandler.readNextcloudUsers();
    ldapHandler.readLdapEntries(new ASN1GeneralizedTime(0l), true);
    try(JavaScriptEngine js = new JavaScriptEngine())
    {
      for(String userId : nextcloudHandler.getNextcloudUserIds())
      {
        Entry entry = ldapHandler.getLdapEntryMap().get(userId);
        if(entry != null)
        {
          String phone = entry.getAttributeValue("telephoneNumber");
          if(phone != null)
          {
            ArrayList<String> groups = new ArrayList<>();
            NextcloudCreateUser updateUser = new NextcloudCreateUser();
            updateUser.setUserId(userId);
            updateUser.setGroups(groups);
            js.getValue().executeVoid("update", updateUser, entry);
            LOGGER.debug("UPDATE {} phone = {}", userId, updateUser.getPhone());
            nextcloudHandler.updateUser(userId, "phone", updateUser.getPhone());
          }
        }
      }
    }
  }
  
  @Command(description = "Remove all configured groups from Nextcloud")
  public void deleteAllConfiguredGroups()
    throws Throwable
  {
    nextcloudHandler.readNextcloudGroups();
    deleteGroupCounter = 0;
    attributesMapService.getGroups().forEach((groupId, displayName) ->
    {
      
      if(nextcloudHandler.getNextcloudGroupIds().contains(groupId))
      {
        deleteGroupCounter ++;
        LOGGER.debug("DELETE group ({}) : {}, {}", deleteGroupCounter, groupId, displayName);
        nextcloudHandler.deleteGroup(groupId);
      }
      
    });
  }
  
  private int deleteGroupCounter;
  
  private int createGroupCounter;
  
}
