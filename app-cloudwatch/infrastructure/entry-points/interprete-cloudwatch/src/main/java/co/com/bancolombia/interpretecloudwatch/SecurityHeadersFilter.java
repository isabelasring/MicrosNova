package co.com.bancolombia.interpretecloudwatch;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class SecurityHeadersFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        response.setHeader(
                "Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload"
        );
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader(
                "Cache-Control",
                "no-store, no-cache, must-revalidate, proxy-revalidate"
        );
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        filterChain.doFilter(request, response);
    }
}
