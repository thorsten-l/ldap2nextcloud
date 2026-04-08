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

import java.util.List;
import l9g.app.ldap2nextcloud.client.NextcloudClient;
import l9g.app.ldap2nextcloud.handler.KeyValueStoreHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

/**
 * Maintenance commands for managing the key-value store and performing
 * cleanup operations on Nextcloud users and groups.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Command(group = "Maintenance")
@RequiredArgsConstructor
@Slf4j
public class MaintenanceCommands
{
  private final KeyValueStoreHandler keyValueStoreHandler;

  private final NextcloudClient nextcloudClient;

  @Command(description = "show max threshold for users")
  public void showMaxThresholdUsers()
  {
    System.out.println("Max threshold users: " + keyValueStoreHandler.maxThresholdUsers());
  }

  @Command(description = "show max threshold for groups")
  public void showMaxThresholdGroups()
  {
    System.out.println("Max threshold groups: " + keyValueStoreHandler.maxThresholdGroups());
  }

  @Command(description = "list users to be deleted")
  public void listToDeleteUsers(
    @Option(description = "threshold", required = true) int threshold)
  {
    List<String> users = keyValueStoreHandler.toDeleteUsers(threshold);
    System.out.println("Users to delete (threshold " + threshold + "):");
    users.forEach(System.out :: println);
    System.out.println("Count: " + users.size());
  }

  @Command(description = "list groups to be deleted")
  public void listToDeleteGroups(
    @Option(description = "threshold", required = true) int threshold)
  {
    List<String> groups = keyValueStoreHandler.toDeleteGroups(threshold);
    System.out.println("Groups to delete (threshold " + threshold + "):");
    groups.forEach(System.out :: println);
    System.out.println("Count: " + groups.size());
  }

  @Command(description = "delete users from Nextcloud")
  public void deleteUsers(
    @Option(description = "threshold", required = true) int threshold)
  {
    if(threshold <= 1)
    {
      log.warn("Threshold must be greater than 1.");
      return;
    }

    List<String> users = keyValueStoreHandler.toDeleteUsers(threshold);
    log.info("Deleting {} users from Nextcloud...", users.size());

    for(String userId : users)
    {
      int status = nextcloudClient.userDelete(userId);
      log.info("Delete user '{}' - status: {}", userId, status);
    }

    keyValueStoreHandler.cleanUpUsers(threshold);
    log.info("Cleanup of users map completed.");
  }

  @Command(description = "delete groups from Nextcloud")
  public void deleteGroups(
    @Option(description = "threshold", required = true) int threshold)
  {
    if(threshold <= 1)
    {
      log.warn("Threshold must be greater than 1.");
      return;
    }

    List<String> groups = keyValueStoreHandler.toDeleteGroups(threshold);
    log.info("Deleting {} groups from Nextcloud...", groups.size());

    for(String groupId : groups)
    {
      int status = nextcloudClient.groupDelete(groupId);
      log.info("Delete group '{}' - status: {}", groupId, status);
    }

    keyValueStoreHandler.cleanUpGroups(threshold);
    log.info("Cleanup of groups map completed.");
  }

  @Command(description = "wipe all entries from the persistent store")
  public void wipeAllEntriesFromStore()
  {
    log.info("Wiping all entries from store...");
    keyValueStoreHandler.wipeAll();
    System.out.println("All entries wiped from store.");
  }

}
