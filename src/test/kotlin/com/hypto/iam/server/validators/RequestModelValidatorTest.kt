package com.hypto.iam.server.validators

import com.hypto.iam.server.models.CreateCredentialRequest
import com.hypto.iam.server.models.UpdateCredentialRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class RequestModelValidatorTest {

    //  CreateCredentialRequest validations
    @Test
    fun `CreateCredentialRequest - valid - without validity `() {
        val req = CreateCredentialRequest()
        Assertions.assertInstanceOf(CreateCredentialRequest::class.java, req.validate())
    }

    @Test
    fun `CreateCredentialRequest - valid - with validity `() {
        val req = CreateCredentialRequest(LocalDateTime.MAX.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        Assertions.assertInstanceOf(CreateCredentialRequest::class.java, req.validate())
    }

    @Test
    fun `CreateCredentialRequest - invalid - validity in the past`() {
        val req = CreateCredentialRequest(LocalDateTime.MIN.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            req.validate()
        }
    }

    @Test
    fun `CreateCredentialRequest - invalid - invalid validity string format`() {
        val req = CreateCredentialRequest("not a valid datetime")
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            req.validate()
        }
    }

    // UpdateCredentialRequest validations

    @Test
    fun `UpdateCredentialRequest - valid - with only status `() {
        val req = UpdateCredentialRequest(status = UpdateCredentialRequest.Status.active)
        Assertions.assertInstanceOf(UpdateCredentialRequest::class.java, req.validate())
    }

    @Test
    fun `UpdateCredentialRequest - valid - with only validity `() {
        val req = UpdateCredentialRequest(LocalDateTime.MAX.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        Assertions.assertInstanceOf(UpdateCredentialRequest::class.java, req.validate())
    }

    @Test
    fun `UpdateCredentialRequest - valid - with both status and validity `() {
        val req = UpdateCredentialRequest(
            LocalDateTime.MAX.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            UpdateCredentialRequest.Status.inactive
        )
        Assertions.assertInstanceOf(UpdateCredentialRequest::class.java, req.validate())
    }

    @Test
    fun `UpdateCredentialRequest - invalid - without status and validity `() {
        val req = UpdateCredentialRequest()
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            Assertions.assertInstanceOf(UpdateCredentialRequest::class.java, req.validate())
        }
    }

    @Test
    fun `UpdateCredentialRequest - invalid - validity in the past`() {
        val req = UpdateCredentialRequest(LocalDateTime.MIN.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            req.validate()
        }
    }

    @Test
    fun `UpdateCredentialRequest - invalid - invalid validity string format`() {
        val req = UpdateCredentialRequest("not a valid datetime")
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            req.validate()
        }
    }
}
