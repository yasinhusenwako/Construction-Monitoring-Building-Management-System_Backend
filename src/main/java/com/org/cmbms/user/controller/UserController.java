
package com.org.cmbms.user.controller;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.security.SecurityUtils;
import com.org.cmbms.user.dto.UserUpdateRequest;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> all() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(userService.allUsers(currentUser));
    }

    @GetMapping("/me")
    public ResponseEntity<User> me() {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(userService.currentUser(currentUser));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<User> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(userService.updateUser(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        UserPrincipal currentUser = SecurityUtils.getCurrentUser();
        userService.deleteUser(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
