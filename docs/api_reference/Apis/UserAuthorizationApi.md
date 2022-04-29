# UserAuthorizationApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getToken**](UserAuthorizationApi.md#getToken) | **POST** /token | Generate a token
[**getTokenForOrg**](UserAuthorizationApi.md#getTokenForOrg) | **POST** /organizations/{organization_id}/token | Generate a organization_id scoped token
[**validate**](UserAuthorizationApi.md#validate) | **POST** /validate | Validate an auth request


<a name="getToken"></a>
# **getToken**
> TokenResponse getToken()

Generate a token

    Generate a token for the given user credential (same as /organizations/{organization_id}/token at the moment. Might change in future)

### Parameters
This endpoint does not need any parameter.

### Return type

[**TokenResponse**](../Models/TokenResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, text/plain

<a name="getTokenForOrg"></a>
# **getTokenForOrg**
> TokenResponse getTokenForOrg(organization\_id)

Generate a organization_id scoped token

    Generate a token for the given user credential scoped by the provided organization_id

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organization\_id** | **String**|  | [default to null]

### Return type

[**TokenResponse**](../Models/TokenResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, text/plain

<a name="validate"></a>
# **validate**
> ValidationResponse validate(ValidationRequest)

Validate an auth request

    Validate if the caller has access to resource-action in the request

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ValidationRequest** | [**ValidationRequest**](../Models/ValidationRequest.md)| Payload to validate if a user has access to a resource-action |

### Return type

[**ValidationResponse**](../Models/ValidationResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

