/*
 * Copyright 2026 Thorsten Ludewig (t.ludewig@gmail.com).
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
package l9g.app.ldap2nextcloud.util;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import java.util.Collection;
import java.util.Set;

/**
 * A simple persistent key-value store backed by H2 MVStore.
 * Multiple maps can coexist in a single store file.
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 * @param <V> the type of values stored in this map
 */
public class KeyValueStore<V> implements AutoCloseable
{

  private final MVStore store;

  private final MVMap<String, V> map;

  /**
   * Opens or creates a persistent key-value store.
   *
   * @param filePath the path to the MVStore file
   * @param mapName the name of the map within the store file
   */
  public KeyValueStore(String filePath, String mapName)
  {
    this.store = MVStore.open(filePath);
    this.map = store.openMap(mapName);
  }

  /**
   * Creates a key-value store using an existing MVStore.
   *
   * @param store the existing MVStore
   * @param mapName the name of the map within the store
   */
  public KeyValueStore(MVStore store, String mapName)
  {
    this.store = store;
    this.map = store.openMap(mapName);
  }

  /**
   * Stores a key-value pair.
   *
   * @param key the key
   * @param value the value
   */
  public void put(String key, V value)
  {
    map.put(key, value);
  }

  /**
   * Returns the value for the given key, or {@code null} if not present.
   *
   * @param key the key
   *
   * @return the value, or {@code null}
   */
  public V get(String key)
  {
    return map.get(key);
  }

  /**
   * Returns the value for the given key, or the default value if not present.
   *
   * @param key the key
   * @param defaultValue the fallback value
   *
   * @return the stored value or the default
   */
  public V getOrDefault(String key, V defaultValue)
  {
    return map.getOrDefault(key, defaultValue);
  }

  /**
   * Removes the entry for the given key.
   *
   * @param key the key to remove
   */
  public void remove(String key)
  {
    map.remove(key);
  }

  /**
   * Returns {@code true} if the given key exists in the store.
   *
   * @param key the key to check
   *
   * @return {@code true} if the key is present
   */
  public boolean containsKey(String key)
  {
    return map.containsKey(key);
  }

  /**
   * Returns all keys in this map.
   *
   * @return a set of all keys
   */
  public Set<String> keys()
  {
    return map.keySet();
  }

  /**
   * Returns all values in this map.
   *
   * @return a collection of all values
   */
  public Collection<V> values()
  {
    return map.values();
  }

  /**
   * Returns the number of entries in this map.
   *
   * @return the entry count
   */
  public int size()
  {
    return map.size();
  }

  /**
   * Sets a key to the given value only if it is not already present.
   *
   * @param key the key
   * @param value the value to set if absent
   *
   * @return the existing value, or {@code null} if the key was newly inserted
   */
  public V putIfAbsent(String key, V value)
  {
    return map.putIfAbsent(key, value);
  }

  /**
   * Commits all pending changes to disk immediately.
   */
  public void commit()
  {
    store.commit();
  }
  
  /**
   * Removes all entries from this map and commits the change.
   */
  public void wipeAll()
  {
    map.clear();
    store.commit();
  }

  /**
   * Closes the store and ensures all data is persisted to disk.
   */
  @Override
  public void close()
  {
    store.close();
  }

}
