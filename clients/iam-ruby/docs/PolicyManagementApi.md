# IamRuby::PolicyManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

| Method | HTTP request | Description |
| ------ | ------------ | ----------- |
| [**create_policy**](PolicyManagementApi.md#create_policy) | **POST** /organizations/{organization_id}/policies | Create a policy |
| [**delete_policy**](PolicyManagementApi.md#delete_policy) | **DELETE** /organizations/{organization_id}/policies/{policy_name} | Delete a policy |
| [**get_policy**](PolicyManagementApi.md#get_policy) | **GET** /organizations/{organization_id}/policies/{policy_name} | Get a policy |
| [**list_policies**](PolicyManagementApi.md#list_policies) | **GET** /organizations/{organization_id}/policies | List policies |
| [**update_policy**](PolicyManagementApi.md#update_policy) | **PATCH** /organizations/{organization_id}/policies/{policy_name} | Update a policy |


## create_policy

> <Policy> create_policy(organization_id, create_policy_request)

Create a policy

Create a policy

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::PolicyManagementApi.new
organization_id = 'organization_id_example' # String | 
create_policy_request = IamRuby::CreatePolicyRequest.new({name: 'name_example', statements: [IamRuby::PolicyStatement.new({resource: 'resource_example', action: 'action_example', effect: 'allow'})]}) # CreatePolicyRequest | Payload to create policy

begin
  # Create a policy
  result = api_instance.create_policy(organization_id, create_policy_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling PolicyManagementApi->create_policy: #{e}"
end
```

#### Using the create_policy_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Policy>, Integer, Hash)> create_policy_with_http_info(organization_id, create_policy_request)

```ruby
begin
  # Create a policy
  data, status_code, headers = api_instance.create_policy_with_http_info(organization_id, create_policy_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Policy>
rescue IamRuby::ApiError => e
  puts "Error when calling PolicyManagementApi->create_policy_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **create_policy_request** | [**CreatePolicyRequest**](CreatePolicyRequest.md) | Payload to create policy |  |

### Return type

[**Policy**](Policy.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


## delete_policy

> <BaseSuccessResponse> delete_policy(organization_id, policy_name)

Delete a policy

Delete a policy

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::PolicyManagementApi.new
organization_id = 'organization_id_example' # String | 
policy_name = 'policy_name_example' # String | 

begin
  # Delete a policy
  result = api_instance.delete_policy(organization_id, policy_name)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling PolicyManagementApi->delete_policy: #{e}"
end
```

#### Using the delete_policy_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<BaseSuccessResponse>, Integer, Hash)> delete_policy_with_http_info(organization_id, policy_name)

```ruby
begin
  # Delete a policy
  data, status_code, headers = api_instance.delete_policy_with_http_info(organization_id, policy_name)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <BaseSuccessResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling PolicyManagementApi->delete_policy_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **policy_name** | **String** |  |  |

### Return type

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## get_policy

> <Policy> get_policy(organization_id, policy_name)

Get a policy

Get a policy

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::PolicyManagementApi.new
organization_id = 'organization_id_example' # String | 
policy_name = 'policy_name_example' # String | 

begin
  # Get a policy
  result = api_instance.get_policy(organization_id, policy_name)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling PolicyManagementApi->get_policy: #{e}"
end
```

#### Using the get_policy_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Policy>, Integer, Hash)> get_policy_with_http_info(organization_id, policy_name)

```ruby
begin
  # Get a policy
  data, status_code, headers = api_instance.get_policy_with_http_info(organization_id, policy_name)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Policy>
rescue IamRuby::ApiError => e
  puts "Error when calling PolicyManagementApi->get_policy_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **policy_name** | **String** |  |  |

### Return type

[**Policy**](Policy.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## list_policies

> <PolicyPaginatedResponse> list_policies(organization_id, opts)

List policies

List policies

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::PolicyManagementApi.new
organization_id = 'organization_id_example' # String | 
opts = {
  next_token: 'eyJsYXN0SXRlbUlkIjogInN0cmluZyIsICJwYWdlU2l6ZSI6IDEyMywgInNvcnRPcmRlciI6ICJhc2MifQ==', # String | 
  page_size: '10', # String | 
  sort_order: 'asc' # String | 
}

begin
  # List policies
  result = api_instance.list_policies(organization_id, opts)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling PolicyManagementApi->list_policies: #{e}"
end
```

#### Using the list_policies_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<PolicyPaginatedResponse>, Integer, Hash)> list_policies_with_http_info(organization_id, opts)

```ruby
begin
  # List policies
  data, status_code, headers = api_instance.list_policies_with_http_info(organization_id, opts)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <PolicyPaginatedResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling PolicyManagementApi->list_policies_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
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


## update_policy

> <Policy> update_policy(organization_id, policy_name, update_policy_request)

Update a policy

Update a policy

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::PolicyManagementApi.new
organization_id = 'organization_id_example' # String | 
policy_name = 'policy_name_example' # String | 
update_policy_request = IamRuby::UpdatePolicyRequest.new({statements: [IamRuby::PolicyStatement.new({resource: 'resource_example', action: 'action_example', effect: 'allow'})]}) # UpdatePolicyRequest | Payload to update policy

begin
  # Update a policy
  result = api_instance.update_policy(organization_id, policy_name, update_policy_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling PolicyManagementApi->update_policy: #{e}"
end
```

#### Using the update_policy_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Policy>, Integer, Hash)> update_policy_with_http_info(organization_id, policy_name, update_policy_request)

```ruby
begin
  # Update a policy
  data, status_code, headers = api_instance.update_policy_with_http_info(organization_id, policy_name, update_policy_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Policy>
rescue IamRuby::ApiError => e
  puts "Error when calling PolicyManagementApi->update_policy_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **policy_name** | **String** |  |  |
| **update_policy_request** | [**UpdatePolicyRequest**](UpdatePolicyRequest.md) | Payload to update policy |  |

### Return type

[**Policy**](Policy.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

