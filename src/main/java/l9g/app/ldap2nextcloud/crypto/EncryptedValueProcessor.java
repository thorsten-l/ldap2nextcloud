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
package l9g.app.ldap2nextcloud.crypto;

import java.lang.reflect.Field;
import l9g.app.ldap2nextcloud.handler.CryptoHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.ReflectionUtils;

/**
 *
 * @author Thorsten Ludewig (t.ludewig@gmail.com)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EncryptedValueProcessor implements BeanPostProcessor
{
  private final Environment environment;

  private final CryptoHandler cryptoHandler = CryptoHandler.getInstance();

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
    throws BeansException
  {
    Field[] fields = bean.getClass().getDeclaredFields();

    for(Field field : fields)
    {
      EncryptedValue annotation = field.getAnnotation(EncryptedValue.class);
      if(annotation != null)
      {
        String environmentName = annotation.value()
          .replaceAll("^\\$\\{", "").replaceAll("}$", "");

        if ( environmentName.charAt(environmentName.length()-1) == ':' )
        {
          environmentName = environmentName.substring(0, environmentName.length()-1);
        }
        
        String[] nameAndDefaultValue = environmentName.split("\\:");
        
        String encryptedValue = (nameAndDefaultValue.length == 1)
          ? environment.getProperty(environmentName)
          : environment.getProperty(nameAndDefaultValue[0]);
        
        String decryptedValue = (encryptedValue != null)
          ? cryptoHandler.decrypt(encryptedValue)
          : (nameAndDefaultValue.length == 2) ? nameAndDefaultValue[1] : null;

        log.trace("{} = {}", annotation.value(), decryptedValue);

        field.setAccessible(true);
        ReflectionUtils.setField(field, bean, decryptedValue);
      }
    }
    return bean;
  }

}
