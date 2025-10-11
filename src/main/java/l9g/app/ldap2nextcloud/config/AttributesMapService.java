/*
 * Copyright 2024 Thorsten Ludewig (t.ludewig@gmail.com).
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
package l9g.app.ldap2nextcloud.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Service
@Slf4j
public class AttributesMapService
{
  @Getter
  private final Map<String, String> groups;

  public AttributesMapService(AttributesMapConfig attributesMapConfig)
  {
    this.groups = new HashMap<>();
    groups.putAll(attributesMapConfig.getEmployeeType().getMappingList());
    groups.putAll(attributesMapConfig.getDepartment().getMappingList());
    groups.putAll(attributesMapConfig.getInstitute().getMappingList());
    groups.putAll(attributesMapConfig.getLocality().getMappingList());
    groups.putAll(attributesMapConfig.getAdditional().getMappingList());
  }
}
