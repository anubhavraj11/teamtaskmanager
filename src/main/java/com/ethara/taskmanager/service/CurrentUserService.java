package com.ethara.taskmanager.service;

import com.ethara.taskmanager.entity.User;
import com.ethara.taskmanager.exception.ForbiddenException;
import com.ethara.taskmanager.repository.UserRepository;
import com.ethara.taskmanager.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new ForbiddenException("Current user could not be resolved");
        }

        return userRepository.findById(authenticatedUser.getId())
            .orElseThrow(() -> new ForbiddenException("Authenticated user no longer exists"));
    }
}
