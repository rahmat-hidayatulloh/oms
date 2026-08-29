package com.oms.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

/**
 * Test for {@link VersionController}.
 * Verifies API versioning functionality.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VersionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getVersionInfo_shouldReturnVersionInfo() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/version"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.application").value("OMS"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.latestVersion").value("v1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.availableVersions").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.deprecatedVersions").isArray());
    }

    @Test
    void getVersionInfo_shouldContainApiDocumentation() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/version"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.apiDocumentation").value("/swagger-ui/index.html"));
    }

    @Test
    void getVersionInfo_shouldContainServerTime() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/version"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.serverTime").isString());
    }

    @Test
    void getVersionInfo_shouldContainCurrentVersion() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/version"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.currentVersion").isString());
    }
}