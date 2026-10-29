package com.aloute.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String root() {
	    Authentication auth =
	            SecurityContextHolder.getContext().getAuthentication();

	    if (auth != null
	            && auth.isAuthenticated()
	            && !(auth.getPrincipal() instanceof String principal
	                 && principal.equals("anonymousUser"))) {
	        return "redirect:/home";
	    }

	    return "redirect:/login";
	}

    @GetMapping("/home")
    public String home() {
        return "user/home";
    }
}