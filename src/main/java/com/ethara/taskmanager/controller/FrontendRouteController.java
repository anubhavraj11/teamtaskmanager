package com.ethara.taskmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendRouteController {

    @GetMapping({"/login", "/signup", "/admin-dashboard", "/member-dashboard"})
    public String forwardToSinglePageApplication() {
        return "forward:/index.html";
    }
}
