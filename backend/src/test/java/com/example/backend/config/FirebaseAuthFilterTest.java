package com.example.backend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class FirebaseAuthFilterTest {

    @Test
    void testMissingAuthHeaderReturns401() throws ServletException, IOException {
        FirebaseAuthFilter filter = new FirebaseAuthFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(filterChain);
    }

    @Test
    void testInvalidTokenReturns401() throws ServletException, IOException {
        FirebaseAuthFilter filter = new FirebaseAuthFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // FirebaseAuth.getInstance() will throw an exception since it's not initialized,
        // which simulates a token verification failure.
        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(filterChain);
    }

    @Test
    void testApplicationExceptionNotCaughtAs401() throws Exception {
        FirebaseAuthFilter filter = new FirebaseAuthFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        FilterChain filterChain = mock(FilterChain.class);
        doThrow(new ServletException("Application error")).when(filterChain).doFilter(request, response);

        FirebaseToken mockToken = mock(FirebaseToken.class);
        when(mockToken.getUid()).thenReturn("user123");

        FirebaseAuth mockAuth = mock(FirebaseAuth.class);
        when(mockAuth.verifyIdToken("valid-token")).thenReturn(mockToken);

        try (MockedStatic<FirebaseAuth> mockedStatic = Mockito.mockStatic(FirebaseAuth.class)) {
            mockedStatic.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

            try {
                filter.doFilterInternal(request, response, filterChain);
            } catch (ServletException e) {
                assertEquals("Application error", e.getMessage());
            }

            // The filter should NOT have set the status to 401
            assertEquals(200, response.getStatus()); // Default status
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }
}
