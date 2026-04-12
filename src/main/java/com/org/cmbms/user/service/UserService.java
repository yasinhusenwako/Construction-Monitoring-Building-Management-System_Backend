
package com.org.cmbms.user.service;

import com.org.cmbms.auth.security.UserPrincipal;
import com.org.cmbms.common.enums.Role;
import com.org.cmbms.common.exception.ApiException;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> allUsers(UserPrincipal currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ApiException("Access denied");
        }
        return userRepository.findAll();
    }
}
