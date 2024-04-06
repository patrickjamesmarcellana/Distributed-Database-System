package com.example.distributeddatabasesystem.controller;

import org.springframework.stereotype.*;
import org.springframework.web.bind.annotation.*;

@Controller
class MainController {

    @RequestMapping("/")
    public String main() {
        return "index";
    }
}
