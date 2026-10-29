package com.aloute.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfileController {
    @GetMapping("/myprofile")
    public String myProfile() { return "user/profile"; }

    @GetMapping("/profile")
    public String userProfile(@RequestParam(required = false) String email,
                              @RequestParam(required = false) Long id) {
        return "user/profile";
    }

    @GetMapping("/posts/{id}")
    public String postDetail(@PathVariable String id) { return "user/post-detail"; }

    @GetMapping("/friends")
    public String friends() { return "user/friends"; }

    @GetMapping("/friends/requests")
    public String friendRequests() { return "user/friends"; }

    @GetMapping("/search")
    public String search() { return "user/search"; }
}