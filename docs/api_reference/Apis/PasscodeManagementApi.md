# PasscodeManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**verifyEmail**](PasscodeManagementApi.md#verifyEmail) | **POST** /verifyEmail | Verify email


<a name="verifyEmail"></a>
# **verifyEmail**
> BaseSuccessResponse verifyEmail(VerifyEmailRequest)

Verify email

    Verify email during account opening

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **VerifyEmailRequest** | [**VerifyEmailRequest**](../Models/VerifyEmailRequest.md)| Payload to send verification link to email |

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

