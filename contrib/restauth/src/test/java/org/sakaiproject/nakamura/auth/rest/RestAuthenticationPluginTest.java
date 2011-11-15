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
package org.sakaiproject.nakamura.auth.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Credentials;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class RestAuthenticationPluginTest {
  RestAuthenticationPlugin plugin;

  @Mock
  RestLoginModulePlugin loginModulePlugin;

  @Mock
  Credentials credentials;

  @Before
  public void setUp() {
    plugin = new RestAuthenticationPlugin(loginModulePlugin);
  }

  @Test
  public void authenticateTrue() throws Exception {
    when(loginModulePlugin.canHandle(isA(Credentials.class))).thenReturn(true);
    assertTrue(plugin.authenticate(credentials));
  }

  @Test
  public void authenticateFalse() throws Exception {
    when(loginModulePlugin.canHandle(isA(Credentials.class))).thenReturn(false);
    assertFalse(plugin.authenticate(credentials));
  }
}