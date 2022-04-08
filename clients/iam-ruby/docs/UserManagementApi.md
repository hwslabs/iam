# IamRuby::UserManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

| Method | HTTP request | Description |
| ------ | ------------ | ----------- |
| [**create_user**](UserManagementApi.md#create_user) | **POST** /organizations/{organization_id}/users | Create a user |
| [**delete_user**](UserManagementApi.md#delete_user) | **DELETE** /organizations/{organization_id}/users/{user_name} | Delete a User |
| [**get_user**](UserManagementApi.md#get_user) | **GET** /organizations/{organization_id}/users/{user_name} | Gets a user entity associated with the organization |
| [**list_users**](UserManagementApi.md#list_users) | **GET** /organizations/{organization_id}/users | List users |
| [**update_user**](UserManagementApi.md#update_user) | **PATCH** /organizations/{organization_id}/users/{user_name} | Update a User |


## create_user

> <User> create_user(organization_id, create_user_request)

Create a user

User is an entity which represent a person who is part of the organization or account. This user entity can be created either through user name, password or the user can be federated through an identity provider like Google, Facebook or any SAML 2.0, OIDC identity provider. This is a sign-up api to create a new user in an organization.

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserManagementApi.new
organization_id = 'organization_id_example' # String | 
create_user_request = IamRuby::CreateUserRequest.new({username: 'username_example', password_hash: 'password_hash_example', email: 'email_example', status: 'active'}) # CreateUserRequest | Payload to create user

begin
  # Create a user
  result = api_instance.create_user(organization_id, create_user_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserManagementApi->create_user: #{e}"
end
```

#### Using the create_user_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<User>, Integer, Hash)> create_user_with_http_info(organization_id, create_user_request)

```ruby
begin
  # Create a user
  data, status_code, headers = api_instance.create_user_with_http_info(organization_id, create_user_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <User>
rescue IamRuby::ApiError => e
  puts "Error when calling UserManagementApi->create_user_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **create_user_request** | [**CreateUserRequest**](CreateUserRequest.md) | Payload to create user |  |

### Return type

[**User**](User.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


## delete_user

> <BaseSuccessResponse> delete_user(user_name, organization_id)

Delete a User

Delete a User

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserManagementApi.new
user_name = 'user_name_example' # String | 
organization_id = 'organization_id_example' # String | 

begin
  # Delete a User
  result = api_instance.delete_user(user_name, organization_id)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserManagementApi->delete_user: #{e}"
end
```

#### Using the delete_user_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<BaseSuccessResponse>, Integer, Hash)> delete_user_with_http_info(user_name, organization_id)

```ruby
begin
  # Delete a User
  data, status_code, headers = api_instance.delete_user_with_http_info(user_name, organization_id)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <BaseSuccessResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling UserManagementApi->delete_user_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **user_name** | **String** |  |  |
| **organization_id** | **String** |  |  |

### Return type

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## get_user

> <User> get_user(user_name, organization_id)

Gets a user entity associated with the organization

Get a User

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserManagementApi.new
user_name = 'user_name_example' # String | 
organization_id = 'organization_id_example' # String | 

begin
  # Gets a user entity associated with the organization
  result = api_instance.get_user(user_name, organization_id)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserManagementApi->get_user: #{e}"
end
```

#### Using the get_user_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<User>, Integer, Hash)> get_user_with_http_info(user_name, organization_id)

```ruby
begin
  # Gets a user entity associated with the organization
  data, status_code, headers = api_instance.get_user_with_http_info(user_name, organization_id)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <User>
rescue IamRuby::ApiError => e
  puts "Error when calling UserManagementApi->get_user_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **user_name** | **String** |  |  |
| **organization_id** | **String** |  |  |

### Return type

[**User**](User.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## list_users

> <UserPaginatedResponse> list_users(organization_id, opts)

List users

List users associated with the organization. This is a pagniated api which returns the list of users with a next page token.

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserManagementApi.new
organization_id = 'organization_id_example' # String | 
opts = {
  next_token: 'eyJsYXN0SXRlbUlkIjogInN0cmluZyIsICJwYWdlU2l6ZSI6IDEyMywgInNvcnRPcmRlciI6ICJhc2MifQ==', # String | 
  page_size: '10' # String | 
}

begin
  # List users
  result = api_instance.list_users(organization_id, opts)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserManagementApi->list_users: #{e}"
end
```

#### Using the list_users_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<UserPaginatedResponse>, Integer, Hash)> list_users_with_http_info(organization_id, opts)

```ruby
begin
  # List users
  data, status_code, headers = api_instance.list_users_with_http_info(organization_id, opts)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <UserPaginatedResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling UserManagementApi->list_users_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **next_token** | **String** |  | [optional] |
| **page_size** | **String** |  | [optional] |

### Return type

[**UserPaginatedResponse**](UserPaginatedResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## update_user

> <User> update_user(user_name, organization_id, update_user_request)

Update a User

Update a User

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserManagementApi.new
user_name = 'user_name_example' # String | 
organization_id = 'organization_id_example' # String | 
update_user_request = IamRuby::UpdateUserRequest.new # UpdateUserRequest | Payload to update user

begin
  # Update a User
  result = api_instance.update_user(user_name, organization_id, update_user_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserManagementApi->update_user: #{e}"
end
```

#### Using the update_user_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<User>, Integer, Hash)> update_user_with_http_info(user_name, organization_id, update_user_request)

```ruby
begin
  # Update a User
  data, status_code, headers = api_instance.update_user_with_http_info(user_name, organization_id, update_user_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <User>
rescue IamRuby::ApiError => e
  puts "Error when calling UserManagementApi->update_user_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **user_name** | **String** |  |  |
| **organization_id** | **String** |  |  |
| **update_user_request** | [**UpdateUserRequest**](UpdateUserRequest.md) | Payload to update user |  |

### Return type

[**User**](User.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

