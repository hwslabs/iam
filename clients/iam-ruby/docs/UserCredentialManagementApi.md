# IamRuby::UserCredentialManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

| Method | HTTP request | Description |
| ------ | ------------ | ----------- |
| [**create_credential**](UserCredentialManagementApi.md#create_credential) | **POST** /organizations/{organization_id}/users/{user_name}/credentials | Create a new credential for a user |
| [**delete_credential**](UserCredentialManagementApi.md#delete_credential) | **DELETE** /organizations/{organization_id}/users/{user_name}/credentials/{credential_id} | Delete a credential |
| [**get_credential**](UserCredentialManagementApi.md#get_credential) | **GET** /organizations/{organization_id}/users/{user_name}/credentials/{credential_id} | Gets credential for the user |
| [**update_credential**](UserCredentialManagementApi.md#update_credential) | **PATCH** /organizations/{organization_id}/users/{user_name}/credentials/{credential_id} | Update the status of credential |


## create_credential

> <Credential> create_credential(user_name, organization_id, create_credential_request)

Create a new credential for a user

Create a new credential for a user. This API returns the credential's secret key, which will be available only in the response of this API.

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserCredentialManagementApi.new
user_name = 'user_name_example' # String | 
organization_id = 'organization_id_example' # String | 
create_credential_request = IamRuby::CreateCredentialRequest.new # CreateCredentialRequest | Payload to create credential

begin
  # Create a new credential for a user
  result = api_instance.create_credential(user_name, organization_id, create_credential_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserCredentialManagementApi->create_credential: #{e}"
end
```

#### Using the create_credential_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Credential>, Integer, Hash)> create_credential_with_http_info(user_name, organization_id, create_credential_request)

```ruby
begin
  # Create a new credential for a user
  data, status_code, headers = api_instance.create_credential_with_http_info(user_name, organization_id, create_credential_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Credential>
rescue IamRuby::ApiError => e
  puts "Error when calling UserCredentialManagementApi->create_credential_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **user_name** | **String** |  |  |
| **organization_id** | **String** |  |  |
| **create_credential_request** | [**CreateCredentialRequest**](CreateCredentialRequest.md) | Payload to create credential |  |

### Return type

[**Credential**](Credential.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


## delete_credential

> <BaseSuccessResponse> delete_credential(organization_id, user_name, credential_id)

Delete a credential

Delete a credential associated with the user

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserCredentialManagementApi.new
organization_id = 'organization_id_example' # String | 
user_name = 'user_name_example' # String | 
credential_id = 'credential_id_example' # String | 

begin
  # Delete a credential
  result = api_instance.delete_credential(organization_id, user_name, credential_id)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserCredentialManagementApi->delete_credential: #{e}"
end
```

#### Using the delete_credential_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<BaseSuccessResponse>, Integer, Hash)> delete_credential_with_http_info(organization_id, user_name, credential_id)

```ruby
begin
  # Delete a credential
  data, status_code, headers = api_instance.delete_credential_with_http_info(organization_id, user_name, credential_id)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <BaseSuccessResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling UserCredentialManagementApi->delete_credential_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **user_name** | **String** |  |  |
| **credential_id** | **String** |  |  |

### Return type

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## get_credential

> <CredentialWithoutSecret> get_credential(organization_id, user_name, credential_id)

Gets credential for the user

Gets credential for the user, given the credential id

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserCredentialManagementApi.new
organization_id = 'organization_id_example' # String | 
user_name = 'user_name_example' # String | 
credential_id = 'credential_id_example' # String | 

begin
  # Gets credential for the user
  result = api_instance.get_credential(organization_id, user_name, credential_id)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserCredentialManagementApi->get_credential: #{e}"
end
```

#### Using the get_credential_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<CredentialWithoutSecret>, Integer, Hash)> get_credential_with_http_info(organization_id, user_name, credential_id)

```ruby
begin
  # Gets credential for the user
  data, status_code, headers = api_instance.get_credential_with_http_info(organization_id, user_name, credential_id)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <CredentialWithoutSecret>
rescue IamRuby::ApiError => e
  puts "Error when calling UserCredentialManagementApi->get_credential_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **user_name** | **String** |  |  |
| **credential_id** | **String** |  |  |

### Return type

[**CredentialWithoutSecret**](CredentialWithoutSecret.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## update_credential

> <CredentialWithoutSecret> update_credential(organization_id, user_name, credential_id, update_credential_request)

Update the status of credential

Update the status of credential to ACTIVE/INACTIVE. Credentials which are marked INACTIVE cannot be used to fetch short-term tokens.

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserCredentialManagementApi.new
organization_id = 'organization_id_example' # String | 
user_name = 'user_name_example' # String | 
credential_id = 'credential_id_example' # String | 
update_credential_request = IamRuby::UpdateCredentialRequest.new # UpdateCredentialRequest | Payload to update credential

begin
  # Update the status of credential
  result = api_instance.update_credential(organization_id, user_name, credential_id, update_credential_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserCredentialManagementApi->update_credential: #{e}"
end
```

#### Using the update_credential_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<CredentialWithoutSecret>, Integer, Hash)> update_credential_with_http_info(organization_id, user_name, credential_id, update_credential_request)

```ruby
begin
  # Update the status of credential
  data, status_code, headers = api_instance.update_credential_with_http_info(organization_id, user_name, credential_id, update_credential_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <CredentialWithoutSecret>
rescue IamRuby::ApiError => e
  puts "Error when calling UserCredentialManagementApi->update_credential_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **user_name** | **String** |  |  |
| **credential_id** | **String** |  |  |
| **update_credential_request** | [**UpdateCredentialRequest**](UpdateCredentialRequest.md) | Payload to update credential |  |

### Return type

[**CredentialWithoutSecret**](CredentialWithoutSecret.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

