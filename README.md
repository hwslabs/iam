# Hypto IAM (Identity and Access Management) Service

[![Build](https://github.com/hwslabs/iam/actions/workflows/build.yml/badge.svg)](https://github.com/hwslabs/iam/actions/workflows/build.yml)
[![Coverage](../badges/development/jacoco.svg)](https://github.com/hwslabs/iam/actions/workflows/build.yml)
[![Branches](../badges/development/branches.svg)](https://github.com/hwslabs/iam/actions/workflows/build.yml)

Provide fine-grained access control to your internal & external services / products / resources.
Hypto IAM service provides APIs to manage the authentication and authorization of your users.

## Requirements
* Docker with docker-compose (v3)
* AWS Account with admin access

## Usage
//TODO: Steps to create aws creds and add to dev.env
* Clone the repository 
* From the folder run `docker-compose up`

## Tech stack
* Kotlin 1.6.10
* Gradle 4.3
* Postgres 14.1

# Concepts

<a name="organization"></a>
## Organization
    hrn:::iam-organization:<organization-id>

Organizations in IAM helps you centrally govern your environment, namespace all your entities
and provide a level of isolation.

Refer [Organization Management APIs](docs/api_reference/Apis/OrganizationManagementApi.md) for more info.

<a name="account"></a>
## Account
    hrn:<organization-id>::iam-account:<account-name>

Note: Account is currently under development and not supported. Hence, must be left out of all HRNs

<a name="user"></a>
## User
    hrn:<organization-id>:<account-id>:iam-user:<user-name>

A user is an entity that you create in IAM to represent the person or application that uses
it to interact with IAM and it's in-build resources or wish to interact with
custom resources modeled in IAM. 

A user in AWS consists of a name, identity information and credentials.

Refer [User Management APIs](docs/api_reference/Apis/UserManagementApi.md) for more info.

<a name="credential"></a>
## Credential

IAM requires different types of security credentials depending on how you access IAM. For example, you need a
username and password to sign in to invoke the `/login` API and you need secret key to make programmatic calls to IAM.

Note that the resource iam-credential refers to the Secret Key credential alone and not other credential types.
More information on this can be found in [Authentication Section](README.md#Authentication). 

### Types
- Username / Password (Only for Login API)
- Secret key
- JWT token

Refer [Credential Management APIs](docs/api_reference/Apis/UserCredentialManagementApi.md) for more info.

<a name="resource"></a>
## Resource

A resource is a representation of any entity in your product / service which requires access management

Refer [Resource Management APIs](docs/api_reference/Apis/ResourceManagementApi.md) for more info.

<a name="action"></a>
## Action

An action represents either an operation that can be performed on a resource or
any other form of interaction that the resource supports.

Refer [Action Management APIs](docs/api_reference/Apis/ResourceActionManagementApi.md) for more info.

<a name="policy"></a>
## Policy

You manage access in IAM by creating policies and attaching them to IAM identities

A policy is an object in IAM that, when associated with an entity (iam-user), defines their permissions.
IAM evaluates these policies when a principal, such as a user, makes a request or when /validate API is invoked.
Permissions in the policies determine whether the request is allowed or denied.

IAM provides a [**policy definition language**](README.md#policy-definition) to ease defining policies and permissions.
Internally, policy definitions are stored in IAM as
[**Casbin policy definitions**](https://casbin.org/docs/en/syntax-for-models#policy-definition).

Refer [Policy Management APIs](docs/api_reference/Apis/PolicyManagementApi.md) for more info.

<a name="hrn"></a>
## HRN

Hypto Resource Names (HRNs) uniquely identify **resources** and **actions** within IAM. We require an HRN when you need to
specify a resource unambiguously across all of IAM, such as in policies and API calls. Every resource and action created
in IAM will have a HRN.

<a name="hrnFormat"></a>
### HRN Format

The following are the general formats for HRNs. The specific formats depend on the resource. To use an
HRN, replace the text within '<' and '>' with the resource-specific information. Be aware that the HRNs for
some resources omit the Organization ID, the account ID, or both the Organization ID and the account ID.

<a name="resourceHrn"></a>
#### Resource HRN
Resource HRN uniquely identifies a resource or instance of a resources.

    hrn:<organization-id>:<account-name>:<resource>:<resource-name>

<a name="actionHrn"></a>
#### Action HRN
Action HRN uniquely identifies an action that can be performed on a resource.

    hrn:<organization-id>:<account-id>:<resource>$<action-name>

Note: All internal entities are modeled as resources with names prefixed with "iam-" allowing users to create a custom
resource with the same names.

<a name="policyDefinition"></a>
## Policy definition

### Structure

```json
{
  "name": "policy_name",
  "statements": [
    {
      "action": "actionHrn or actionHrn regex",
      "resource": "resourceHrn or resourceHrn regex",
      "effect": "allow | deny"
    },
    {
      "action": "actionHrn or actionHrn regex",
      "resource": "resourceHrn or resourceHrn regex",
      "effect": "allow | deny"
    }
  ]
}
```

- A single policy can contain an array of statements.
- Policies can have a maximum of 50 statements.
- Default effect of any permission in IAM is deny.
    i.e, if any permission is not explicitly declared to "allow" via a statement, it is considered to be "deny".
- action or resource or both can contain Hrn regexes. A **Hrn Regex** is nothing but a regex which matches a pattern of HRNs
- It is mandatory for all HRNs and HRN Regexes in a policy to be prefixed with "hrn:<policy's organization-id>" as
     **cross organization access is not supported at the moment**.
> **💡️ DEVELOPER TIP:**  
> These statements are internally converted into casbin documents and stored in database.
([Code Reference](https://github.com/hwslabs/iam/blob/development/src/main/kotlin/com/hypto/iam/server/utils/policy/PolicyBuilder.kt#L12))

# APIs

<a name="Authentication"></a>
## Authentication
IAM being a headless service, exposes APIs which can be accessed by going through any of
the available authentication mechanisms listed below.

### App secret key:
This is a single master key which is the only available authentication mechanism to invoke first level
IAM management APIs, generally used for initial setup of the IAM service.
Which is just Create and Delete organization APIs at the moment.
This Key can be configured in `default_config.json` under the path `app.secret_key` or as environment variable.

### Username / Password:
Every user created in IAM will have username and a password pair. APIs using this mechanism of authentication accepts
the pair over [Basic Authentication](https://en.wikipedia.org/wiki/Basic_access_authentication).

At the moment, this authentication mechanism is used just by the
[Token generation API](docs/api_reference/Apis/UserAuthorizationApi.md#gettoken)


### Credential secret:
Credential secret for a user is a long-lived token which can be created user using
[Create Credential API](docs/api_reference/Apis/UserCredentialManagementApi.md#createcredential).
The credential secret is available for download only when you create it. If you don't save your secret or
if you lose it, you must create a new one. When you disable the secret, you can't use it. After you delete the secret,
it's gone forever and can't be restored, but it can be replaced with a new secret.

### JWT token:
A short-lived JWT token can be generated using [Generate Token API](docs/api_reference/Apis/UserAuthorizationApi.md#gettoken).
This can be used as a replacement to the credential secret. The JWT token contains information regarding the
policies and permissions assigned to the authenticated user at the time of token generation. This can be useful to
improve authentication performance by having an intelligent client which understands the JWT data format and
performs authorization using this information thereby avoiding an API call to IAM.

Things to note:
- Clients are currently unavailable and are being worked upon.
- A possible issue with this approach is, the client will be unaware of any changes to permissions of the user during
     the lifetime of the JWT token. This can be handled by having shorter TTL of JWT tokens or by introducing
     a push mechanism to invalidate JWT token on client side in case, the permissions of user has changed.

For more information on JWT token, see [JWT Docs](docs/docs/JWT.md)

<a name="Authorization"></a>
## Authorization

### For IAM APIs
Upon passing the authentication phase, requests to IAM APIs enter the authorization phase where
the system checks if the requesting user (principal) has all the required permissions to
perform the action on the resource or instance of the resource.

Details on permission(s) required for each IAM API can be found in [API documentation](docs/api_reference/README.md).

### For custom actions
For using IAM as authorization engine when your users access custom resources,
a call has to be made to [Validate API](docs/api_reference/Apis/UserAuthorizationApi.md#validate) with necessary parameters.
IAM service will return the effect ("allow" / "deny") based on the policies that are associated
to the user. You can then decide whether to allow the requesting user perform action on the
requested resource based on the response.


## Documentation for API Endpoints

[**Documentation for API Endpoints**](docs/api_reference/README.md)

# Contribution

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

## Gradle tasks
// TODO: List important / all gradle tasks and their purposes
