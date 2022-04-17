# KeyManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getKey**](KeyManagementApi.md#getKey) | **GET** /keys/{kid} | Get keys


<a name="getKey"></a>
# **getKey**
> KeyResponse getKey(kid, format, type)

Get keys

    Get public/private keys from Key-id in der/pem format

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **kid** | **String**|  | [default to null]
 **format** | **String**|  | [optional] [default to pem] [enum: der, pem]
 **type** | **String**|  | [optional] [default to public] [enum: public]

### Return type

[**KeyResponse**](../Models/KeyResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

