package com.ethara.taskmanager.repository;

import com.ethara.taskmanager.entity.User;
import com.ethara.taskmanager.entity.enums.Role;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    List<User> findByIdIn(Collection<Long> ids);

    List<User> findAllByOrderByFullNameAscIdAsc();
}
