
package com.org.cmbms.user.repository;

import com.org.cmbms.common.enums.Role;
import com.org.cmbms.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByRoleAndDivisionId(Role role, Long divisionId);
}
