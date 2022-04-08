# UserCredentialManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createCredential**](UserCredentialManagementApi.md#createCredential) | **POST** /organizations/{organization_id}/users/{user_name}/credentials | Create a new credential for a user
[**deleteCredential**](UserCredentialManagementApi.md#deleteCredential) | **DELETE** /organizations/{organization_id}/users/{user_name}/credentials/{credential_id} | Delete a credential
[**getCredential**](UserCredentialManagementApi.md#getCredential) | **GET** /organizations/{organization_id}/users/{user_name}/credentials/{credential_id} | Gets credential for the user
[**updateCredential**](UserCredentialManagementApi.md#updateCredential) | **PATCH** /organizations/{organization_id}/users/{user_name}/credentials/{credential_id} | Update the status of credential


<a name="createCredential"></a>
# **createCredential**
> Credential createCredential(userName, organizationId, createCredentialRequest)

Create a new credential for a user

Create a new credential for a user. This API returns the credential&#39;s secret key, which will be available only in the response of this API.

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserCredentialManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserCredentialManagementApi apiInstance = new UserCredentialManagementApi(defaultClient);
    String userName = "userName_example"; // String | 
    String organizationId = "organizationId_example"; // String | 
    CreateCredentialRequest createCredentialRequest = new CreateCredentialRequest(); // CreateCredentialRequest | Payload to create credential
    try {
      Credential result = apiInstance.createCredential(userName, organizationId, createCredentialRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserCredentialManagementApi#createCredential");
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
 **userName** | **String**|  |
 **organizationId** | **String**|  |
 **createCredentialRequest** | [**CreateCredentialRequest**](CreateCredentialRequest.md)| Payload to create credential |

### Return type

[**Credential**](Credential.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Response for CreateCredentialRequest |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="deleteCredential"></a>
# **deleteCredential**
> BaseSuccessResponse deleteCredential(organizationId, userName, credentialId)

Delete a credential

Delete a credential associated with the user

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserCredentialManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserCredentialManagementApi apiInstance = new UserCredentialManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String userName = "userName_example"; // String | 
    String credentialId = "credentialId_example"; // String | 
    try {
      BaseSuccessResponse result = apiInstance.deleteCredential(organizationId, userName, credentialId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserCredentialManagementApi#deleteCredential");
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
 **userName** | **String**|  |
 **credentialId** | **String**|  |

### Return type

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | OK |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="getCredential"></a>
# **getCredential**
> CredentialWithoutSecret getCredential(organizationId, userName, credentialId)

Gets credential for the user

Gets credential for the user, given the credential id

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserCredentialManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserCredentialManagementApi apiInstance = new UserCredentialManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String userName = "userName_example"; // String | 
    String credentialId = "credentialId_example"; // String | 
    try {
      CredentialWithoutSecret result = apiInstance.getCredential(organizationId, userName, credentialId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserCredentialManagementApi#getCredential");
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
 **userName** | **String**|  |
 **credentialId** | **String**|  |

### Return type

[**CredentialWithoutSecret**](CredentialWithoutSecret.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for GetCredentialRequest |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="updateCredential"></a>
# **updateCredential**
> CredentialWithoutSecret updateCredential(organizationId, userName, credentialId, updateCredentialRequest)

Update the status of credential

Update the status of credential to ACTIVE/INACTIVE. Credentials which are marked INACTIVE cannot be used to fetch short-term tokens.

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.UserCredentialManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    UserCredentialManagementApi apiInstance = new UserCredentialManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String userName = "userName_example"; // String | 
    String credentialId = "credentialId_example"; // String | 
    UpdateCredentialRequest updateCredentialRequest = new UpdateCredentialRequest(); // UpdateCredentialRequest | Payload to update credential
    try {
      CredentialWithoutSecret result = apiInstance.updateCredential(organizationId, userName, credentialId, updateCredentialRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling UserCredentialManagementApi#updateCredential");
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
 **userName** | **String**|  |
 **credentialId** | **String**|  |
 **updateCredentialRequest** | [**UpdateCredentialRequest**](UpdateCredentialRequest.md)| Payload to update credential |

### Return type

[**CredentialWithoutSecret**](CredentialWithoutSecret.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for UpdateCredentialRequest |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

