package com.pmsconnect.mage.user;

import com.pmsconnect.mage.connector.Connector;
import com.pmsconnect.mage.utils.AppScore;
import com.pmsconnect.mage.utils.PMSScore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "pmage/user")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getUsers() {
        return userService.getUsers();
    }

    @GetMapping(path = "/{userName}")
    public User getUser(@PathVariable("userName") String userName) {
        return userService.getUser(userName);
    }

    @GetMapping(path = "/login")
    public User login(@RequestParam String userName,
                      @RequestParam String password) {
        return userService.login(userName, password);
    }

    @PostMapping(path = "/add")
    public String addNewConnector(
            @RequestParam String userName,
            @RequestParam String password,
            @RequestParam String role) {
        return userService.addNewUser(userName, password, role);
    }

    @GetMapping(path = "/{userName}/pms-info")
    public List<PMSScore> getUserCommonPMSInfo(@PathVariable("userName") String userName,
                                               @RequestParam String selectedPMS) {
        return userService.getUserCommonPMSInfo(userName, selectedPMS);
    }

    @GetMapping(path = "/{userName}/app-info")
    public List<AppScore> getUserCommonAppInfo(@PathVariable("userName") String userName,
                                               @RequestParam String selectedApp) {
        return userService.getUserCommonAppInfo(userName, selectedApp);
    }
}
