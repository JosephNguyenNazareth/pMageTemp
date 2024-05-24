package com.pmsconnect.mage.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "api/pmage/user")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(path = "{userName}")
    public User getUser(@PathVariable("userName") String userName) {
        return userService.getUser(userName);
    }

    @PostMapping(path = "/add")
    public String addNewConnector(
            @RequestParam String userName,
            @RequestParam String password,
            @RequestParam String role) {
        return userService.addNewUser(userName, password, role);
    }
}
