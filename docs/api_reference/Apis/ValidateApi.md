# ValidateApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**validate**](ValidateApi.md#validate) | **POST** /validate | Validate an auth request


<a name="validate"></a>
# **validate**
> TokenResponse validate(ValidationRequest)

Validate an auth request

    Validate if the caller has access to resource-action in the request

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **ValidationRequest** | [**ValidationRequest**](../Models/ValidationRequest.md)| Payload to validate if a user has access to a resource-action |

### Return type

[**TokenResponse**](../Models/TokenResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

