package com.aloute;

import com.aloute.controller.web.HomeController;
import com.aloute.controller.web.ProfileController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebPageRoutingTests {

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void homeRouteUsesTheExistingUserTemplate() {
        String view = new HomeController().home();
        assertEquals("user/home", view);
        assertNotNull(getClass().getClassLoader().getResource("templates/" + view + ".html"));
    }

    @Test
    void rootRedirectsAnonymousVisitorsToLogin() {
        SecurityContextHolder.clearContext();

        assertEquals("redirect:/login", new HomeController().root());
    }

    @Test
    void rootRedirectsAuthenticatedVisitorsToHome() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("user@example.test", "password", "ROLE_USER"));

        assertEquals("redirect:/home", new HomeController().root());
    }

    @Test
    void friendRequestPageUsesTheFriendsTemplate() {
        assertEquals("user/friends", new ProfileController().friendRequests());
    }
}
