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

import com.unboundid.asn1.ASN1GeneralizedTime;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.HashMap;
import javax.net.ssl.SSLSocketFactory;
import l9g.app.ldap2nextcloud.crypto.EncryptedValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Component
@RequiredArgsConstructor
public class LdapHandler
{
  private final static Logger LOGGER 
    = LoggerFactory.getLogger(LdapHandler.class);

    @Value("${ldap.host.name}")
  private String ldapHostname;

  @Value("${ldap.host.port}")
  private int ldapPort;

  @Value("${ldap.host.ssl}")
  private boolean ldapSslEnabled;

  @Value("${ldap.base-dn}")
  private String ldapBaseDn;

  @Value("${ldap.bind.dn}")
  private String ldapBindDn;

  @EncryptedValue("${ldap.bind.password}")
  private String ldapBindPassword;

  @Value("${ldap.scope}")
  private String ldapScope;

  @Value("${ldap.filter}")
  private String ldapFilter;

  @Value("${ldap.user.id}")
  private String ldapUserId;

  @Value("${ldap.user.attributes}")
  private String[] ldapUserAttributeNames;

 
  private LDAPConnection getConnection() throws Exception
  {
    LOGGER.debug("host={}", ldapHostname);
    LOGGER.debug("port={}", ldapPort);
    LOGGER.debug("ssl={}", ldapSslEnabled);
    LOGGER.debug("bind dn={}", ldapBindDn);
    LOGGER.trace("bind pw={}", ldapBindPassword);

    LDAPConnection ldapConnection;

    LDAPConnectionOptions options = new LDAPConnectionOptions();
    if (ldapSslEnabled)
    {
      ldapConnection = new LDAPConnection(createSSLSocketFactory(), options,
        ldapHostname, ldapPort,
        ldapBindDn,
        ldapBindPassword);
    }
    else
    {
      ldapConnection = new LDAPConnection(options,
        ldapHostname, ldapPort,
        ldapBindDn,
        ldapBindPassword);
    }
    ldapConnection.setConnectionName(ldapHostname);
    return ldapConnection;
  }

  private SSLSocketFactory createSSLSocketFactory() throws
    GeneralSecurityException
  {
    SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
    return sslUtil.createSSLSocketFactory();
  }

  private void printLdapEntriesMap()
  {
    ldapEntryMap.forEach((k, v) ->
    {
      System.out.println(k + " = " + v);
    });
  }

  public void readLdapEntries(
    ASN1GeneralizedTime lastSyncTimestamp, boolean withAttributes)
    throws Throwable
  {
    ldapEntryMap.clear();

    String filter = new MessageFormat(
      ldapFilter).format(new Object[]
    {
      lastSyncTimestamp.toString()
    });

    LOGGER.debug("filter={}", filter);

    try (LDAPConnection connection = getConnection())
    {
      SearchRequest searchRequest;

      if (withAttributes)
      {
        searchRequest = new SearchRequest(
          ldapBaseDn, SearchScope.SUB, filter,
          ldapUserAttributeNames);
      }
      else
      {
        searchRequest = new SearchRequest(
          ldapBaseDn, SearchScope.SUB, filter,
          ldapUserId);
      }

      int totalSourceEntries = 0;
      ASN1OctetString resumeCookie = null;
      SimplePagedResultsControl responseControl = null;

      // int pagedResultSize = ldapConfig.getPagedResultSize() > 0
      //   ? ldapConfig.getPagedResultSize() : 1000;
      int pagedResultSize = 1000;

      do
      {
        searchRequest.setControls(
          new SimplePagedResultsControl(pagedResultSize, resumeCookie));

        SearchResult sourceSearchResult = connection.search(searchRequest);

        int sourceEntries = sourceSearchResult.getEntryCount();
        totalSourceEntries += sourceEntries;

        if (sourceEntries > 0)
        {
          for (Entry entry : sourceSearchResult.getSearchEntries())
          {
            ldapEntryMap.put(
              entry.getAttributeValue(
                ldapUserId).trim().toLowerCase(), entry);
          }

          responseControl = SimplePagedResultsControl.get(sourceSearchResult);

          if (responseControl != null)
          {
            resumeCookie = responseControl.getCookie();
          }
        }
      }
      while (responseControl != null && responseControl.moreResultsToReturn());

      if (totalSourceEntries == 0)
      {
        LOGGER.info("no ldap entries found");
      }
      else
      {
        LOGGER.
          info("loaded {} ldap entries", totalSourceEntries);
      }
    }
  }

  public void readAllLdapEntryUIDs() throws Throwable
  {
    readLdapEntries(new ASN1GeneralizedTime(0), false);
  }

  public void test() throws Throwable
  {
    LOGGER.debug("basedn={}", ldapBaseDn);
    LOGGER.debug("scope={}", ldapScope);
    LOGGER.debug("user id={}", ldapUserId);

    readAllLdapEntryUIDs();
    printLdapEntriesMap();
    readLdapEntries(new ASN1GeneralizedTime(0), true);
    printLdapEntriesMap();
  }

  @Getter
  private final HashMap<String, Entry> ldapEntryMap = new HashMap<>();
}
