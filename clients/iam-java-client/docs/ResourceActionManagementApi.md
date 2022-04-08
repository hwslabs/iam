# ResourceActionManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createAction**](ResourceActionManagementApi.md#createAction) | **POST** /organizations/{organization_id}/resources/{resource_name}/actions | Create an action
[**deleteAction**](ResourceActionManagementApi.md#deleteAction) | **DELETE** /organizations/{organization_id}/resources/{resource_name}/actions/{action_name} | Delete an action
[**getAction**](ResourceActionManagementApi.md#getAction) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions/{action_name} | Get an action
[**listActions**](ResourceActionManagementApi.md#listActions) | **GET** /organizations/{organization_id}/resources/{resource_name}/actions | List actions
[**updateAction**](ResourceActionManagementApi.md#updateAction) | **PATCH** /organizations/{organization_id}/resources/{resource_name}/actions/{action_name} | Update an action


<a name="createAction"></a>
# **createAction**
> Action createAction(organizationId, resourceName, createActionRequest)

Create an action

Create an action

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.ResourceActionManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    ResourceActionManagementApi apiInstance = new ResourceActionManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String resourceName = "resourceName_example"; // String | 
    CreateActionRequest createActionRequest = new CreateActionRequest(); // CreateActionRequest | Payload to create action
    try {
      Action result = apiInstance.createAction(organizationId, resourceName, createActionRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceActionManagementApi#createAction");
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
 **resourceName** | **String**|  |
 **createActionRequest** | [**CreateActionRequest**](CreateActionRequest.md)| Payload to create action |

### Return type

[**Action**](Action.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Response for action requests returning an action entity |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="deleteAction"></a>
# **deleteAction**
> BaseSuccessResponse deleteAction(organizationId, resourceName, actionName)

Delete an action

Delete an action

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.ResourceActionManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    ResourceActionManagementApi apiInstance = new ResourceActionManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String resourceName = "resourceName_example"; // String | 
    String actionName = "actionName_example"; // String | 
    try {
      BaseSuccessResponse result = apiInstance.deleteAction(organizationId, resourceName, actionName);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceActionManagementApi#deleteAction");
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
 **resourceName** | **String**|  |
 **actionName** | **String**|  |

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

<a name="getAction"></a>
# **getAction**
> Action getAction(organizationId, resourceName, actionName)

Get an action

Get an action

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.ResourceActionManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    ResourceActionManagementApi apiInstance = new ResourceActionManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String resourceName = "resourceName_example"; // String | 
    String actionName = "actionName_example"; // String | 
    try {
      Action result = apiInstance.getAction(organizationId, resourceName, actionName);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceActionManagementApi#getAction");
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
 **resourceName** | **String**|  |
 **actionName** | **String**|  |

### Return type

[**Action**](Action.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for action requests returning an action entity |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="listActions"></a>
# **listActions**
> ActionPaginatedResponse listActions(organizationId, resourceName, nextToken, pageSize, sortOrder)

List actions

List actions

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.ResourceActionManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    ResourceActionManagementApi apiInstance = new ResourceActionManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String resourceName = "resourceName_example"; // String | 
    String nextToken = "eyJsYXN0SXRlbUlkIjogInN0cmluZyIsICJwYWdlU2l6ZSI6IDEyMywgInNvcnRPcmRlciI6ICJhc2MifQ=="; // String | 
    String pageSize = "10"; // String | 
    String sortOrder = "asc"; // String | 
    try {
      ActionPaginatedResponse result = apiInstance.listActions(organizationId, resourceName, nextToken, pageSize, sortOrder);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceActionManagementApi#listActions");
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
 **resourceName** | **String**|  |
 **nextToken** | **String**|  | [optional]
 **pageSize** | **String**|  | [optional]
 **sortOrder** | **String**|  | [optional] [enum: asc, desc]

### Return type

[**ActionPaginatedResponse**](ActionPaginatedResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for list actions request |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="updateAction"></a>
# **updateAction**
> Action updateAction(organizationId, resourceName, actionName, updateActionRequest)

Update an action

Update an action

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.ResourceActionManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    ResourceActionManagementApi apiInstance = new ResourceActionManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String resourceName = "resourceName_example"; // String | 
    String actionName = "actionName_example"; // String | 
    UpdateActionRequest updateActionRequest = new UpdateActionRequest(); // UpdateActionRequest | Payload to update action
    try {
      Action result = apiInstance.updateAction(organizationId, resourceName, actionName, updateActionRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceActionManagementApi#updateAction");
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
 **resourceName** | **String**|  |
 **actionName** | **String**|  |
 **updateActionRequest** | [**UpdateActionRequest**](UpdateActionRequest.md)| Payload to update action |

### Return type

[**Action**](Action.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for action requests returning an action entity |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

