package com.ethara.taskmanager.service;

import com.ethara.taskmanager.dto.auth.UserSummaryResponse;
import com.ethara.taskmanager.entity.User;
import com.ethara.taskmanager.entity.enums.Role;
import com.ethara.taskmanager.exception.ForbiddenException;
import com.ethara.taskmanager.repository.UserRepository;
import com.ethara.taskmanager.service.support.ResponseMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ResponseMapper responseMapper;

    public List<UserSummaryResponse> getAllUsers() {
        User currentUser = currentUserService.getCurrentUser();

        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to view all users");
        }

        return userRepository.findAllByOrderByFullNameAscIdAsc().stream()
            .map(responseMapper::toUserSummary)
            .toList();
    }

    public UserSummaryResponse getCurrentUserProfile() {
        return responseMapper.toUserSummary(currentUserService.getCurrentUser());
    }
}
