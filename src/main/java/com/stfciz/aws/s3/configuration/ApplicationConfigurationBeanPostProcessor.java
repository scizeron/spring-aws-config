package com.stfciz.aws.s3.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.stfciz.aws.EnvUtils;

/**
 * 
 * @author stcizero
 *
 */
public class ApplicationConfigurationBeanPostProcessor implements
    BeanFactoryPostProcessor {

  private static final String AWS_S3_APP_FILE_SYS_PROP = "aws.s3.app.file";

  private static final String AWS_S3_APP_PATH_SYS_PROP = "aws.s3.app.path";

  private static final String AWS_CONF_BUCKET_NAME_SYS_PROP_NAME = "aws.conf.bucket";

  private static final String DEFAULT_CONFIGURATION_FILE = "configuration.yml";

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ApplicationConfigurationBeanPostProcessor.class);

  private Class<?> configurationClass;

  private String bucketName;

  private String path;

  private String file;

  /**
   * 
   * @param bucketName
   * @param path
   * @param file
   */
  public ApplicationConfigurationBeanPostProcessor(Class<?> configurationClass) {
    this.configurationClass = configurationClass;

    this.bucketName = System.getProperty(AWS_CONF_BUCKET_NAME_SYS_PROP_NAME);
    this.path = System.getProperty(AWS_S3_APP_PATH_SYS_PROP);
    this.file = System.getProperty(AWS_S3_APP_FILE_SYS_PROP);

    if (this.file == null) {
      this.file = DEFAULT_CONFIGURATION_FILE;
    }
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    if (!EnvUtils.isCloudEnvironment()) {
      LOGGER.debug("No AWS cloud environment ...");
      return;
    }

    LOGGER.debug("The application is starting on AWS cloud environment");
    AWSCredentials credentials = new SystemPropertiesCredentialsProvider()
        .getCredentials();
    AmazonS3 s3client = new AmazonS3Client(credentials);
    String prefix = String.format("%s/%s", this.path, this.file);
    LOGGER
        .debug(
            "application configuration AWS S3 location : {'bucketName' : '{}', 'path' : '{}', 'file' : '{}'}",
            new Object[] { this.bucketName, this.path, this.file });
    S3ObjectSummary file = S3Objects
        .withPrefix(s3client, this.bucketName, prefix).iterator().next();
    String key = file.getKey();

    try {
      register(new ObjectMapper(new YAMLFactory()).readValue(s3client
          .getObject(this.bucketName, key).getObjectContent(),
          this.configurationClass), beanFactory);
    } catch (Exception e) {
      LOGGER.error("Fail to retrieve {}", prefix, e);
      throw new BeanCreationException("AWS S3 configuration error", e);
    }
  }

  /**
   * 
   * @param instance
   * @throws Exception
   */
  private void register(Object singletonObject,
      ConfigurableListableBeanFactory beanFactory) throws Exception {
    if (LOGGER.isDebugEnabled()) {
      LOGGER
          .debug("===================================================================================");
      LOGGER.debug(" Register \"{}\" from AWS S3 : {}", getBeanId(),
          new ObjectMapper().writeValueAsString(singletonObject));
      LOGGER
          .debug("===================================================================================");
    }
    beanFactory.registerSingleton(getBeanId(), singletonObject);
  }

  /**
   * 
   * @return
   */
  private String getBeanId() {
    String beanId = this.configurationClass.getSimpleName();
    return beanId.substring(0, 1).toLowerCase() + beanId.substring(1);
  }
}