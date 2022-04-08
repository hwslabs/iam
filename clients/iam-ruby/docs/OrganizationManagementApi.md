# IamRuby::OrganizationManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

| Method | HTTP request | Description |
| ------ | ------------ | ----------- |
| [**create_organization**](OrganizationManagementApi.md#create_organization) | **POST** /organizations | Creates an organization. |
| [**delete_organization**](OrganizationManagementApi.md#delete_organization) | **DELETE** /organizations/{organization_id} | Delete an organization |
| [**get_organization**](OrganizationManagementApi.md#get_organization) | **GET** /organizations/{organization_id} | Get an organization |
| [**update_organization**](OrganizationManagementApi.md#update_organization) | **PATCH** /organizations/{organization_id} | Update an organization |


## create_organization

> <CreateOrganizationResponse> create_organization(create_organization_request)

Creates an organization.

Organization is the top level entity. All resources (like user, actions, policies) are created and managed under an organization. This is a privileged api and only internal applications has access to create an Organization.

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure API key authorization: apiKeyAuth
  config.api_key['apiKeyAuth'] = 'YOUR API KEY'
  # Uncomment the following line to set a prefix for the API key, e.g. 'Bearer' (defaults to nil)
  # config.api_key_prefix['apiKeyAuth'] = 'Bearer'
end

api_instance = IamRuby::OrganizationManagementApi.new
create_organization_request = IamRuby::CreateOrganizationRequest.new({name: 'name_example', admin_user: IamRuby::AdminUser.new({username: 'username_example', password_hash: 'password_hash_example', email: 'email_example', phone: 'phone_example'})}) # CreateOrganizationRequest | Payload to create organization

begin
  # Creates an organization.
  result = api_instance.create_organization(create_organization_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling OrganizationManagementApi->create_organization: #{e}"
end
```

#### Using the create_organization_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<CreateOrganizationResponse>, Integer, Hash)> create_organization_with_http_info(create_organization_request)

```ruby
begin
  # Creates an organization.
  data, status_code, headers = api_instance.create_organization_with_http_info(create_organization_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <CreateOrganizationResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling OrganizationManagementApi->create_organization_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **create_organization_request** | [**CreateOrganizationRequest**](CreateOrganizationRequest.md) | Payload to create organization |  |

### Return type

[**CreateOrganizationResponse**](CreateOrganizationResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


## delete_organization

> <BaseSuccessResponse> delete_organization(organization_id)

Delete an organization

Delete an organization. This is a privileged api and only internal application will have access to delete organization.

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure API key authorization: apiKeyAuth
  config.api_key['apiKeyAuth'] = 'YOUR API KEY'
  # Uncomment the following line to set a prefix for the API key, e.g. 'Bearer' (defaults to nil)
  # config.api_key_prefix['apiKeyAuth'] = 'Bearer'
end

api_instance = IamRuby::OrganizationManagementApi.new
organization_id = 'organization_id_example' # String | 

begin
  # Delete an organization
  result = api_instance.delete_organization(organization_id)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling OrganizationManagementApi->delete_organization: #{e}"
end
```

#### Using the delete_organization_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<BaseSuccessResponse>, Integer, Hash)> delete_organization_with_http_info(organization_id)

```ruby
begin
  # Delete an organization
  data, status_code, headers = api_instance.delete_organization_with_http_info(organization_id)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <BaseSuccessResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling OrganizationManagementApi->delete_organization_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |

### Return type

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## get_organization

> <Organization> get_organization(organization_id)

Get an organization

Get an organization and the metadata for the given organization.

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::OrganizationManagementApi.new
organization_id = 'organization_id_example' # String | 

begin
  # Get an organization
  result = api_instance.get_organization(organization_id)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling OrganizationManagementApi->get_organization: #{e}"
end
```

#### Using the get_organization_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Organization>, Integer, Hash)> get_organization_with_http_info(organization_id)

```ruby
begin
  # Get an organization
  data, status_code, headers = api_instance.get_organization_with_http_info(organization_id)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Organization>
rescue IamRuby::ApiError => e
  puts "Error when calling OrganizationManagementApi->get_organization_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |

### Return type

[**Organization**](Organization.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## update_organization

> <Organization> update_organization(organization_id, update_organization_request)

Update an organization

Update an organization

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::OrganizationManagementApi.new
organization_id = 'organization_id_example' # String | 
update_organization_request = IamRuby::UpdateOrganizationRequest.new # UpdateOrganizationRequest | Payload to update organization

begin
  # Update an organization
  result = api_instance.update_organization(organization_id, update_organization_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling OrganizationManagementApi->update_organization: #{e}"
end
```

#### Using the update_organization_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<Organization>, Integer, Hash)> update_organization_with_http_info(organization_id, update_organization_request)

```ruby
begin
  # Update an organization
  data, status_code, headers = api_instance.update_organization_with_http_info(organization_id, update_organization_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <Organization>
rescue IamRuby::ApiError => e
  puts "Error when calling OrganizationManagementApi->update_organization_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |
| **update_organization_request** | [**UpdateOrganizationRequest**](UpdateOrganizationRequest.md) | Payload to update organization |  |

### Return type

[**Organization**](Organization.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

