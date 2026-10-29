package com.aloute.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {
    @GetMapping("/admin")
    public String dashboard() { return "admin/dashboard"; }

    @GetMapping("/admin/users")
    public String users() { return "admin/user-manage"; }

    @GetMapping("/admin/posts")
    public String posts() { return "admin/post-manage"; }

    @GetMapping("/admin/system")
    public String system() { return "admin/system"; }
}