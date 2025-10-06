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
package l9g.app.ldap2nextcloud.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
public class OcsResult
{

  private Ocs ocs;

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString
  public static class Ocs
  {

    private Meta meta;

    private Map<String, Object> data;

  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @ToString
  public static class Meta
  {

    private String status;

    private int statuscode;

    private String message;

    private String totalitems;

    private String itemsperpage;

  }

}
