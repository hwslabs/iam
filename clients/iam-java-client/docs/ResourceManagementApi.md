# ResourceManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createResource**](ResourceManagementApi.md#createResource) | **POST** /organizations/{organization_id}/resources | Create a resource name in an organization.
[**deleteResource**](ResourceManagementApi.md#deleteResource) | **DELETE** /organizations/{organization_id}/resources/{resource_name} | Delete a resource
[**getResource**](ResourceManagementApi.md#getResource) | **GET** /organizations/{organization_id}/resources/{resource_name} | Get the resource details
[**listResources**](ResourceManagementApi.md#listResources) | **GET** /organizations/{organization_id}/resources | List Resources
[**updateResource**](ResourceManagementApi.md#updateResource) | **PATCH** /organizations/{organization_id}/resources/{resource_name} | Update a resource


<a name="createResource"></a>
# **createResource**
> Resource createResource(organizationId, createResourceRequest)

Create a resource name in an organization.

Creates a resource name. Access policies can be associated with the instances of these resources. ex - \\\&quot;Wallet\\\&quot; is a resource name in the organization org - \\\&quot;Org#1\\\&quot; and \\\&quot;wallet#1\\\&quot; is the instance of the resource \\\&quot;Wallet\\\&quot;. Policies on which user to access the wallet#1 can be created by the user having privilege to access the resource.

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.ResourceManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    ResourceManagementApi apiInstance = new ResourceManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    CreateResourceRequest createResourceRequest = new CreateResourceRequest(); // CreateResourceRequest | Payload to create resource
    try {
      Resource result = apiInstance.createResource(organizationId, createResourceRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceManagementApi#createResource");
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
 **createResourceRequest** | [**CreateResourceRequest**](CreateResourceRequest.md)| Payload to create resource |

### Return type

[**Resource**](Resource.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Response for resource requests returning a resource entity |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="deleteResource"></a>
# **deleteResource**
> BaseSuccessResponse deleteResource(organizationId, resourceName)

Delete a resource

Delete a resource

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.ResourceManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    ResourceManagementApi apiInstance = new ResourceManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String resourceName = "resourceName_example"; // String | 
    try {
      BaseSuccessResponse result = apiInstance.deleteResource(organizationId, resourceName);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceManagementApi#deleteResource");
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

<a name="getResource"></a>
# **getResource**
> Resource getResource(organizationId, resourceName)

Get the resource details

Gets the resource details associated with the organization

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.ResourceManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    ResourceManagementApi apiInstance = new ResourceManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String resourceName = "resourceName_example"; // String | 
    try {
      Resource result = apiInstance.getResource(organizationId, resourceName);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceManagementApi#getResource");
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

### Return type

[**Resource**](Resource.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for resource requests returning a resource entity |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="listResources"></a>
# **listResources**
> ResourcePaginatedResponse listResources(organizationId, nextToken, pageSize, sortOrder)

List Resources

List all the resource names in an organization.

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.ResourceManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    ResourceManagementApi apiInstance = new ResourceManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String nextToken = "eyJsYXN0SXRlbUlkIjogInN0cmluZyIsICJwYWdlU2l6ZSI6IDEyMywgInNvcnRPcmRlciI6ICJhc2MifQ=="; // String | 
    String pageSize = "10"; // String | 
    String sortOrder = "asc"; // String | 
    try {
      ResourcePaginatedResponse result = apiInstance.listResources(organizationId, nextToken, pageSize, sortOrder);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceManagementApi#listResources");
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

[**ResourcePaginatedResponse**](ResourcePaginatedResponse.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Response for list resources request |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="updateResource"></a>
# **updateResource**
> Resource updateResource(organizationId, resourceName, updateResourceRequest)

Update a resource

Update resource name of the organization

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.ResourceManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    ResourceManagementApi apiInstance = new ResourceManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    String resourceName = "resourceName_example"; // String | 
    UpdateResourceRequest updateResourceRequest = new UpdateResourceRequest(); // UpdateResourceRequest | Payload to update resource
    try {
      Resource result = apiInstance.updateResource(organizationId, resourceName, updateResourceRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling ResourceManagementApi#updateResource");
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
 **updateResourceRequest** | [**UpdateResourceRequest**](UpdateResourceRequest.md)| Payload to update resource |

### Return type

[**Resource**](Resource.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for resource requests returning a resource entity |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

