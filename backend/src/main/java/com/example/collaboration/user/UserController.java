package com.example.collaboration.user;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    List<UserSummaryResponse> list() {
        return users.findAllByOrderByDisplayNameAsc().stream().map(UserSummaryResponse::from).toList();
    }
}

