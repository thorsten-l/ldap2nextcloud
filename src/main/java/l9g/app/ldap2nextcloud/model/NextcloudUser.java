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
package l9g.app.ldap2nextcloud.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@ToString
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class NextcloudUser
{
  @JsonProperty("userid")
  private String userId;

  // private String storageLocation;
  // private Long firstLoginTimestamp;
  // private Long lastLoginTimestamp;
  // private Long lastLogin;
  // private String backend;
  // private List<String> subadmin;
  // private Quota quota;
  // private String manager;
  private String email;

  //@JsonProperty("additional_mail")
  //private List<String> additionalMail;
  
  @JsonProperty("displayname")
  private String displayName;

  private String phone;

  private String address;

  private String website;

  //private String twitter;
  //private String fediverse;
  private String organisation;
  
  private String quota;

  //private String role;
  //private String headline;
  //private String biography;
  //@JsonProperty("profile_enabled")
  //private String profileEnabled;
  //private String pronouns;
  private List<String> groups;

  private String language;

  private String locale;

  //@JsonProperty("notify_email")
  //private String notifyEmail;
  //private BackendCapabilities backendCapabilities;
  @ToString
  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public static class Quota
  {
    private long free;

    private long used;

    private long total;

    private double relative;

    private long quota;

  }

  @ToString
  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  public static class BackendCapabilities
  {
    private boolean setDisplayName;

    private boolean setPassword;

  }

}
