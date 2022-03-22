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

## Organization
// TODO: Explain about organization and point to organization management APIs

## Account
// TODO: Explain about account and mention this is under development

## User
// TODO: Explain about user and point to user management APIs

## Credential
// TODO: Explain about credential and point to credential management APIs

## Resource
// TODO: Explain about Resource and point to Resource management APIs

## Action
// TODO: Explain about Action and point to Action management APIs

## Policy
// TODO: Explain what is Policy, its structure, regex support, etc. and point to Policy management APIs

## Hrn
// TODO: Explain
  - what is Hrn
  - Types of Hrn and their formats

# APIs

## Authentication
- Root key: explain purpose
- Credentials: explain or link to Credentials under concepts
- JWT

## Authorization
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
