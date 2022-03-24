# Documentation for Hypto IAM

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*ActionApi* | [**createAction**](Apis/ActionApi.md#createaction) | **POST** /organizations/{organization_id}/resources/{resource_name}/actions | Create an action
*ActionApi* | [**deleteAction**](Apis/ActionApi.md#deleteaction) | **DELETE** /organizations/{organization_id}/resources/{resource_name}/actions/{id} | Delete an action
*ActionApi* | [**getAction**](Apis/ActionApi.md#getaction) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions/{id} | Get an action
*ActionApi* | [**updateAction**](Apis/ActionApi.md#updateaction) | **PATCH** /organizations/{organization_id}/resources/{resource_name}/actions/{id} | Update an action
*ActionsApi* | [**listActions**](Apis/ActionsApi.md#listactions) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions | List actions
*CredentialApi* | [**createCredential**](Apis/CredentialApi.md#createcredential) | **POST** /organizations/{organization_id}/users/{user_id}/credentials | Create a Credential
*CredentialApi* | [**deleteCredential**](Apis/CredentialApi.md#deletecredential) | **DELETE** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Delete a credential
*CredentialApi* | [**getCredential**](Apis/CredentialApi.md#getcredential) | **GET** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Get a credential
*CredentialApi* | [**updateCredential**](Apis/CredentialApi.md#updatecredential) | **PATCH** /organizations/{organization_id}/users/{user_id}/credentials/{id} | Update a credential
*OrganizationApi* | [**createOrganization**](Apis/OrganizationApi.md#createorganization) | **POST** /organizations | Create an organization
*OrganizationApi* | [**deleteOrganization**](Apis/OrganizationApi.md#deleteorganization) | **DELETE** /organizations/{id} | Delete an organization
*OrganizationApi* | [**getOrganization**](Apis/OrganizationApi.md#getorganization) | **GET** /organizations/{id} | Get an organization
*OrganizationApi* | [**updateOrganization**](Apis/OrganizationApi.md#updateorganization) | **PATCH** /organizations/{id} | Update an organization
*PolicyApi* | [**createPolicy**](Apis/PolicyApi.md#createpolicy) | **POST** /organizations/{organization_id}/policies | Create a policy
*PolicyApi* | [**deletePolicy**](Apis/PolicyApi.md#deletepolicy) | **DELETE** /organizations/{organization_id}/policies/{id} | Delete a policy
*PolicyApi* | [**getPolicy**](Apis/PolicyApi.md#getpolicy) | **GET** /organizations/{organization_id}/policies/{id} | Get a policy
*PolicyApi* | [**getUserPolicies**](Apis/PolicyApi.md#getuserpolicies) | **GET** /organizations/{organization_id}/users/{user_id}/policies | List policies of a user
*PolicyApi* | [**listPolicies**](Apis/PolicyApi.md#listpolicies) | **GET** /organizations/{organization_id}/policies | List policies
*PolicyApi* | [**updatePolicy**](Apis/PolicyApi.md#updatepolicy) | **PATCH** /organizations/{organization_id}/policies/{id} | Update a policy
*ResourceApi* | [**createResource**](Apis/ResourceApi.md#createresource) | **POST** /organizations/{organization_id}/resources | Create a resource
*ResourceApi* | [**deleteResource**](Apis/ResourceApi.md#deleteresource) | **DELETE** /organizations/{organization_id}/resources/{id} | Delete a resource
*ResourceApi* | [**getResource**](Apis/ResourceApi.md#getresource) | **GET** /organizations/{organization_id}/resources/{id} | Get a resource
*ResourceApi* | [**listResources**](Apis/ResourceApi.md#listresources) | **GET** /organizations/{organization_id}/resources | List Resources
*ResourceApi* | [**updateResource**](Apis/ResourceApi.md#updateresource) | **PATCH** /organizations/{organization_id}/resources/{id} | Update a resource
*TokenApi* | [**getToken**](Apis/TokenApi.md#gettoken) | **POST** /token | Generate a token
*UsersApi* | [**attachPolicies**](Apis/UsersApi.md#attachpolicies) | **PUT** /organizations/{organization_id}/users/{id}/attach_policies | Attach policies to user
*UsersApi* | [**createUser**](Apis/UsersApi.md#createuser) | **POST** /organizations/{organization_id}/users | Create a user
*UsersApi* | [**deleteUser**](Apis/UsersApi.md#deleteuser) | **DELETE** /organizations/{organization_id}/users/{id} | Delete a User
*UsersApi* | [**detachPolicies**](Apis/UsersApi.md#detachpolicies) | **PUT** /organizations/{organization_id}/users/{id}/detach_policies | Detach policies to user
*UsersApi* | [**getUser**](Apis/UsersApi.md#getuser) | **GET** /organizations/{organization_id}/users/{id} | Get a User
*UsersApi* | [**listUsers**](Apis/UsersApi.md#listusers) | **GET** /organizations/{organization_id}/users | List users
*UsersApi* | [**updateUser**](Apis/UsersApi.md#updateuser) | **PATCH** /organizations/{organization_id}/users/{id} | Update a User
*ValidateApi* | [**validate**](Apis/ValidateApi.md#validate) | **POST** /validate | Validate an auth request


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

All endpoints do not require authorization.
