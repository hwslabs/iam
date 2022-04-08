# UserAuthorizationApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**getToken**](UserAuthorizationApi.md#getToken) | **POST** /organizations/{organization_id}/token | Generate a token
[**validate**](UserAuthorizationApi.md#validate) | **POST** /validate | Validate an auth request


<a name="getToken"></a>
# **getToken**
> TokenResponse getToken(organizationId)

Generate a token

Generate a token for the given user credential

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserAuthorizationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserAuthorizationApi apiInstance = new UserAuthorizationApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    try {
      TokenResponse result = apiInstance.getToken(organizationId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserAuthorizationApi#getToken");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **organizationId** | **String**|  |

### Return type

[**TokenResponse**](TokenResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for generate token request |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="validate"></a>
# **validate**
> ValidationResponse validate(validationRequest)

Validate an auth request

Validate if the caller has access to resource-action in the request

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserAuthorizationApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserAuthorizationApi apiInstance = new UserAuthorizationApi(defaultClient);
    ValidationRequest validationRequest = new ValidationRequest(); // ValidationRequest | Payload to validate if a user has access to a resource-action
    try {
      ValidationResponse result = apiInstance.validate(validationRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserAuthorizationApi#validate");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **validationRequest** | [**ValidationRequest**](ValidationRequest.md)| Payload to validate if a user has access to a resource-action |

### Return type

[**ValidationResponse**](ValidationResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for validation request |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

