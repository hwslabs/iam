# IamRuby::UserAuthorizationApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

| Method | HTTP request | Description |
| ------ | ------------ | ----------- |
| [**get_token**](UserAuthorizationApi.md#get_token) | **POST** /organizations/{organization_id}/token | Generate a token |
| [**validate**](UserAuthorizationApi.md#validate) | **POST** /validate | Validate an auth request |


## get_token

> <TokenResponse> get_token(organization_id)

Generate a token

Generate a token for the given user credential

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserAuthorizationApi.new
organization_id = 'organization_id_example' # String | 

begin
  # Generate a token
  result = api_instance.get_token(organization_id)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserAuthorizationApi->get_token: #{e}"
end
```

#### Using the get_token_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<TokenResponse>, Integer, Hash)> get_token_with_http_info(organization_id)

```ruby
begin
  # Generate a token
  data, status_code, headers = api_instance.get_token_with_http_info(organization_id)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <TokenResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling UserAuthorizationApi->get_token_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **organization_id** | **String** |  |  |

### Return type

[**TokenResponse**](TokenResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## validate

> <ValidationResponse> validate(validation_request)

Validate an auth request

Validate if the caller has access to resource-action in the request

### Examples

```ruby
require 'time'
require 'iam-ruby'
# setup authorization
IamRuby.configure do |config|
  # Configure Bearer authorization (Bearer <credential>): bearerAuth
  config.access_token = 'YOUR_BEARER_TOKEN'
end

api_instance = IamRuby::UserAuthorizationApi.new
validation_request = IamRuby::ValidationRequest.new({validations: [IamRuby::ResourceAction.new({resource: 'resource_example', action: 'action_example'})]}) # ValidationRequest | Payload to validate if a user has access to a resource-action

begin
  # Validate an auth request
  result = api_instance.validate(validation_request)
  p result
rescue IamRuby::ApiError => e
  puts "Error when calling UserAuthorizationApi->validate: #{e}"
end
```

#### Using the validate_with_http_info variant

This returns an Array which contains the response data, status code and headers.

> <Array(<ValidationResponse>, Integer, Hash)> validate_with_http_info(validation_request)

```ruby
begin
  # Validate an auth request
  data, status_code, headers = api_instance.validate_with_http_info(validation_request)
  p status_code # => 2xx
  p headers # => { ... }
  p data # => <ValidationResponse>
rescue IamRuby::ApiError => e
  puts "Error when calling UserAuthorizationApi->validate_with_http_info: #{e}"
end
```

### Parameters

| Name | Type | Description | Notes |
| ---- | ---- | ----------- | ----- |
| **validation_request** | [**ValidationRequest**](ValidationRequest.md) | Payload to validate if a user has access to a resource-action |  |

### Return type

[**ValidationResponse**](ValidationResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

