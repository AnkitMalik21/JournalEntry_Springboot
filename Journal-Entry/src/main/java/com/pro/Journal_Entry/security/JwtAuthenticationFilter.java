package com.pro.Journal_Entry.security;

import com.pro.Journal_Entry.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1.Get Authorization header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 2. Check if header exists and start with Bearer
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }
        // 3. Extract token (remove "Bearer " prefix)
        jwt = authHeader.substring(7);
        username = jwtUtil.extractUsername(jwt);

        // 4. If username exists and user not already authenticated
        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
            // 5 Load user from database
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 6 validate token
            if(jwtUtil.validateToken(jwt, userDetails)){
                //7 create authentication object
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                //8 Set authentication in securityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // 9. Continue the filter chain
        filterChain.doFilter(request,response);
    }
}
