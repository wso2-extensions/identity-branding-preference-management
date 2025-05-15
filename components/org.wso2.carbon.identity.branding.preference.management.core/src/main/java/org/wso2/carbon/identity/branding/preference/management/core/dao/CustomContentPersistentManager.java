package org.wso2.carbon.identity.branding.preference.management.core.dao;

import org.wso2.carbon.identity.branding.preference.management.core.exception.CustomContentServerException;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomContent;

/**
 * Interface defining the operations for managing custom content persistence.
 * Provides methods to add, update, retrieve, check existence, and delete custom content.
 */
public interface CustomContentPersistentManager {

    /**
     * Update the custom content if exists or add a new custom content if not exists.
     *
     * @param customContent         The Custom Content Object to be persisted
     * @param applicationUuid       Application UUID.
     * @param tenantDomain          Tenant domain.
     * @throws CustomContentServerException If an error occurred while adding or updating the content.
     */
    void addOrUpdateCustomContent(CustomContent customContent, String applicationUuid,
                                         String tenantDomain) throws CustomContentServerException;

    /**
     * Check whether the specified custom content exists.
     *
     * @param applicationUuid       Application UUID.
     * @param tenantDomain          Tenant domain.
     * @return True if the custom content exists, false otherwise.
     * @throws CustomContentServerException If an error occurred while checking the existence.
     */
    boolean isCustomContentExists(String applicationUuid, String tenantDomain) throws CustomContentServerException;

    /**
     * Get specified custom content.
     *
     * @param applicationUuid       Application UUID.
     * @param tenantDomain          Tenant domain.
     * @return Custom Content for a particular APP or ORG
     * @throws CustomContentServerException If an error occurred while retrieving the content.
     */
    CustomContent getCustomContent(String applicationUuid, String tenantDomain) throws CustomContentServerException;

    /**
     * Delete specified custom content.
     *
     * @param applicationUuid       Application UUID.
     * @param tenantDomain          Tenant domain.
     * @throws CustomContentServerException If an error occurred while deleting the content.
     */
    void deleteCustomContent(String applicationUuid, String tenantDomain) throws CustomContentServerException;

}
