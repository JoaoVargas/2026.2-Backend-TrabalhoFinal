package br.pucpr.alexandria.security

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status


@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    private fun login(email: String, password: String): String {
        val body = mockMvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"}""")
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        return JsonPath.read(body, "$.token")
    }

    @Test
    fun `orders are rejected for anonymous callers`() {
        mockMvc.perform(get("/orders"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `creating a role is rejected for anonymous callers`() {
        mockMvc.perform(
            post("/roles").contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"TEMP","description":"temp"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `logging in with the seeded admin issues a JWT that unlocks orders and role creation`() {
        val token = login("admin@alexandria.com", "admin123")

        mockMvc.perform(get("/orders").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/roles").header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"TEMP_ADMIN_ROLE","description":"temp"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("TEMP_ADMIN_ROLE"))
    }

    @Test
    fun `logging in with the wrong password is rejected`() {
        mockMvc.perform(
            post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"admin@alexandria.com","password":"wrong-password"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `a freshly registered user has no role and cannot create roles`() {
        mockMvc.perform(
            post("/users").contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"plain-user@mail.com","password":"secret123","name":"Plain"}""")
        )
            .andExpect(status().isCreated)

        val token = login("plain-user@mail.com", "secret123")

        mockMvc.perform(get("/orders").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)

        mockMvc.perform(
            post("/roles").header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"HACK_ROLE","description":"x"}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `a request with a garbage bearer token is treated as unauthenticated`() {
        mockMvc.perform(get("/orders").header("Authorization", "Bearer not-a-real-jwt"))
            .andExpect(status().isUnauthorized)
    }
}
