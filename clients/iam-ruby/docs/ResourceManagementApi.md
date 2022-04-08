# IamRuby::ResourceManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

| Method | HTTP request | Description |
| ------ | ------------ | ----------- |
| [**create_resource**](ResourceManagementApi.md#create_resource) | **POST** /organizations/{organization_id}/resources | Create a resource name in an organization. |
| [**delete_resource**](ResourceManagementApi.md#delete_resource) | **DELETE** /organizations/{organization_id}/resources/{resource_name} | Delete a resource |
| [**get_resource**](ResourceManagementApi.md#get_resource) | **GET** /organizations/{organization_id}/resources/{resource_name} | Get the resource details |
| [**list_resources**](ResourceManagementApi.md#list_resources) | **GET** /organizations/{organization_id}/resources | List Resources |
| [**update_resource**](ResourceManagementApi.md#update_resource) | **PATCH** /organizations/{organization_id}/resources/{resource_name} | Update a resource |


## create_resource

> <Resource> create_resource(organization_id, create_resource_request)

Create a resource name in an organization.

Creates a resource name. Access policies can be associated with the instances of these resources. ex - \\\"Wallet\\\" is a resource name in the organization org - \\\"Org#1\\\" and \\\"wallet#1\\\" is the instance of the resource \\\"Wallet\\\". Policies on which user to access the wallet#1 can be created by the user having privilege to access the resource.

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::ResourceManagementApi.new
organization_id = 'organization_id_example' # String | 
create_resource_request = IamRuby::CreateResourceRequest.new({name: 'name_example'}) # CreateResourceRequest | Payload to create resource

begin
  # Create a resource name in an organization.
  result = api_instance.create_resource(organization_id, create_resource_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceManagementApi->create_resource: #{e}"
end
```

#### Using the create_resource_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Resource>, Integer, Hash)> create_resource_with_http_info(organization_id, create_resource_request)

```ruby
begin
  # Create a resource name in an organization.
  data, status_code, headers = api_instance.create_resource_with_http_info(organization_id, create_resource_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Resource>
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceManagementApi->create_resource_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **create_resource_request** | [**CreateResourceRequest**](CreateResourceRequest.md) | Payload to create resource |  |

### Return type

[**Resource**](Resource.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


## delete_resource

> <BaseSuccessResponse> delete_resource(organization_id, resource_name)

Delete a resource

Delete a resource

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::ResourceManagementApi.new
organization_id = 'organization_id_example' # String | 
resource_name = 'resource_name_example' # String | 

begin
  # Delete a resource
  result = api_instance.delete_resource(organization_id, resource_name)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceManagementApi->delete_resource: #{e}"
end
```

#### Using the delete_resource_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<BaseSuccessResponse>, Integer, Hash)> delete_resource_with_http_info(organization_id, resource_name)

```ruby
begin
  # Delete a resource
  data, status_code, headers = api_instance.delete_resource_with_http_info(organization_id, resource_name)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <BaseSuccessResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceManagementApi->delete_resource_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **resource_name** | **String** |  |  |

### Return type

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## get_resource

> <Resource> get_resource(organization_id, resource_name)

Get the resource details

Gets the resource details associated with the organization

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::ResourceManagementApi.new
organization_id = 'organization_id_example' # String | 
resource_name = 'resource_name_example' # String | 

begin
  # Get the resource details
  result = api_instance.get_resource(organization_id, resource_name)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceManagementApi->get_resource: #{e}"
end
```

#### Using the get_resource_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Resource>, Integer, Hash)> get_resource_with_http_info(organization_id, resource_name)

```ruby
begin
  # Get the resource details
  data, status_code, headers = api_instance.get_resource_with_http_info(organization_id, resource_name)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Resource>
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceManagementApi->get_resource_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **resource_name** | **String** |  |  |

### Return type

[**Resource**](Resource.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## list_resources

> <ResourcePaginatedResponse> list_resources(organization_id, opts)

List Resources

List all the resource names in an organization.

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::ResourceManagementApi.new
organization_id = 'organization_id_example' # String | 
opts = {
  next_token: 'eyJsYXN0SXRlbUlkIjogInN0cmluZyIsICJwYWdlU2l6ZSI6IDEyMywgInNvcnRPcmRlciI6ICJhc2MifQ==', # String | 
  page_size: '10', # String | 
  sort_order: 'asc' # String | 
}

begin
  # List Resources
  result = api_instance.list_resources(organization_id, opts)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceManagementApi->list_resources: #{e}"
end
```

#### Using the list_resources_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<ResourcePaginatedResponse>, Integer, Hash)> list_resources_with_http_info(organization_id, opts)

```ruby
begin
  # List Resources
  data, status_code, headers = api_instance.list_resources_with_http_info(organization_id, opts)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <ResourcePaginatedResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceManagementApi->list_resources_with_http_info: #{e}"
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

[**ResourcePaginatedResponse**](ResourcePaginatedResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## update_resource

> <Resource> update_resource(organization_id, resource_name, update_resource_request)

Update a resource

Update resource name of the organization

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::ResourceManagementApi.new
organization_id = 'organization_id_example' # String | 
resource_name = 'resource_name_example' # String | 
update_resource_request = IamRuby::UpdateResourceRequest.new # UpdateResourceRequest | Payload to update resource

begin
  # Update a resource
  result = api_instance.update_resource(organization_id, resource_name, update_resource_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceManagementApi->update_resource: #{e}"
end
```

#### Using the update_resource_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Resource>, Integer, Hash)> update_resource_with_http_info(organization_id, resource_name, update_resource_request)

```ruby
begin
  # Update a resource
  data, status_code, headers = api_instance.update_resource_with_http_info(organization_id, resource_name, update_resource_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Resource>
rescue IamRuby::ApiError => e
  puts "Error when calling ResourceManagementApi->update_resource_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **resource_name** | **String** |  |  |
| **update_resource_request** | [**UpdateResourceRequest**](UpdateResourceRequest.md) | Payload to update resource |  |

### Return type

[**Resource**](Resource.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

