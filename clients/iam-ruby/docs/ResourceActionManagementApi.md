# IamRuby::ResourceActionManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

| Method | HTTP request | Description |
| ------ | ------------ | ----------- |
| [**create_action**](ResourceActionManagementApi.md#create_action) | **POST** /organizations/{organization_id}/resources/{resource_name}/actions | Create an action |
| [**delete_action**](ResourceActionManagementApi.md#delete_action) | **DELETE** /organizations/{organization_id}/resources/{resource_name}/actions/{action_name} | Delete an action |
| [**get_action**](ResourceActionManagementApi.md#get_action) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions/{action_name} | Get an action |
| [**list_actions**](ResourceActionManagementApi.md#list_actions) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions | List actions |
| [**update_action**](ResourceActionManagementApi.md#update_action) | **PATCH** /organizations/{organization_id}/resources/{resource_name}/actions/{action_name} | Update an action |


## create_action

> <Action> create_action(organization_id, resource_name, create_action_request)

Create an action

Create an action

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::ResourceActionManagementApi.new
organization_id = 'organization_id_example' # String | 
resource_name = 'resource_name_example' # String | 
create_action_request = IamRuby::CreateActionRequest.new({name: 'name_example'}) # CreateActionRequest | Payload to create action

begin
  # Create an action
  result = api_instance.create_action(organization_id, resource_name, create_action_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceActionManagementApi->create_action: #{e}"
end
```

#### Using the create_action_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Action>, Integer, Hash)> create_action_with_http_info(organization_id, resource_name, create_action_request)

```ruby
begin
  # Create an action
  data, status_code, headers = api_instance.create_action_with_http_info(organization_id, resource_name, create_action_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Action>
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceActionManagementApi->create_action_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **resource_name** | **String** |  |  |
| **create_action_request** | [**CreateActionRequest**](CreateActionRequest.md) | Payload to create action |  |

### Return type

[**Action**](Action.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


## delete_action

> <BaseSuccessResponse> delete_action(organization_id, resource_name, action_name)

Delete an action

Delete an action

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::ResourceActionManagementApi.new
organization_id = 'organization_id_example' # String | 
resource_name = 'resource_name_example' # String | 
action_name = 'action_name_example' # String | 

begin
  # Delete an action
  result = api_instance.delete_action(organization_id, resource_name, action_name)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceActionManagementApi->delete_action: #{e}"
end
```

#### Using the delete_action_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<BaseSuccessResponse>, Integer, Hash)> delete_action_with_http_info(organization_id, resource_name, action_name)

```ruby
begin
  # Delete an action
  data, status_code, headers = api_instance.delete_action_with_http_info(organization_id, resource_name, action_name)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <BaseSuccessResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceActionManagementApi->delete_action_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **resource_name** | **String** |  |  |
| **action_name** | **String** |  |  |

### Return type

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## get_action

> <Action> get_action(organization_id, resource_name, action_name)

Get an action

Get an action

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::ResourceActionManagementApi.new
organization_id = 'organization_id_example' # String | 
resource_name = 'resource_name_example' # String | 
action_name = 'action_name_example' # String | 

begin
  # Get an action
  result = api_instance.get_action(organization_id, resource_name, action_name)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceActionManagementApi->get_action: #{e}"
end
```

#### Using the get_action_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Action>, Integer, Hash)> get_action_with_http_info(organization_id, resource_name, action_name)

```ruby
begin
  # Get an action
  data, status_code, headers = api_instance.get_action_with_http_info(organization_id, resource_name, action_name)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Action>
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceActionManagementApi->get_action_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **resource_name** | **String** |  |  |
| **action_name** | **String** |  |  |

### Return type

[**Action**](Action.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## list_actions

> <ActionPaginatedResponse> list_actions(organization_id, resource_name, opts)

List actions

List actions

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::ResourceActionManagementApi.new
organization_id = 'organization_id_example' # String | 
resource_name = 'resource_name_example' # String | 
opts = {
  next_token: 'eyJsYXN0SXRlbUlkIjogInN0cmluZyIsICJwYWdlU2l6ZSI6IDEyMywgInNvcnRPcmRlciI6ICJhc2MifQ==', # String | 
  page_size: '10', # String | 
  sort_order: 'asc' # String | 
}

begin
  # List actions
  result = api_instance.list_actions(organization_id, resource_name, opts)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceActionManagementApi->list_actions: #{e}"
end
```

#### Using the list_actions_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<ActionPaginatedResponse>, Integer, Hash)> list_actions_with_http_info(organization_id, resource_name, opts)

```ruby
begin
  # List actions
  data, status_code, headers = api_instance.list_actions_with_http_info(organization_id, resource_name, opts)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <ActionPaginatedResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceActionManagementApi->list_actions_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **resource_name** | **String** |  |  |
| **next_token** | **String** |  | [optional] |
| **page_size** | **String** |  | [optional] |
| **sort_order** | **String** |  | [optional] |

### Return type

[**ActionPaginatedResponse**](ActionPaginatedResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## update_action

> <Action> update_action(organization_id, resource_name, action_name, update_action_request)

Update an action

Update an action

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::ResourceActionManagementApi.new
organization_id = 'organization_id_example' # String | 
resource_name = 'resource_name_example' # String | 
action_name = 'action_name_example' # String | 
update_action_request = IamRuby::UpdateActionRequest.new({description: 'description_example'}) # UpdateActionRequest | Payload to update action

begin
  # Update an action
  result = api_instance.update_action(organization_id, resource_name, action_name, update_action_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceActionManagementApi->update_action: #{e}"
end
```

#### Using the update_action_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Action>, Integer, Hash)> update_action_with_http_info(organization_id, resource_name, action_name, update_action_request)

```ruby
begin
  # Update an action
  data, status_code, headers = api_instance.update_action_with_http_info(organization_id, resource_name, action_name, update_action_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Action>
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceActionManagementApi->update_action_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **resource_name** | **String** |  |  |
| **action_name** | **String** |  |  |
| **update_action_request** | [**UpdateActionRequest**](UpdateActionRequest.md) | Payload to update action |  |

### Return type

[**Action**](Action.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

