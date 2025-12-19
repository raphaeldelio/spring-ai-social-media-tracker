package com.redis.socialmediatracker.slack;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter that wraps Slack requests to cache the request body.
 * This allows the body to be read multiple times for signature verification
 * and request processing.
 */
@Component
public class SlackRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest) {
            String path = httpRequest.getRequestURI();
            
            // Only wrap requests to Slack endpoints
            if (path.startsWith("/slack/events")) {
                CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
                chain.doFilter(cachedRequest, response);
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
}

