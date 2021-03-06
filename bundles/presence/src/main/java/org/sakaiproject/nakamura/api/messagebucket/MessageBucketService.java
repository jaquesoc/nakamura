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
package org.sakaiproject.nakamura.api.messagebucket;



import javax.servlet.http.HttpServletRequest;


public interface MessageBucketService {


  /**
   * Get the bucket identified by the token.
   * @param token
   * @return Returns a null bucket if the token is null.
   * @throws MessageBucketException
   */
  MessageBucket getBucket(String token) throws MessageBucketException;

  /**
   * Generate a token for the userID in the context. This token may 
   * @param userId
   * @param context
   * @return
   * @throws MessageBucketException
   */
  String getToken(String userId, String context) throws MessageBucketException;

  String getBucketUrl(HttpServletRequest request, String context)
      throws MessageBucketException;

}
