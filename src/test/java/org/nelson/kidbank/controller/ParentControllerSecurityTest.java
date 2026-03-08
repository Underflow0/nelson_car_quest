package org.nelson.kidbank.controller;

import org.junit.jupiter.api.Test;
import org.nelson.kidbank.config.*;
import org.nelson.kidbank.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ParentController.class)
@Import({SecurityConfig.class, CustomUserDetailsService.class,
         LoginSuccessHandler.class, LoginFailureHandler.class,
         GlobalExceptionHandler.class})
class ParentControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserService userService;
    @MockBean AccountService accountService;
    @MockBean org.nelson.kidbank.repository.UserRepository userRepository;

    @Test
    void unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/parent/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "child1", roles = {"CHILD"})
    void childUser_postToDeposit_returns403() throws Exception {
        mockMvc.perform(post("/parent/accounts/1/deposit")
                        .param("amount", "10.00")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "child1", roles = {"CHILD"})
    void childUser_postToWithdraw_returns403() throws Exception {
        mockMvc.perform(post("/parent/accounts/1/withdraw")
                        .param("amount", "10.00")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "child1", roles = {"CHILD"})
    void childUser_postToInterestRate_returns403() throws Exception {
        mockMvc.perform(post("/parent/accounts/1/interest-rate")
                        .param("rate", "0.05")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}
