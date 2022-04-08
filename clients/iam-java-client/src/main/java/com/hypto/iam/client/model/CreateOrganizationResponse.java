/*
 * Hypto IAM
 * APIs for Hypto IAM Service.
 *
 * The version of the OpenAPI document: 1.0.0
 * Contact: engineering@hypto.in
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package com.hypto.iam.client.model;

import java.util.Objects;
import java.util.Arrays;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.hypto.iam.client.model.Credential;
import com.hypto.iam.client.model.Organization;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * CreateOrganizationResponse
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-04-08T16:17:19.306080+05:30[Asia/Kolkata]")
public class CreateOrganizationResponse {
  public static final String SERIALIZED_NAME_ORGANIZATION = "organization";
  @SerializedName(SERIALIZED_NAME_ORGANIZATION)
  private Organization organization;

  public static final String SERIALIZED_NAME_ADMIN_USER_CREDENTIAL = "adminUserCredential";
  @SerializedName(SERIALIZED_NAME_ADMIN_USER_CREDENTIAL)
  private Credential adminUserCredential;

  public CreateOrganizationResponse() { 
  }

  public CreateOrganizationResponse organization(Organization organization) {
    
    this.organization = organization;
    return this;
  }

   /**
   * Get organization
   * @return organization
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Organization getOrganization() {
    return organization;
  }


  public void setOrganization(Organization organization) {
    this.organization = organization;
  }


  public CreateOrganizationResponse adminUserCredential(Credential adminUserCredential) {
    
    this.adminUserCredential = adminUserCredential;
    return this;
  }

   /**
   * Get adminUserCredential
   * @return adminUserCredential
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Credential getAdminUserCredential() {
    return adminUserCredential;
  }


  public void setAdminUserCredential(Credential adminUserCredential) {
    this.adminUserCredential = adminUserCredential;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateOrganizationResponse createOrganizationResponse = (CreateOrganizationResponse) o;
    return Objects.equals(this.organization, createOrganizationResponse.organization) &&
        Objects.equals(this.adminUserCredential, createOrganizationResponse.adminUserCredential);
  }

  @Override
  public int hashCode() {
    return Objects.hash(organization, adminUserCredential);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateOrganizationResponse {\n");
    sb.append("    organization: ").append(toIndentedString(organization)).append("\n");
    sb.append("    adminUserCredential: ").append(toIndentedString(adminUserCredential)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

