# UserAuthenticationApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**authenticate**](UserAuthenticationApi.md#authenticate) | **POST** /authenticate | Authenticate a request


<a name="authenticate"></a>
# **authenticate**
> TokenResponse authenticate()

Authenticate a request

    Authenticates the request and respond with token. Upon successful authentication, - For basic auth as well as credential based bearer auth, this API generates a token and returns it. - For JWT bearer auth, returns the same JWT token in response 

### Parameters
This endpoint does not need any parameter.

### Return type

[**TokenResponse**](../Models/TokenResponse.md)

### Authorization

[basicAuth](../README.md#basicAuth), [bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json, text/plain

