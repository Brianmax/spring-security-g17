package com.jwt.codigo.service;

import com.jwt.codigo.dto.user.CreateUserRequest;
import com.jwt.codigo.dto.user.UpdateUserRequest;
import com.jwt.codigo.dto.user.UserResponse;
import com.jwt.codigo.entity.UserEntity;
import com.jwt.codigo.enums.UserStatus;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.mapper.UserMapper;
import com.jwt.codigo.repository.BankAccountRepository;
import com.jwt.codigo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BankAccountRepository accountRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, accountRepository, new UserMapper());
    }

    @Test
    void createsUserWithNormalizedEmail() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.create(new CreateUserRequest(" Ada ", " Lovelace ", " ADA@Example.com "));

        assertThat(response.firstName()).isEqualTo("Ada");
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updatesUser() {
        UserEntity existing = new UserEntity("Ada", "Lovelace", "ada@example.com");
        when(userRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("ada.byron@example.com", existing.getId())).thenReturn(false);
        when(userRepository.saveAndFlush(existing)).thenReturn(existing);

        UserResponse response = userService.update(
                existing.getId(),
                new UpdateUserRequest("Ada", "Byron", "ada.byron@example.com", UserStatus.INACTIVE)
        );

        assertThat(response.lastName()).isEqualTo("Byron");
        assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
    }

    @Test
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(
                new CreateUserRequest("Ada", "Lovelace", "ada@example.com")
        )).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo("DUPLICATE_EMAIL");
            assertThat(exception.getStatus().value()).isEqualTo(409);
        });
    }
}
