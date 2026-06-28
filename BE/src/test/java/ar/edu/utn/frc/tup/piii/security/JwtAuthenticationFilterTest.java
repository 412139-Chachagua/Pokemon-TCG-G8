package ar.edu.utn.frc.tup.piii.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Test
    void validToken_setsAuthenticationInSecurityContext() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String token = "valid-token";
        JwtUserDetails userDetails = new JwtUserDetails(userId, "USER", playerId);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserDetailsFromToken(token)).thenReturn(userDetails);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());
        assertEquals(userDetails, authentication.getPrincipal());
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_USER")), authentication.getAuthorities());

        SecurityContextHolder.clearContext();
    }

    @Test
    void invalidToken_noAuthenticationSet() throws ServletException, IOException {
        String token = "invalid-token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        SecurityContextHolder.clearContext();
    }

    @Test
    void noAuthorizationHeader_noAuthenticationSet() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        SecurityContextHolder.clearContext();
    }

    @Test
    void emptyBearerToken_noAuthenticationSet() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNull(authentication);

        SecurityContextHolder.clearContext();
    }

    @Test
    void optionsRequest_shouldNotFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");

        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void getRequest_shouldNotFilterOptions() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");

        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void tokenWithValidUserDetails_setsCorrectAuthorities() throws ServletException, IOException {
        UUID userId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String token = "admin-token";
        JwtUserDetails userDetails = new JwtUserDetails(userId, "ADMIN", playerId);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserDetailsFromToken(token)).thenReturn(userDetails);

        filter.doFilterInternal(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(1, authentication.getAuthorities().size());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));

        SecurityContextHolder.clearContext();
    }
}
