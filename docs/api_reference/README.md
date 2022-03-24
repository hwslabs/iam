# Documentation for Hypto IAM

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*OrganizationManagementApi* | [**createOrganization**](Apis/OrganizationManagementApi.md#createorganization) | **POST** /organizations | Creates an organization.
*OrganizationManagementApi* | [**deleteOrganization**](Apis/OrganizationManagementApi.md#deleteorganization) | **DELETE** /organizations/{id} | Delete an organization
*OrganizationManagementApi* | [**getOrganization**](Apis/OrganizationManagementApi.md#getorganization) | **GET** /organizations/{id} | Get an organization
*OrganizationManagementApi* | [**updateOrganization**](Apis/OrganizationManagementApi.md#updateorganization) | **PATCH** /organizations/{id} | Update an organization
*PolicyManagementApi* | [**createPolicy**](Apis/PolicyManagementApi.md#createpolicy) | **POST** /organizations/{organization_id}/policies | Create a policy
*PolicyManagementApi* | [**deletePolicy**](Apis/PolicyManagementApi.md#deletepolicy) | **DELETE** /organizations/{organization_id}/policies/{id} | Delete a policy
*PolicyManagementApi* | [**getPolicy**](Apis/PolicyManagementApi.md#getpolicy) | **GET** /organizations/{organization_id}/policies/{id} | Get a policy
*PolicyManagementApi* | [**listPolicies**](Apis/PolicyManagementApi.md#listpolicies) | **GET** /organizations/{organization_id}/policies | List policies
*PolicyManagementApi* | [**updatePolicy**](Apis/PolicyManagementApi.md#updatepolicy) | **PATCH** /organizations/{organization_id}/policies/{id} | Update a policy
*ResourceActionManagementApi* | [**createAction**](Apis/ResourceActionManagementApi.md#createaction) | **POST** /organizations/{organization_id}/resources/{resource_name}/actions | Create an action
*ResourceActionManagementApi* | [**deleteAction**](Apis/ResourceActionManagementApi.md#deleteaction) | **DELETE** /organizations/{organization_id}/resources/{resource_name}/actions/{id} | Delete an action
*ResourceActionManagementApi* | [**getAction**](Apis/ResourceActionManagementApi.md#getaction) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions/{id} | Get an action
*ResourceActionManagementApi* | [**listActions**](Apis/ResourceActionManagementApi.md#listactions) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions | List actions
*ResourceActionManagementApi* | [**updateAction**](Apis/ResourceActionManagementApi.md#updateaction) | **PATCH** /organizations/{organization_id}/resources/{resource_name}/actions/{id} | Update an action
*ResourceManagementApi* | [**createResource**](Apis/ResourceManagementApi.md#createresource) | **POST** /organizations/{organization_id}/resources | Create a resource name in an organization.
*ResourceManagementApi* | [**deleteResource**](Apis/ResourceManagementApi.md#deleteresource) | **DELETE** /organizations/{organization_id}/resources/{id} | Delete a resource
*ResourceManagementApi* | [**getResource**](Apis/ResourceManagementApi.md#getresource) | **GET** /organizations/{organization_id}/resources/{id} | Get the resource details
*ResourceManagementApi* | [**listResources**](Apis/ResourceManagementApi.md#listresources) | **GET** /organizations/{organization_id}/resources | List Resources
*ResourceManagementApi* | [**updateResource**](Apis/ResourceManagementApi.md#updateresource) | **PATCH** /organizations/{organization_id}/resources/{id} | Update a resource
*UserAuthorizationApi* | [**getToken**](Apis/UserAuthorizationApi.md#gettoken) | **POST** /token | Generate a token
*UserAuthorizationApi* | [**validate**](Apis/UserAuthorizationApi.md#validate) | **POST** /validate | Validate an auth request
*UserCredentialManagementApi* | [**createCredential**](Apis/UserCredentialManagementApi.md#createcredential) | **POST** /organizations/{organization_id}/users/{user_id}/credentials | Create a new credential for a user
*UserCredentialManagementApi* | [**deleteCredential**](Apis/UserCredentialManagementApi.md#deletecredential) | **DELETE** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Delete a credential
*UserCredentialManagementApi* | [**getCredential**](Apis/UserCredentialManagementApi.md#getcredential) | **GET** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Gets credential for the user
*UserCredentialManagementApi* | [**updateCredential**](Apis/UserCredentialManagementApi.md#updatecredential) | **PATCH** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Update the status of credential
*UserManagementApi* | [**createUser**](Apis/UserManagementApi.md#createuser) | **POST** /organizations/{organization_id}/users | Create a user
*UserManagementApi* | [**deleteUser**](Apis/UserManagementApi.md#deleteuser) | **DELETE** /organizations/{organization_id}/users/{user_id} | Delete a User
*UserManagementApi* | [**getUser**](Apis/UserManagementApi.md#getuser) | **GET** /organizations/{organization_id}/users/{user_id} | Gets a user entity associated with the organization
*UserManagementApi* | [**listUsers**](Apis/UserManagementApi.md#listusers) | **GET** /organizations/{organization_id}/users | List users
*UserManagementApi* | [**updateUser**](Apis/UserManagementApi.md#updateuser) | **PATCH** /organizations/{organization_id}/users/{user_id} | Update a User
*UserPolicyManagementApi* | [**attachPolicies**](Apis/UserPolicyManagementApi.md#attachpolicies) | **PATCH** /organizations/{organization_id}/users/{user_id}/attach_policies | Attach policies to user
*UserPolicyManagementApi* | [**detachPolicies**](Apis/UserPolicyManagementApi.md#detachpolicies) | **PATCH** /organizations/{organization_id}/users/{id}/detach_policies | Detach policies from user
*UserPolicyManagementApi* | [**getUserPolicies**](Apis/UserPolicyManagementApi.md#getuserpolicies) | **GET** /organizations/{organization_id}/users/{user_id}/policies | List all policies associated with user


<a name="documentation-for-models"></a>
## Documentation for Models

 - [Action](./Models/Action.md)
 - [ActionPaginatedResponse](./Models/ActionPaginatedResponse.md)
 - [AdminUser](./Models/AdminUser.md)
 - [BaseSuccessResponse](./Models/BaseSuccessResponse.md)
 - [CreateActionRequest](./Models/CreateActionRequest.md)
 - [CreateCredentialRequest](./Models/CreateCredentialRequest.md)
 - [CreateOrganizationRequest](./Models/CreateOrganizationRequest.md)
 - [CreateOrganizationResponse](./Models/CreateOrganizationResponse.md)
 - [CreatePolicyRequest](./Models/CreatePolicyRequest.md)
 - [CreateResourceRequest](./Models/CreateResourceRequest.md)
 - [CreateUserRequest](./Models/CreateUserRequest.md)
 - [Credential](./Models/Credential.md)
 - [CredentialWithoutSecret](./Models/CredentialWithoutSecret.md)
 - [ErrorResponse](./Models/ErrorResponse.md)
 - [GetUserPoliciesResponse](./Models/GetUserPoliciesResponse.md)
 - [Organization](./Models/Organization.md)
 - [PaginationOptions](./Models/PaginationOptions.md)
 - [Policy](./Models/Policy.md)
 - [PolicyAssociationRequest](./Models/PolicyAssociationRequest.md)
 - [PolicyPaginatedResponse](./Models/PolicyPaginatedResponse.md)
 - [PolicyStatement](./Models/PolicyStatement.md)
 - [Resource](./Models/Resource.md)
 - [ResourceAction](./Models/ResourceAction.md)
 - [ResourceActionEffect](./Models/ResourceActionEffect.md)
 - [ResourcePaginatedResponse](./Models/ResourcePaginatedResponse.md)
 - [TokenResponse](./Models/TokenResponse.md)
 - [UpdateActionRequest](./Models/UpdateActionRequest.md)
 - [UpdateCredentialRequest](./Models/UpdateCredentialRequest.md)
 - [UpdateOrganizationRequest](./Models/UpdateOrganizationRequest.md)
 - [UpdatePolicyRequest](./Models/UpdatePolicyRequest.md)
 - [UpdateResourceRequest](./Models/UpdateResourceRequest.md)
 - [UpdateUserRequest](./Models/UpdateUserRequest.md)
 - [User](./Models/User.md)
 - [UserPaginatedResponse](./Models/UserPaginatedResponse.md)
 - [UserPolicy](./Models/UserPolicy.md)
 - [ValidationRequest](./Models/ValidationRequest.md)
 - [ValidationResponse](./Models/ValidationResponse.md)


<a name="documentation-for-authorization"></a>
## Documentation for Authorization

<a name="apiKeyAuth"></a>
### apiKeyAuth

- **Type**: API key
- **API key parameter name**: X-API-Key
- **Location**: HTTP header

<a name="bearerAuth"></a>
### bearerAuth

- **Type**: HTTP basic authentication

