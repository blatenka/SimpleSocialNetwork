package com.aloute;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PublicAndHomePageMvcTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void loginAndRegistrationUseOnlyThePublicHeader() throws Exception {
        for (String path : new String[]{"/login", "/register"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Aloute")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Register")))
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("id=\"navUserName\""))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("id=\"notifCount\""))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("> Home</a>"))))
                    .andExpect(content().string(org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("> Chat</a>"))));
        }
    }

    @Test
    void resetPasswordUsesTokenFromLinkWithoutShowingTokenInput() throws Exception {
        mockMvc.perform(get("/reset-password").param("token", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("type=\"hidden\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("<label>Token</label>"))));
        mockMvc.perform(get("/login").param("registered", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(
                                "Đăng ký thành công. Vui lòng đăng nhập"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Sai email hoặc mật khẩu.")));
    }

    @Test
    void authenticatedHomeRendersFeedLoadingErrorEmptyAndRetryStates() throws Exception {
        mockMvc.perform(get("/home").with(user("person@example.test")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("feedLoading")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("feedEmpty")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("feedError")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("retryFeed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/home-feed.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("home-sidebar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("createPostForm")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/home-post-composer.js")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Friends")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("Online Friends"))));
    }

    @Test
    void profileRendersMutualFriendsSection() throws Exception {
        MvcResult result = mockMvc.perform(get("/profile").with(user("person@example.test")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mutualFriendsCard")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mutualFriendsList")))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        int friendship = html.indexOf("id=\"friendshipCard\"");
        int mutualFriends = html.indexOf("id=\"mutualFriendsCard\"");
        int posts = html.indexOf("id=\"profilePosts\"");
        assertTrue(friendship >= 0 && friendship < posts);
        assertTrue(mutualFriends >= 0 && mutualFriends < posts);
    }

    @Test
    void adminPagesRequireAdministratorRoleAndRenderTheirTemplates() throws Exception {
        for (String path : new String[]{"/admin", "/admin/users", "/admin/posts", "/admin/system"}) {
            mockMvc.perform(get(path).with(user("admin@example.test").roles("ADMIN")))
                    .andExpect(status().isOk());
            mockMvc.perform(get(path).with(user("user@example.test").roles("USER")))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/login"));
        }
    }

    @Test
    void adminApiRejectsRegularUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(user("user@example.test").roles("USER")))
                .andExpect(status().isForbidden());
    }
}
