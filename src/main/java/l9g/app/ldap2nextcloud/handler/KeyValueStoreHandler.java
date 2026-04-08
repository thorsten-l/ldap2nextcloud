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

import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.ArrayList;
import l9g.app.ldap2nextcloud.util.KeyValueStore;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.h2.mvstore.MVStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The KeyValueStoreHandler class manages persistent storage for tracking users
 * and groups that are candidates for removal. It uses H2 MVStore to persist
 * "strike counts" for entries that are no longer found in LDAP.
 *
 * @author Thorsten Ludewig, t.ludewig@gmail.com
 */
@Component
@Slf4j
@Getter
public class KeyValueStoreHandler
{
  /**
   * Persistent map storing strike counts for users marked as removable.
   */
  private final KeyValueStore<Integer> removableUsersMap;

  /**
   * Persistent map storing strike counts for groups marked as removable.
   */
  private final KeyValueStore<Integer> removableGroupsMap;

  /**
   * Constructs a new KeyValueStoreHandler and initializes the persistent maps.
   *
   * @param filepath the path to the MVStore file
   */
  public KeyValueStoreHandler(
    @Value("${app.kvstore-filepath:data/store.kv}") String filepath)
  {
    MVStore store = MVStore.open(filepath);
    removableUsersMap = new KeyValueStore<>(store, "removableUsers");
    removableGroupsMap = new KeyValueStore<>(store, "removableGroups");
  }

  public List<String> toDeleteUsers(int threshold)
  {
    log.debug("Listing users with threshold: {}", threshold);
    ArrayList<String> toDelete = new ArrayList<>();

    for(String userId : removableUsersMap.keys())
    {
      Integer count = removableUsersMap.get(userId);
      if(count != null && count >= threshold)
      {
        toDelete.add(userId);
      }
    }

    return toDelete;
  }

  public void incrementUser(String userId)
  {
    removableUsersMap.put(userId,
      removableUsersMap.getOrDefault(userId, 0) + 1);
  }

  public void incrementGroup(String groupId)
  {
    removableGroupsMap.put(groupId,
      removableGroupsMap.getOrDefault(groupId, 0) + 1);
  }

  public List<String> toDeleteGroups(int threshold)
  {
    log.debug("Listing groups with threshold: {}", threshold);
    ArrayList<String> toDelete = new ArrayList<>();

    for(String groupId : removableGroupsMap.keys())
    {
      Integer count = removableGroupsMap.get(groupId);
      if(count != null && count >= threshold)
      {
        toDelete.add(groupId);
      }
    }
    return toDelete;
  }

  /**
   * Returns the maximum strike count currently stored in the removable users map.
   *
   * @return the maximum strike count, or 0 if the map is empty
   */
  public int maxThresholdUsers()
  {
    int max = 0;
    for(Integer count : removableUsersMap.values())
    {
      if(count != null && count > max)
      {
        max = count;
      }
    }
    return max;
  }

  /**
   * Returns the maximum strike count currently stored in the removable groups map.
   *
   * @return the maximum strike count, or 0 if the map is empty
   */
  public int maxThresholdGroups()
  {
    int max = 0;
    for(Integer count : removableGroupsMap.values())
    {
      if(count != null && count > max)
      {
        max = count;
      }
    }
    return max;
  }

  /**
   * Iterates through the removable users map
   * and deletes users from Nextcloud
   * whose strike count has reached or exceeded the specified threshold.
   *
   * @param threshold the strike count threshold for user deletion
   */
  public void cleanUpUsers(int threshold)
  {
    List<String> toDelete = toDeleteUsers(threshold);

    for(String userId : toDelete)
    {
      log.debug("Threshold reached for user '{}' (strikes: {}). Deleting...",
        userId, removableUsersMap.get(userId));
      removableUsersMap.remove(userId);
    }

    if( ! toDelete.isEmpty())
    {
      removableUsersMap.commit();
    }
  }

  /**
   * Iterates through the removable groups map and deletes groups from Nextcloud
   * whose strike count has reached or exceeded the specified threshold.
   *
   * @param threshold the strike count threshold for group deletion
   */
  public void cleanUpGroups(int threshold)
  {
    List<String> toDelete = toDeleteGroups(threshold);

    for(String groupId : toDelete)
    {
      log.debug("Threshold reached for group '{}' (strikes: {}). Deleting...",
        groupId, removableGroupsMap.get(groupId));
      removableGroupsMap.remove(groupId);
    }

    if( ! toDelete.isEmpty())
    {
      removableGroupsMap.commit();
    }
  }

  /**
   * Clears both the removable users and group maps.
   */
  public void wipeAll()
  {
    log.info("Wiping all entries from removable users and groups maps.");
    removableUsersMap.wipeAll();
    removableGroupsMap.wipeAll();
  }

  /**
   * Ensures the persistent stores are properly closed and all data is committed to disk.
   */
  @PreDestroy
  public void close()
  {
    log.debug("Closing KeyValueStoreHandler stores");
    removableUsersMap.close();
    removableGroupsMap.close();
  }

}
