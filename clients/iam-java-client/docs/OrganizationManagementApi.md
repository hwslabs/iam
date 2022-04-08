# OrganizationManagementApi

All URIs are relative to *https://sandbox-iam.us.hypto.com/v1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createOrganization**](OrganizationManagementApi.md#createOrganization) | **POST** /organizations | Creates an organization.
[**deleteOrganization**](OrganizationManagementApi.md#deleteOrganization) | **DELETE** /organizations/{organization_id} | Delete an organization
[**getOrganization**](OrganizationManagementApi.md#getOrganization) | **GET** /organizations/{organization_id} | Get an organization
[**updateOrganization**](OrganizationManagementApi.md#updateOrganization) | **PATCH** /organizations/{organization_id} | Update an organization


<a name="createOrganization"></a>
# **createOrganization**
> CreateOrganizationResponse createOrganization(createOrganizationRequest)

Creates an organization.

Organization is the top level entity. All resources (like user, actions, policies) are created and managed under an organization. This is a privileged api and only internal applications has access to create an Organization.

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.OrganizationManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure API key authorization: apiKeyAuth
    ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("apiKeyAuth");
    apiKeyAuth.setApiKey("YOUR API KEY");
    // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
    //apiKeyAuth.setApiKeyPrefix("Token");

    OrganizationManagementApi apiInstance = new OrganizationManagementApi(defaultClient);
    CreateOrganizationRequest createOrganizationRequest = new CreateOrganizationRequest(); // CreateOrganizationRequest | Payload to create organization
    try {
      CreateOrganizationResponse result = apiInstance.createOrganization(createOrganizationRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OrganizationManagementApi#createOrganization");
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
 **createOrganizationRequest** | [**CreateOrganizationRequest**](CreateOrganizationRequest.md)| Payload to create organization |

### Return type

[**CreateOrganizationResponse**](CreateOrganizationResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**201** | Response for Create Organization request |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="deleteOrganization"></a>
# **deleteOrganization**
> BaseSuccessResponse deleteOrganization(organizationId)

Delete an organization

Delete an organization. This is a privileged api and only internal application will have access to delete organization.

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.OrganizationManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure API key authorization: apiKeyAuth
    ApiKeyAuth apiKeyAuth = (ApiKeyAuth) defaultClient.getAuthentication("apiKeyAuth");
    apiKeyAuth.setApiKey("YOUR API KEY");
    // Uncomment the following line to set a prefix for the API key, e.g. "Token" (defaults to null)
    //apiKeyAuth.setApiKeyPrefix("Token");

    OrganizationManagementApi apiInstance = new OrganizationManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    try {
      BaseSuccessResponse result = apiInstance.deleteOrganization(organizationId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OrganizationManagementApi#deleteOrganization");
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

[**BaseSuccessResponse**](BaseSuccessResponse.md)

### Authorization

[apiKeyAuth](../README.md#apiKeyAuth)

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

<a name="getOrganization"></a>
# **getOrganization**
> Organization getOrganization(organizationId)

Get an organization

Get an organization and the metadata for the given organization.

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.OrganizationManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    OrganizationManagementApi apiInstance = new OrganizationManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    try {
      Organization result = apiInstance.getOrganization(organizationId);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OrganizationManagementApi#getOrganization");
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

[**Organization**](Organization.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for Get and Update Organization requests |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

<a name="updateOrganization"></a>
# **updateOrganization**
> Organization updateOrganization(organizationId, updateOrganizationRequest)

Update an organization

Update an organization

### Example
```java
// Import classes:
import com.hypto.iam.client.ApiClient;
import com.hypto.iam.client.ApiException;
import com.hypto.iam.client.Configuration;
import com.hypto.iam.client.auth.*;
import com.hypto.iam.client.models.*;
import com.hypto.iam.client.api.OrganizationManagementApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("https://sandbox-iam.us.hypto.com/v1");
    
    // Configure HTTP bearer authorization: bearerAuth
    HttpBearerAuth bearerAuth = (HttpBearerAuth) defaultClient.getAuthentication("bearerAuth");
    bearerAuth.setBearerToken("BEARER TOKEN");

    OrganizationManagementApi apiInstance = new OrganizationManagementApi(defaultClient);
    String organizationId = "organizationId_example"; // String | 
    UpdateOrganizationRequest updateOrganizationRequest = new UpdateOrganizationRequest(); // UpdateOrganizationRequest | Payload to update organization
    try {
      Organization result = apiInstance.updateOrganization(organizationId, updateOrganizationRequest);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling OrganizationManagementApi#updateOrganization");
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
 **updateOrganizationRequest** | [**UpdateOrganizationRequest**](UpdateOrganizationRequest.md)| Payload to update organization |

### Return type

[**Organization**](Organization.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
**200** | Response for Get and Update Organization requests |  -  |
**400** | Error response |  -  |
**401** | Error response |  -  |
**403** | Error response |  -  |
**404** | Error response |  -  |
**429** | Error response |  -  |
**0** | Error response |  -  |

