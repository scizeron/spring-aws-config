package com.stfciz.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.util.EC2MetadataUtils;

/**
 * 
 * @author stcizero
 *
 */
public class EnvUtils {
  
  private static Boolean cloudEnvironment = null;

  /**
   * 
   * @return
   */
  public static boolean isCloudEnvironment() {
    if (cloudEnvironment == null) {
      try {
        cloudEnvironment = EC2MetadataUtils.getData("/latest/meta-data/instance-id", 1) != null;
       } catch (AmazonClientException e) {
         cloudEnvironment = false;
       }      
    }
    return cloudEnvironment;
  }
}
