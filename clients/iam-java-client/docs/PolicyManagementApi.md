# PolicyManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createPolicy**](PolicyManagementApi.md#createPolicy) | **POST** /organizations/{organization_id}/policies | Create a policy
[**deletePolicy**](PolicyManagementApi.md#deletePolicy) | **DELETE** /organizations/{organization_id}/policies/{policy_name} | Delete a policy
[**getPolicy**](PolicyManagementApi.md#getPolicy) | **GET** /organizations/{organization_id}/policies/{policy_name} | Get a policy
[**listPolicies**](PolicyManagementApi.md#listPolicies) | **GET** /organizations/{organization_id}/policies | List policies
[**updatePolicy**](PolicyManagementApi.md#updatePolicy) | **PATCH** /organizations/{organization_id}/policies/{policy_name} | Update a policy


<a name="createPolicy"></a>
# **createPolicy**
> Policy createPolicy(organizationId, createPolicyRequest)

Create a policy

Create a policy

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.PolicyManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    PolicyManagementApi apiInstance = new PolicyManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    CreatePolicyRequest createPolicyRequest = new CreatePolicyRequest(); // CreatePolicyRequest | Payload to create policy
    try {
      Policy result = apiInstance.createPolicy(organizationId, createPolicyRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PolicyManagementApi#createPolicy");
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
 **createPolicyRequest** | [**CreatePolicyRequest**](CreatePolicyRequest.md)| Payload to create policy |

### Return type

[**Policy**](Policy.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Response for policy requests returning an policy entity |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="deletePolicy"></a>
# **deletePolicy**
> BaseSuccessResponse deletePolicy(organizationId, policyName)

Delete a policy

Delete a policy

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.PolicyManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    PolicyManagementApi apiInstance = new PolicyManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String policyName = "policyName_example"; // String | 
    try {
      BaseSuccessResponse result = apiInstance.deletePolicy(organizationId, policyName);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PolicyManagementApi#deletePolicy");
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
 **policyName** | **String**|  |

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

<a name="getPolicy"></a>
# **getPolicy**
> Policy getPolicy(organizationId, policyName)

Get a policy

Get a policy

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.PolicyManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    PolicyManagementApi apiInstance = new PolicyManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String policyName = "policyName_example"; // String | 
    try {
      Policy result = apiInstance.getPolicy(organizationId, policyName);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PolicyManagementApi#getPolicy");
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
 **policyName** | **String**|  |

### Return type

[**Policy**](Policy.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for policy requests returning an policy entity |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="listPolicies"></a>
# **listPolicies**
> PolicyPaginatedResponse listPolicies(organizationId, nextToken, pageSize, sortOrder)

List policies

List policies

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.PolicyManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    PolicyManagementApi apiInstance = new PolicyManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String nextToken = "eyJsYXN0SXRlbUlkIjogInN0cmluZyIsICJwYWdlU2l6ZSI6IDEyMywgInNvcnRPcmRlciI6ICJhc2MifQ=="; // String | 
    String pageSize = "10"; // String | 
    String sortOrder = "asc"; // String | 
    try {
      PolicyPaginatedResponse result = apiInstance.listPolicies(organizationId, nextToken, pageSize, sortOrder);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PolicyManagementApi#listPolicies");
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
 **nextToken** | **String**|  | [optional]
 **pageSize** | **String**|  | [optional]
 **sortOrder** | **String**|  | [optional] [enum: asc, desc]

### Return type

[**PolicyPaginatedResponse**](PolicyPaginatedResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Response for list policies request and list policies for user request |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="updatePolicy"></a>
# **updatePolicy**
> Policy updatePolicy(organizationId, policyName, updatePolicyRequest)

Update a policy

Update a policy

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.PolicyManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    PolicyManagementApi apiInstance = new PolicyManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String policyName = "policyName_example"; // String | 
    UpdatePolicyRequest updatePolicyRequest = new UpdatePolicyRequest(); // UpdatePolicyRequest | Payload to update policy
    try {
      Policy result = apiInstance.updatePolicy(organizationId, policyName, updatePolicyRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling PolicyManagementApi#updatePolicy");
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
 **policyName** | **String**|  |
 **updatePolicyRequest** | [**UpdatePolicyRequest**](UpdatePolicyRequest.md)| Payload to update policy |

### Return type

[**Policy**](Policy.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for policy requests returning an policy entity |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

