/**
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.messagebucket;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.cluster.ClusterUser;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucket;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucketException;
import org.sakaiproject.nakamura.api.messagebucket.MessageBucketService;
import org.sakaiproject.nakamura.util.Signature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

/**
 * Buckets from this service are not greatly trusted, and we wont trust them to push data
 * into the server, only receive data from the server.
 */
@Component(immediate = true, metatype = true)
@Service(value=MessageBucketService.class)
public class UntrustedMessageBucketServiceImpl implements MessageBucketService {
  private static final Logger LOG = LoggerFactory
      .getLogger(UntrustedMessageBucketServiceImpl.class);
  private static final int TOKEN_PARTS_LENGTH = 4;
  private static final String DEFAULT_URL_PATTERN = "http://localhost:8080/system/uievent/default?token={3}&server={6}&user={7}";
  private static final String BUCKETURLPATTERN_CONFIG = "bucketurlpattern";
  private String sharedSecret;
  private Map<String, MessageBucket> messageBuckets = new ConcurrentHashMap<String, MessageBucket>();
  private String urlPattern;
  
  @Reference
  private ClusterTrackingService clusterService;
  
  @Activate
  public void activate(Map<String, Object> properties) {
    LOG.debug("activate(Map<String, Object> {})", properties);
    // FIXME investigate what a better shared secret should be?
    sharedSecret = String.valueOf(System.currentTimeMillis()); // not that secure !
    urlPattern = PropertiesUtil.toString(properties.get(BUCKETURLPATTERN_CONFIG), DEFAULT_URL_PATTERN);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.messagebucket.MessageBucketService#getBucket(java.lang.String)
   */
  public MessageBucket getBucket(final String token) throws MessageBucketException {
    LOG.debug("getBucket(String {})", token);
    MessageBucket mb = null;
    if (token != null) {
      String key = getKey(token);
      if (key == null) {
        throw new MessageBucketException("Invalid Token " + token);
      }
      mb = messageBuckets.get(key);
      if (mb == null) {
        mb = new MessageBucketImpl();
        messageBuckets.put(key, mb);
      }
    }
    return mb;
  }

  public String getToken(String userId, String context) throws MessageBucketException {
    LOG.debug("getToken(String {}, String {})", userId, context);
    try {
      String timeStamp = Long.toHexString(System.currentTimeMillis());
      String hmac = Signature.calculateRFC2104HMAC(userId + ";" + timeStamp + ";"
          + context, sharedSecret);
      String token = userId + ";" + timeStamp + ";" + context + ";" + hmac;
      return Base64.encodeBase64URLSafeString(token.getBytes("UTF8"));
    } catch (SignatureException e) {
      throw new MessageBucketException(e.getMessage(), e);
    } catch (UnsupportedEncodingException e) {
      throw new MessageBucketException(e.getMessage(), e);
    }
  }

  public String getKey(String token) throws MessageBucketException {
    LOG.debug("getKey(String {})", token);
    String key = null;
    if (token != null) {
      try {
        String bareToken = new String(Base64.decodeBase64(token), "UTF8");
        String[] parts = StringUtils.split(bareToken, ";", TOKEN_PARTS_LENGTH);
        if (parts.length == TOKEN_PARTS_LENGTH) {
          String hmac = Signature.calculateRFC2104HMAC(parts[0] + ";" + parts[1] + ";"
              + parts[2], sharedSecret);
          if (hmac.equals(parts[3])) {
            key = parts[0] + "-" + parts[2];
          }
        }
      } catch (UnsupportedEncodingException e) {
        throw new MessageBucketException(e.getMessage(), e);
      } catch (SignatureException e) {
        throw new MessageBucketException(e.getMessage(), e);
      }
    }
    return key;
  }

  public String getBucketUrl(HttpServletRequest request, String context) throws MessageBucketException {
    LOG.debug("getBucketUrl(HttpServletRequest request, String {})", context);
    String[] trackingCookies = clusterService.getRequestTrackingCookie(request);
    if ( trackingCookies != null ) {
      for(String trackingCookie : trackingCookies) {
        ClusterUser clusterUser = clusterService.getUser(trackingCookie);
        if (clusterUser != null) {
          String token = getToken(clusterUser.getUser(), context);
          return MessageFormat.format(urlPattern,
              request.getScheme(),
              request.getLocalName(),
              String.valueOf(request.getLocalPort()),
              token,
              token.substring(0,1),
              token.substring(0,2),
              clusterUser.getServerId(),
              request.getRemoteUser());
        }
      }
    }
    throw new MessageBucketException("No Cluster tracking is available");
  }

}
