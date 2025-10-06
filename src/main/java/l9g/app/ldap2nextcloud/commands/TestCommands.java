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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unboundid.asn1.ASN1GeneralizedTime;
import com.unboundid.ldap.sdk.Entry;
import java.util.List;
import l9g.app.ldap2nextcloud.LogbackConfig;
import l9g.app.ldap2nextcloud.engine.JavaScriptEngine;
import l9g.app.ldap2nextcloud.handler.LdapHandler;
import l9g.app.ldap2nextcloud.model.NextcloudUser;
import l9g.app.ldap2nextcloud.client.NextcloudClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.command.annotation.Command;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Command(group = "Test")
@RequiredArgsConstructor
@Slf4j
public class TestCommands
{
  private final LdapHandler ldapHandler;

  private final LogbackConfig logbackConfig;

  private final NextcloudClient nextcloudClient;

  @Command(alias = "t1", description = "test javascipt file with ldap data")
  public void testJavaScript()
    throws Throwable
  {
    ldapHandler.readLdapEntries(new ASN1GeneralizedTime(0), true);
    ObjectMapper objectMapper = new ObjectMapper();

    try(JavaScriptEngine js = new JavaScriptEngine())
    {
      String[] loginList = ldapHandler.getLdapEntryMap().keySet().toArray(
        String[] :: new);

      int counter = 0;

      for(String login : loginList)
      {
        Entry entry = ldapHandler.getLdapEntryMap().get(login);
        System.out.println("\n" + ( ++ counter) + " : " + entry);
        NextcloudUser user = new NextcloudUser();
        user.setLogin(login);
        js.getValue().executeVoid("test", user, entry);
        System.out.println(objectMapper.writeValueAsString(user));
      }
    }
  }

  @Command(alias = "t2", description = "list all user ids")
  public void testListAllUserIds()
    throws Throwable
  {
    List<String> users = nextcloudClient.listUsers();
    users.forEach(System.out :: println);
    List<String> groups = nextcloudClient.listGroups();
    groups.forEach(System.out :: println);
  }

  @Command(alias = "t3", description = "send test error mail")
  public void testLoggerErrorMail()
    throws Throwable
  {
    logbackConfig.getRootLogger().setLevel(Level.INFO);
    logbackConfig.getL9gLogger().setLevel(Level.INFO);

    log.info("INFO");
    log.debug("DEBUG");
    log.trace("TRACE");
    log.warn("WARN");
    log.error("ERROR");

    //////////////////
    logbackConfig.getL9gLogger().setLevel(Level.DEBUG);

    //////////////////
    log.info("INFO");
    log.debug("DEBUG");
    log.trace("TRACE");
    log.warn("WARN");
    log.error("ERROR");

    logbackConfig.getL9gLogger().setLevel(Level.INFO);

    log.info("INFO");
    log.info(logbackConfig.getNotificationMarker(),
      "This is a test notification INFO mail.");
  }

}
