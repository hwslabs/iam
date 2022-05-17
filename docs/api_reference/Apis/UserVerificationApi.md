# UserVerificationApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**verifyEmail**](UserVerificationApi.md#verifyEmail) | **POST** /verifyEmail | Verify email


<a name="verifyEmail"></a>
# **verifyEmail**
> BaseSuccessResponse verifyEmail(VerifyEmailRequest)

Verify email

    Verify email during account opening and resetting password

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **VerifyEmailRequest** | [**VerifyEmailRequest**](../Models/VerifyEmailRequest.md)| Payload to send verification link to email |

### Return type

[**BaseSuccessResponse**](../Models/BaseSuccessResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json

