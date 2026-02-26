package eu.nimble.core.infrastructure.identity.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * CORS filter that runs before Spring Security (Ordered.HIGHEST_PRECEDENCE).
 *
 * WHY a custom filter instead of Spring's CorsFilter + WebMvcConfigurer:
 *   - Spring Security (order -100 in Spring Boot 1.4.x) runs before WebMvcConfigurer's
 *     CORS handling, so OPTIONS preflight requests get 401 with no CORS headers.
 *   - Spring's CorsFilter with UrlBasedCorsConfigurationSource throws NullPointerException
 *     in Spring 4.3.x when CorsConfiguration fields are not fully initialised.
 *   - This filter writes CORS headers directly — no Spring CORS machinery, no NPE risk.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // When the browser sends withCredentials:true, Access-Control-Allow-Origin
        // MUST be the exact requesting origin (not "*") and
        // Access-Control-Allow-Credentials:true MUST also be present.
        // Reflect the request Origin back; fall back to "*" for non-credentialed callers.
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            response.setHeader("Access-Control-Allow-Origin", "*");
        }

        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, X-Requested-With");
        response.setHeader("Access-Control-Expose-Headers", "Authorization");
        response.setHeader("Access-Control-Max-Age",        "3600");

        // Preflight: return 200 immediately so the browser can proceed
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
