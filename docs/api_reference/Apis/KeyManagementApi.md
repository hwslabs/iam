# KeyManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getPublicKey**](KeyManagementApi.md#getPublicKey) | **GET** /public_keys/{kid} | Get public key


<a name="getPublicKey"></a>
# **getPublicKey**
> PublicKeyResponse getPublicKey(kid, UNKNOWN_PARAMETER_NAME)

Get public key

    Get public key from Key-id

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **kid** | **String**|  | [default to null]
 **UNKNOWN_PARAMETER_NAME** | [****](../Models/.md)|  | [optional]

### Return type

[**PublicKeyResponse**](../Models/PublicKeyResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

