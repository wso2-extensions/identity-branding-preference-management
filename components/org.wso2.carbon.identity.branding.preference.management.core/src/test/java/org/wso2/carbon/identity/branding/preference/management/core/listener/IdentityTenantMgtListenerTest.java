/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.branding.preference.management.core.listener;

import org.mockito.MockedStatic;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.branding.preference.management.core.dao.CustomContentPersistentDAO;
import org.wso2.carbon.identity.branding.preference.management.core.dao.impl.CustomContentPersistentFactory;
import org.wso2.carbon.identity.branding.preference.management.core.exception.BrandingPreferenceMgtException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

/**
 * Test class for IdentityTenantMgtListener.
 */
public class IdentityTenantMgtListenerTest {

    @Test(description = "Test onTenantDelete method of IdentityTenantMgtListener.")
    public void testOnTenantDelete() throws Exception {

        try (MockedStatic<CustomContentPersistentFactory> mockedStatic = mockStatic(
                CustomContentPersistentFactory.class);
             MockedStatic<IdentityTenantUtil> tenantUtilMockedStatic = mockStatic(IdentityTenantUtil.class)) {
            // Success case: Verify that the deleteCustomContent method is called with the correct parameters.
            CustomContentPersistentDAO mockedDAO = mock(CustomContentPersistentDAO.class);
            mockedStatic.when(CustomContentPersistentFactory::getCustomContentPersistentDAO)
                    .thenReturn(mockedDAO);
            tenantUtilMockedStatic.when(() -> IdentityTenantUtil.getTenantDomain(1)).thenReturn("tenant-domain");
            IdentityTenantMgtListener identityTenantMgtListener = new IdentityTenantMgtListener();
            identityTenantMgtListener.onTenantDelete(1);
            verify(mockedDAO).deleteCustomContent(null, "tenant-domain");

            // Verify the exception handling path.
            clearInvocations(mockedDAO);
            doThrow(BrandingPreferenceMgtException.class).when(mockedDAO).deleteCustomContent(null, "tenant-domain");
            identityTenantMgtListener.onTenantDelete(1);
        }
    }
}
