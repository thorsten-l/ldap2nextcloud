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
package l9g.app.ldap2nextcloud;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Getter
@ToString
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class Config
{


  @Value("${mail.enabled}")
  private boolean mailEnabled;

  @Value("${mail.host.name}")
  private String mailHostname;

  @Value("${mail.host.port}")
  private int mailPort;

  @Value("${mail.host.startTLS}")
  private boolean mailStartTLS;

  @Value("${mail.credentials.uid}")
  private String mailCredentialsUid;

  @Value("${mail.credentials.password}")
  private String mailCredentialsPassword;

  @Value("${mail.subject}")
  private String mailSubject;

  @Value("${mail.from}")
  private String mailFrom;

  @Value("${mail.receipients}")
  private String[] mailReceipients;

  @Setter
  private boolean dryRun;

  @Setter
  private boolean debug;
}
