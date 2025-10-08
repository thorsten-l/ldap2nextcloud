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
import java.util.List;
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
@Getter
public class AttributesMapService
{
  private final Map<String, String> employeeTypes;

  private final Map<String, String> departments;

  private final Map<String, String> institutes;

  private final Map<String, String> localities;

  private final Map<String, String> additional;

  private final Map<String, String> groups;

  private final String employeeTypeAttributeName;

  private final String departmentAttributeName;

  private final String instituteAttributeName;

  private final String localityAttributeName;

  public AttributesMapService(AttributesMapConfig attributesMapConfig)
  {
    this.employeeTypes = new HashMap<>();
    mappingListToMap(attributesMapConfig.getEmployeeType().getMappingList(),
      employeeTypes);
    this.employeeTypeAttributeName =
      attributesMapConfig.getEmployeeType().getLdapAttributeName();

    this.departments = new HashMap<>();
    mappingListToMap(attributesMapConfig.getDepartment().getMappingList(),
      departments);
    this.departmentAttributeName =
      attributesMapConfig.getDepartment().getLdapAttributeName();

    this.institutes = new HashMap<>();
    mappingListToMap(attributesMapConfig.getInstitute().getMappingList(),
      institutes);
    this.instituteAttributeName =
      attributesMapConfig.getInstitute().getLdapAttributeName();

    this.localities = new HashMap<>();
    mappingListToMap(attributesMapConfig.getLocality().getMappingList(),
      localities);
    this.localityAttributeName =
      attributesMapConfig.getLocality().getLdapAttributeName();

    this.additional = new HashMap<>();
    mappingListToMap(attributesMapConfig.getAdditional().getMappingList(),
      additional);

    this.groups = new HashMap<>();
    groups.putAll(employeeTypes);
    groups.putAll(departments);
    groups.putAll(institutes);
    groups.putAll(localities);
    groups.putAll(additional);
  }

  private void mappingListToMap(List<String> mappingList, Map<String, String> map)
  {
    mappingList.forEach(entry ->
    {
      int i = entry.indexOf(',');
      if(i > 0)
      {
        map.put(entry.substring(0, i).trim(), entry.substring(i + 1).trim());
      }
    });
  }

}
