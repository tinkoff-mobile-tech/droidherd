/*
 * Kubernetes
 * No description provided (generated by Openapi Generator https://github.com/openapitools/openapi-generator)
 *
 * The version of the OpenAPI document: v1.21.1
 *
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package ru.tinkoff.testops.droidherd.models;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import java.util.Objects;

/**
 * V1DroidherdSessionSpecClientCi
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2022-07-17T14:14:44.512Z[Etc/UTC]")
public class V1DroidherdSessionSpecClientCi {
  public static final String SERIALIZED_NAME_JOB_URL = "jobUrl";
  @SerializedName(SERIALIZED_NAME_JOB_URL)
  private String jobUrl;

  public static final String SERIALIZED_NAME_NAME = "name";
  @SerializedName(SERIALIZED_NAME_NAME)
  private String name;

  public static final String SERIALIZED_NAME_REFERENCE = "reference";
  @SerializedName(SERIALIZED_NAME_REFERENCE)
  private String reference;

  public static final String SERIALIZED_NAME_REPOSITORY = "repository";
  @SerializedName(SERIALIZED_NAME_REPOSITORY)
  private String repository;

  public static final String SERIALIZED_NAME_TRIGGERED_BY = "triggeredBy";
  @SerializedName(SERIALIZED_NAME_TRIGGERED_BY)
  private String triggeredBy;


  public V1DroidherdSessionSpecClientCi jobUrl(String jobUrl) {

    this.jobUrl = jobUrl;
    return this;
  }

   /**
   * Get jobUrl
   * @return jobUrl
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getJobUrl() {
    return jobUrl;
  }


  public void setJobUrl(String jobUrl) {
    this.jobUrl = jobUrl;
  }


  public V1DroidherdSessionSpecClientCi name(String name) {

    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public V1DroidherdSessionSpecClientCi reference(String reference) {

    this.reference = reference;
    return this;
  }

   /**
   * Get reference
   * @return reference
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getReference() {
    return reference;
  }


  public void setReference(String reference) {
    this.reference = reference;
  }


  public V1DroidherdSessionSpecClientCi repository(String repository) {

    this.repository = repository;
    return this;
  }

   /**
   * Get repository
   * @return repository
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getRepository() {
    return repository;
  }


  public void setRepository(String repository) {
    this.repository = repository;
  }


  public V1DroidherdSessionSpecClientCi triggeredBy(String triggeredBy) {

    this.triggeredBy = triggeredBy;
    return this;
  }

   /**
   * Get triggeredBy
   * @return triggeredBy
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getTriggeredBy() {
    return triggeredBy;
  }


  public void setTriggeredBy(String triggeredBy) {
    this.triggeredBy = triggeredBy;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    V1DroidherdSessionSpecClientCi v1DroidherdSessionSpecClientCi = (V1DroidherdSessionSpecClientCi) o;
    return Objects.equals(this.jobUrl, v1DroidherdSessionSpecClientCi.jobUrl) &&
        Objects.equals(this.name, v1DroidherdSessionSpecClientCi.name) &&
        Objects.equals(this.reference, v1DroidherdSessionSpecClientCi.reference) &&
        Objects.equals(this.repository, v1DroidherdSessionSpecClientCi.repository) &&
        Objects.equals(this.triggeredBy, v1DroidherdSessionSpecClientCi.triggeredBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(jobUrl, name, reference, repository, triggeredBy);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class V1DroidherdSessionSpecClientCi {\n");
    sb.append("    jobUrl: ").append(toIndentedString(jobUrl)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    reference: ").append(toIndentedString(reference)).append("\n");
    sb.append("    repository: ").append(toIndentedString(repository)).append("\n");
    sb.append("    triggeredBy: ").append(toIndentedString(triggeredBy)).append("\n");
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

