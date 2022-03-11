package com.hypto.iam.server.service

import com.google.gson.Gson
import com.hypto.iam.server.db.repositories.OrganizationRepo
import com.hypto.iam.server.db.tables.pojos.Organizations
import com.hypto.iam.server.di.applicationModule
import com.hypto.iam.server.di.controllerModule
import com.hypto.iam.server.di.repositoryModule
import com.hypto.iam.server.models.AdminUser
import com.hypto.iam.server.utils.ApplicationIdUtil
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockkClass
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.test.junit5.AutoCloseKoinTest
import org.koin.test.junit5.KoinTestExtension
import org.koin.test.junit5.mock.MockProviderExtension
import org.koin.test.mock.declareMock

internal class OrganizationsServiceImplTest : AutoCloseKoinTest() {
    private val gson = Gson()

    @JvmField
    @RegisterExtension
    val koinTestExtension = KoinTestExtension.create {
        modules(repositoryModule, controllerModule, applicationModule)
    }

    @JvmField
    @RegisterExtension
    val koinMockProvider = MockProviderExtension.create { mockkClass(it) }

    @BeforeEach
    fun setUp() {
        declareMock<OrganizationRepo> {
            coEvery { this@declareMock.insert(any<Organizations>()) } just runs
            coEvery { this@declareMock.findById(any()) } coAnswers {
                Organizations(firstArg(),
                    "testName",
                    "",
                    "adminUser",
                    LocalDateTime.MAX, LocalDateTime.MAX)
            }
        }
        declareMock<ApplicationIdUtil.Generator> {
            coEvery { this@declareMock.organizationId() } returns "testId"
        }
    }

    @Test
    fun `create organization success test`() {
        val organizationServiceImpl: OrganizationsServiceImpl by inject()
        runBlocking {
            val org = organizationServiceImpl.createOrganization(
                "testName", "testDescription", AdminUser("testPassword",
                    "testEmail", "testPhone", "testUserName"))
            assertEquals("testName", org.name)
            assertEquals("", org.description)
            assertEquals("testId", org.id)
        }
        val orgRepo = get<OrganizationRepo>()
        val orgInputSlot = slot<Organizations>()
        verify(exactly = 1) { orgRepo.findById("testId") }
        verify(exactly = 1) {
            orgRepo.insert(capture(orgInputSlot))
        }
        val orgInput = orgInputSlot.captured
        assertEquals("testId", orgInput.id)
        assertEquals("testName", orgInput.name)
    }

    @Test
    fun `get organization success test`() {
        val organizationServiceImpl: OrganizationsServiceImpl by inject()
        runBlocking {
            val org = organizationServiceImpl.getOrganization("testId")
            assertEquals("testName", org.name)
            assertEquals("", org.description)
            assertEquals("testId", org.id)
        }
        val orgRepo = get<OrganizationRepo>()
        verify(exactly = 1) { orgRepo.findById("testId") }
    }

    @Test
    fun updateOrganization() {
    }

    @Test
    fun deleteOrganization() {
    }
}
