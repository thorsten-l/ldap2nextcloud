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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.boolex.OnMarkerEvaluator;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.net.SMTPAppender;
import ch.qos.logback.core.spi.CyclicBufferTracker;
import jakarta.annotation.PostConstruct;
import l9g.app.ldap2nextcloud.crypto.EncryptedValue;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Component
public class LogbackConfig
{
  private final static Logger LOGGER =
    LoggerFactory.getLogger(LogbackConfig.class);

  public final static String SMTP_NOTIFICATION = "SMTP_NOTIFICATION";

  private final static String PATTERN =
    "%date{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger:%line - %msg %n";

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

  @EncryptedValue("${mail.credentials.password}")
  private String mailCredentialsPassword;

  @Value("${mail.subject}")
  private String mailSubject;

  @Value("${mail.from}")
  private String mailFrom;

  @Value("${mail.receipients}")
  private String[] mailReceipients;

  @PostConstruct
  public void initialize()
  {
    LOGGER.debug("initialize - post construct - mail is enabled = {}",
      mailEnabled);
    loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
    rootLogger = loggerContext.getLogger(
      ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    l9gLogger = loggerContext.getLogger("l9g");

    notificationMarker = MarkerFactory.getMarker(SMTP_NOTIFICATION);

    if(mailEnabled)
    {
      PatternLayoutEncoder layoutEncoder = new PatternLayoutEncoder();
      layoutEncoder.setContext(loggerContext);
      layoutEncoder.setPattern(PATTERN);
      layoutEncoder.start();
      //
      smtpAppender = buildSmtpAppender("SMTP", layoutEncoder, null);
      smtpMarkerAppender =
        buildSmtpAppender(SMTP_NOTIFICATION, layoutEncoder,
          SMTP_NOTIFICATION);
      rootLogger.addAppender(smtpAppender);
      rootLogger.addAppender(smtpMarkerAppender);
    }
  }

  @Bean
  @Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
  public LogbackConfig logbackConfigBean()
  {
    LOGGER.debug("logbackConfigBean");
    return this;
  }

  private SMTPAppender buildSmtpAppender(String name,
    PatternLayoutEncoder layoutEncoder, String markerName)
  {
    SMTPAppender appender = new SMTPAppender();

    CyclicBufferTracker bufferTracker = new CyclicBufferTracker();
    bufferTracker.setBufferSize(2);

    appender.setContext(loggerContext);
    appender.setName(name);
    appender.setFrom(mailFrom);

    for(String to : mailReceipients)
    {
      appender.addTo(to);
    }

    appender.setSmtpHost(mailHostname);
    appender.setSmtpPort(mailPort);
    appender.setSTARTTLS(mailStartTLS);
    appender.setSubject(mailSubject);
    appender.setUsername(mailCredentialsUid);
    appender.setPassword(mailCredentialsPassword);
    appender.setLayout(layoutEncoder.getLayout());
    appender.setAsynchronousSending(false);
    appender.setCyclicBufferTracker(bufferTracker);

    if(markerName != null)
    {
      OnMarkerEvaluator evaluator = new OnMarkerEvaluator();
      evaluator.addMarker(markerName);
      appender.setEvaluator(evaluator);
    }

    appender.start();

    return appender;
  }

  private SMTPAppender smtpAppender;

  private SMTPAppender smtpMarkerAppender;

  private LoggerContext loggerContext;

  @Getter
  private ch.qos.logback.classic.Logger rootLogger;

  @Getter
  private ch.qos.logback.classic.Logger l9gLogger;

  @Getter
  private Marker notificationMarker;

}
