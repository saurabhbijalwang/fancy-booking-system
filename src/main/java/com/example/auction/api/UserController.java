package com.example.auction.api;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.auction.api.ApiModels.CreateUsersRequest;
import com.example.auction.api.ApiModels.UserBatchResponse;
import com.example.auction.api.ApiModels.UserResponse;
import com.example.auction.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserBatchResponse createUsers(@Valid @RequestBody CreateUsersRequest request) {
        return userService.createUsers(request.count());
    }

    @GetMapping
    public List<UserResponse> listUsers(@RequestParam(defaultValue = "100") int limit) {
        return userService.listUsers(limit);
    }
}
