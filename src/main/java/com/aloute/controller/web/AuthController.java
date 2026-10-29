package com.aloute.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller("webAuthController")
@RequestMapping
public class AuthController {

    @GetMapping("/login")
    public String login() { return "auth/login"; }

    @GetMapping("/register")
    public String register() { return "auth/register"; }

    @GetMapping("/forgot-password")
    public String forgot() { return "auth/forgot-password"; }

    @GetMapping("/reset-password")
    public String reset() { return "auth/reset-password"; }
}