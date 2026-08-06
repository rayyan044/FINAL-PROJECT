package com.falconenergy.security;

import com.falconenergy.entity.Role;
import com.falconenergy.entity.User;
import com.falconenergy.entity.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabledAccountCannotAuthenticateWithAnExistingJwt() throws Exception {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
        User disabledDriver = User.builder()
                .email("driver@falconenergy.local")
                .password("encoded-password")
                .roleEntity(Role.builder().roleName("DRIVER").build())
                .status(UserStatus.INACTIVE)
                .build();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer existing-token");

        when(tokenProvider.extractUsername("existing-token")).thenReturn(disabledDriver.getEmail());
        when(userDetailsService.loadUserByUsername(disabledDriver.getEmail()))
                .thenReturn(new CustomUserDetails(disabledDriver));

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(tokenProvider, never()).isTokenValid(any(), any());
    }
}
