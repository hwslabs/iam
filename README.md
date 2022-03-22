[![Build](https://github.com/hwslabs/iam/actions/workflows/build.yml/badge.svg)](https://github.com/hwslabs/iam/actions/workflows/build.yml)
# com.hypto.iam - Kotlin Server for Hypto IAM

APIs for Hypto IAM Service.

Generated by Swagger Codegen 3.0.32 (2022-02-04T21:21:51.653+05:30[Asia/Kolkata]).

## Requires

* Kotlin 1.6.0
* Gradle 4.3

## Dev setup
1. Install intellij community edition IDE & Docker -
  ```brew cask install intellij-idea-ce``` & ```brew install --cask docker```
2. Install Java 11. (You can easily manage multiple java versions using [SDKMan](https://sdkman.io/usage)) 
3. Clone repo - ```git clone git@github.com:hwslabs/iam.git```
4. Create gradle wrapper script to build - ```gradle wrapper```
5. Run ```./gradlew installGitHooks``` to install git hooks
6. Run ```docker-compose up``` to initialize postgres db
7. Run ```./gradlew build```

### Run local server
1. Run using ```java -jar ./build/libs/hypto-iam-server.jar```

### Local testing
1. Test createOrganization - ```curl --location --request POST 'localhost:8081/organizations' \
   --header 'Content-Type: application/json' \
   --header 'X-Api-Key: hypto-root-secret-key' \
   --data-raw '{
   "name": "<name>",
   "description": "<value>"
   }'```


2. Test other apis - ```curl --location --request GET 'localhost:8081/<api_path>' \
   --header 'Content-Type: application/json' \
   --header 'Authorization: Bearer test-bearer-token'```

## Features/Implementation Notes

* Supports JSON inputs/outputs, File inputs, and Form inputs (see ktor documentation for more info).
* ~Supports collection formats for query parameters: csv, tsv, ssv, pipes.~
* Some Kotlin and Java types are fully qualified to avoid conflicts with types defined in Swagger definitions.

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *https://{environment}.{region}.hypto.com/v1*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*ActionApi* | [**createAction**](docs/ActionApi.md#createaction) | **POST** /resources/{resourceId}/action | Create an action
*ActionApi* | [**deleteAction**](docs/ActionApi.md#deleteaction) | **DELETE** /resources/{resourceId}/action/{id} | Delete an action
*ActionApi* | [**getAction**](docs/ActionApi.md#getaction) | **GET** /resources/{resourceId}/action/{id} | Get an action
*ActionApi* | [**updateAction**](docs/ActionApi.md#updateaction) | **PATCH** /resources/{resourceId}/action/{id} | Update an action
*CredentialApi* | [**createCredential**](docs/CredentialApi.md#createcredential) | **POST** /users/{userId}/credential | Create a Credential
*CredentialApi* | [**deleteCredential**](docs/CredentialApi.md#deletecredential) | **DELETE** /users/{userId}/credential/{id} | Delete a credential
*CredentialApi* | [**getCredential**](docs/CredentialApi.md#getcredential) | **GET** /users/{userId}/credential/{id} | Get a credential
*CredentialApi* | [**updateCredential**](docs/CredentialApi.md#updatecredential) | **PATCH** /users/{userId}/credential/{id} | Update a credential
*OrganizationApi* | [**createOrganization**](docs/OrganizationApi.md#createorganization) | **POST** /organizations | Create an organization
*OrganizationApi* | [**deleteOrganization**](docs/OrganizationApi.md#deleteorganization) | **DELETE** /organization/{id} | Delete an organization
*OrganizationApi* | [**getOrganization**](docs/OrganizationApi.md#getorganization) | **GET** /organization/{id} | Get an organization
*OrganizationApi* | [**updateOrganization**](docs/OrganizationApi.md#updateorganization) | **PATCH** /organization/{id} | Update an organization
*PolicyApi* | [**createPolicy**](docs/PolicyApi.md#createpolicy) | **POST** /policies | Create a policy
*PolicyApi* | [**deletePolicy**](docs/PolicyApi.md#deletepolicy) | **DELETE** /policies/{id} | Delete a policy
*PolicyApi* | [**getPolicy**](docs/PolicyApi.md#getpolicy) | **GET** /policies/{id} | Get a policy
*PolicyApi* | [**getUserPolicies**](docs/PolicyApi.md#getuserpolicies) | **GET** /users/{id}/policies | Get policies of a user
*PolicyApi* | [**updatePolicy**](docs/PolicyApi.md#updatepolicy) | **PATCH** /policies/{id} | Update a policy
*ResourceApi* | [**createResource**](docs/ResourceApi.md#createresource) | **POST** /resources | Create a resource
*ResourceApi* | [**deleteResource**](docs/ResourceApi.md#deleteresource) | **DELETE** /resources/{id} | Delete a resource
*ResourceApi* | [**getResource**](docs/ResourceApi.md#getresource) | **GET** /resources/{id} | Get a resource
*ResourceApi* | [**updateResource**](docs/ResourceApi.md#updateresource) | **PATCH** /resources/{id} | Update a resource
*TokenApi* | [**getToken**](docs/TokenApi.md#gettoken) | **POST** /token | Generate a token
*UsersApi* | [**attachPolicies**](docs/UsersApi.md#attachpolicies) | **PUT** /users/{id}/attach_policies | Attach policies to user
*UsersApi* | [**createUser**](docs/UsersApi.md#createuser) | **POST** /users | Create a user
*UsersApi* | [**deleteUser**](docs/UsersApi.md#deleteuser) | **DELETE** /users/{id} | Delete a User
*UsersApi* | [**detachPolicies**](docs/UsersApi.md#detachpolicies) | **PUT** /users/{id}/detach_policies | Detach policies to user
*UsersApi* | [**getUser**](docs/UsersApi.md#getuser) | **GET** /users/{id} | Get a User
*UsersApi* | [**updateUser**](docs/UsersApi.md#updateuser) | **PATCH** /users/{id} | Update a User

<a name="documentation-for-models"></a>
## Documentation for Models

 - [io.swagger.server.models.Action](docs/Action.md)
 - [io.swagger.server.models.CreateActionRequest](docs/CreateActionRequest.md)
 - [io.swagger.server.models.CreateCredentialRequest](docs/CreateCredentialRequest.md)
 - [io.swagger.server.models.CreateOrganizationRequest](docs/CreateOrganizationRequest.md)
 - [io.swagger.server.models.CreatePolicyRequest](docs/CreatePolicyRequest.md)
 - [io.swagger.server.models.CreateResourceRequest](docs/CreateResourceRequest.md)
 - [io.swagger.server.models.CreateUserRequest](docs/CreateUserRequest.md)
 - [io.swagger.server.models.Credential](docs/Credential.md)
 - [io.swagger.server.models.CredentialWithoutSecret](docs/CredentialWithoutSecret.md)
 - [io.swagger.server.models.ErrorResponse](docs/ErrorResponse.md)
 - [io.swagger.server.models.InlineResponse200](docs/InlineResponse200.md)
 - [io.swagger.server.models.InlineResponse2001](docs/InlineResponse2001.md)
 - [io.swagger.server.models.Organization](docs/Organization.md)
 - [io.swagger.server.models.Policy](docs/Policy.md)
 - [io.swagger.server.models.PolicyAssociationRequest](docs/PolicyAssociationRequest.md)
 - [io.swagger.server.models.PolicyStatement](docs/PolicyStatement.md)
 - [io.swagger.server.models.Resource](docs/Resource.md)
 - [io.swagger.server.models.UpdateActionRequest](docs/UpdateActionRequest.md)
 - [io.swagger.server.models.UpdateCredentialRequest](docs/UpdateCredentialRequest.md)
 - [io.swagger.server.models.UpdateOrganizationRequest](docs/UpdateOrganizationRequest.md)
 - [io.swagger.server.models.UpdatePolicyRequest](docs/UpdatePolicyRequest.md)
 - [io.swagger.server.models.UpdateResourceRequest](docs/UpdateResourceRequest.md)
 - [io.swagger.server.models.UpdateUserRequest](docs/UpdateUserRequest.md)
 - [io.swagger.server.models.User](docs/User.md)

<a name="documentation-for-authorization"></a>
## Documentation for Authorization

<a name="access_token"></a>
### access_token


<a name="refresh_token"></a>
### refresh_token


