package com.emc.vipr.sanity.setup

import static com.emc.vipr.sanity.setup.Sanity.*
import com.emc.storageos.model.auth.AuthnCreateParam
import com.emc.storageos.model.tenant.TenantUpdateParam
import com.emc.storageos.model.tenant.UserMappingChanges
import com.emc.storageos.model.tenant.UserMappingParam
import com.emc.storageos.model.tenant.UserMappingAttributeParam
import com.emc.storageos.model.auth.RoleAssignmentChanges
import com.emc.storageos.model.auth.RoleAssignmentEntry
import static org.junit.Assert.*

class SecuritySetup {
    static final String PROVIDER_NAME = "ad configuration"

    static void setupActiveDirectory() {
        println "Setting Active Directory"
        if (client.authnProviders().search().byExactName(PROVIDER_NAME).first()) {
            login(System.getenv("SYSADMIN"), System.getenv("SYSADMIN_PASSWORD"))
            return
        }
        client.authnProviders().create(new AuthnCreateParam(
            mode: "ad",
            serverUrls: System.getenv("AD_AUTHN_URLS"),
            domains: [System.getenv("AD_AUTHN_DOMAIN")] as Set,
            groupWhitelistValues: System.getenv("AD_AUTHN_WHITELIST") as Set,
            label: PROVIDER_NAME,
            searchBase: System.getenv("AD_AUTHN_SEARCH_BASE").join(','),
            searchFilter: System.getenv("AD_AUTHN_SEARCH_FILTER"),
            managerDn: System.getenv("AD_AUTHN_MANAGER_DN").join(','),
            managerPassword: System.getenv("AD_AUTHN_MANAGER_PWD"),
            groupAttribute: [System.getenv("AD_AUTHN_GROUP_ATTR")] as Set
        ))

        client.tenants().update(client.userTenantId, new TenantUpdateParam(
            userMappingChanges: new UserMappingChanges(
                add: [
                    new UserMappingParam(domain: System.getenv("AD_AUTHN_DOMAINS"),
                        attributes: [new UserMappingAttributeParam('ou', ['sanity'])]),
                    new UserMappingParam(domain: System.getenv("AD_AUTHN_DOMAINS"),
                        groups: ["  test Group    "])
                ]
            )
        ))

        client.tenants().updateRoleAssignments(client.userTenantId, new RoleAssignmentChanges(
            add: [new RoleAssignmentEntry(subjectId: System.getenv("SYSADMIN"), roles: ['TENANT_ADMIN', 'TENANT_APPROVER'])]
        ))

        client.vdc().updateRoleAssignments(new RoleAssignmentChanges(
            add: [new RoleAssignmentEntry(subjectId: System.getenv("SYSADMIN"),
                roles: ['SYSTEM_ADMIN', 'SYSTEM_MONITOR', 'SECURITY_ADMIN', 'SYSTEM_AUDITOR']
            )]
        ))

        login(System.getenv("SYSADMIN"), System.getenv("SYSADMIN_PASSWORD"))
    }
}
