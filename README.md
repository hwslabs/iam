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

// TODO: Link to organization management APIs

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

// TODO: Link to user management APIs

<a name="credential"></a>
## Credential

IAM requires different types of security credentials depending on how you access IAM. For example, you need a
user name and password to sign in to invoke the /login API and you need secret key to make programmatic calls to IAM.

### Types
- Username / Password (Only for Login API)
- Secret key
- JWT token

// TODO: Link to credential APIs

<a name="resource"></a>
## Resource

A resource is a representation of any entity in your product / service which requires access management

// TODO: Link to Resource management APIs

<a name="action"></a>
## Action

An action represents either an operation that can be performed on a resource or
any other form of interaction that the resource supports.

// TODO: Link to Action management APIs

<a name="policy"></a>
## Policy

You manage access in IAM by creating policies and attaching them to IAM identities

A policy is an object in IAM that, when associated with an entity (iam-user), defines their permissions.
IAM evaluates these policies when a principal, such as a user, makes a request or when /validate API is invoked.
Permissions in the policies determine whether the request is allowed or denied.

IAM provides a [**policy definition language**](README.md#policyDefinitionLanguage) to ease defining policies and permissions.
Internally, policy definitions are stored in IAM as
[**Casbin policy definitions**](https://casbin.org/docs/en/syntax-for-models#policy-definition).

// TODO: Link to Policy management APIs

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
These uniquely identifies a resource or instance of a resources

    hrn:<organization-id>:<account-name>:<resource>:<resource-name>

<a name="actionHrn"></a>
#### Action HRN
These uniquely identifies an action that can be performed on a resource

    hrn:<organization-id>:<account-id>:<resource>$<action-name>

Note: All internal entities are modeled as resources with names prefixed with "iam-" allowing users to create a custom
resource with the same names.

<a name="policyDefinitionLanguage"></a>
## Policy definition language

// TODO: Explain IAM's internal policy definition language: structure, regex support, prefix validation etc.
// TODO: Mention that cross origin is currently not supported

# APIs

## Authentication
// TODOS:
- Root key: explain purpose
- Credentials: explain or link to Credentials under concepts
- JWT

## Authorization
// TODOS:
- JWT
  - Explain structure of JWT, Compression: jjwt lib, MasterKey and it's rotation 
  - Authorization for IAM APIs
  - Authorization for external resources (/generate_token and /validate API)
  - Explicitly mention that the power of jwt is not fully used now, and it's a work is progress. 


## Documentation for API Endpoints

[**Documentation for API Endpoints**](docs/README.md)

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
