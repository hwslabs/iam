# IamRuby::UserPolicyManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

| Method | HTTP request | Description |
| ------ | ------------ | ----------- |
| [**attach_policies**](UserPolicyManagementApi.md#attach_policies) | **PATCH** /organizations/{organization_id}/users/{user_name}/attach_policies | Attach policies to user |
| [**detach_policies**](UserPolicyManagementApi.md#detach_policies) | **PATCH** /organizations/{organization_id}/users/{user_name}/detach_policies | Detach policies from user |
| [**get_user_policies**](UserPolicyManagementApi.md#get_user_policies) | **GET** /organizations/{organization_id}/users/{user_name}/policies | List all policies associated with user |


## attach_policies

> <BaseSuccessResponse> attach_policies(user_name, organization_id, policy_association_request)

Attach policies to user

Attach policies to user

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserPolicyManagementApi.new
user_name = 'user_name_example' # String | 
organization_id = 'organization_id_example' # String | 
policy_association_request = IamRuby::PolicyAssociationRequest.new({policies: ['policies_example']}) # PolicyAssociationRequest | Payload to attach / detach a policy to a user / resource

begin
  # Attach policies to user
  result = api_instance.attach_policies(user_name, organization_id, policy_association_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserPolicyManagementApi->attach_policies: #{e}"
end
```

#### Using the attach_policies_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<BaseSuccessResponse>, Integer, Hash)> attach_policies_with_http_info(user_name, organization_id, policy_association_request)

```ruby
begin
  # Attach policies to user
  data, status_code, headers = api_instance.attach_policies_with_http_info(user_name, organization_id, policy_association_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <BaseSuccessResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling UserPolicyManagementApi->attach_policies_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **user_name** | **String** |  |  |
| **organization_id** | **String** |  |  |
| **policy_association_request** | [**PolicyAssociationRequest**](PolicyAssociationRequest.md) | Payload to attach / detach a policy to a user / resource |  |

### Return type

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


## detach_policies

> <BaseSuccessResponse> detach_policies(user_name, organization_id, policy_association_request)

Detach policies from user

Detach policies from user

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserPolicyManagementApi.new
user_name = 'user_name_example' # String | 
organization_id = 'organization_id_example' # String | 
policy_association_request = IamRuby::PolicyAssociationRequest.new({policies: ['policies_example']}) # PolicyAssociationRequest | Payload to attach / detach a policy to a user / resource

begin
  # Detach policies from user
  result = api_instance.detach_policies(user_name, organization_id, policy_association_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserPolicyManagementApi->detach_policies: #{e}"
end
```

#### Using the detach_policies_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<BaseSuccessResponse>, Integer, Hash)> detach_policies_with_http_info(user_name, organization_id, policy_association_request)

```ruby
begin
  # Detach policies from user
  data, status_code, headers = api_instance.detach_policies_with_http_info(user_name, organization_id, policy_association_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <BaseSuccessResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling UserPolicyManagementApi->detach_policies_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **user_name** | **String** |  |  |
| **organization_id** | **String** |  |  |
| **policy_association_request** | [**PolicyAssociationRequest**](PolicyAssociationRequest.md) | Payload to attach / detach a policy to a user / resource |  |

### Return type

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


## get_user_policies

> <PolicyPaginatedResponse> get_user_policies(user_name, organization_id, opts)

List all policies associated with user

List all policies associated with user

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserPolicyManagementApi.new
user_name = 'user_name_example' # String | 
organization_id = 'organization_id_example' # String | 
opts = {
  next_token: 'eyJsYXN0SXRlbUlkIjogInN0cmluZyIsICJwYWdlU2l6ZSI6IDEyMywgInNvcnRPcmRlciI6ICJhc2MifQ==', # String | 
  page_size: '10', # String | 
  sort_order: 'asc' # String | 
}

begin
  # List all policies associated with user
  result = api_instance.get_user_policies(user_name, organization_id, opts)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserPolicyManagementApi->get_user_policies: #{e}"
end
```

#### Using the get_user_policies_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<PolicyPaginatedResponse>, Integer, Hash)> get_user_policies_with_http_info(user_name, organization_id, opts)

```ruby
begin
  # List all policies associated with user
  data, status_code, headers = api_instance.get_user_policies_with_http_info(user_name, organization_id, opts)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <PolicyPaginatedResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling UserPolicyManagementApi->get_user_policies_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **user_name** | **String** |  |  |
| **organization_id** | **String** |  |  |
| **next_token** | **String** |  | [optional] |
| **page_size** | **String** |  | [optional] |
| **sort_order** | **String** |  | [optional] |

### Return type

[**PolicyPaginatedResponse**](PolicyPaginatedResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

