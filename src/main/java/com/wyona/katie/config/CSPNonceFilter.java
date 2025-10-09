package com.wyona.katie.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Add Content Security Policy with dynamically generated nonce
 */
public class CSPNonceFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        HttpServletRequest request= (HttpServletRequest) servletRequest;

        final String nonce = java.util.UUID.randomUUID().toString();
        //final String nonce = "0123456789";

        request.setAttribute("nonce", nonce); // INFO: Make the nonce available to other classes/scripts, e.g. IndexPageController.java (TODO: Replace "nonce" by global variable)

        // INFO: Blocks XSS, like for example <b onmouseover="alert('KABOOM!')">click me!</b> within an answer
        // INFO: Evaluate CSP with https://csp-evaluator.withgoogle.com/
        String cspHeaderName = "Content-Security-Policy";
        String cspHeaderValue = "default-src 'none'; " +
                "base-uri 'self';" +
                // TODO: Re-activate nonce, whereas add nonce dynamically to src/main/webapp/index.html using for example IndexPageController
                //"script-src 'self' 'nonce-" + nonce + "' 'unsafe-inline' 'unsafe-eval'; " +
                "script-src * 'unsafe-inline' 'unsafe-eval'; " +
                // INFO: iframe (https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/frame-src)
                "frame-src 'self'; " +
                //"frame-src 'self' *; " +
                // TODO: Verify which policies below make sense actually
                "font-src 'self' fonts.gstatic.com; " +
                //"font-src 'self'; " +
                "connect-src 'self' *; " +
                "img-src 'self' data:; " +
                "object-src 'self'; " +
                "media-src 'self' blob:; " +
                "style-src 'self' 'unsafe-inline' fonts.googleapis.com; " +
                //"style-src 'self' 'unsafe-inline'; " +
                "report-uri /api/v1/configuration/csp-violation-report";

        // INFO: Add Content-Security-Policy to response header
        response.setHeader(cspHeaderName, cspHeaderValue);

        String cspReportOnlyHeaderName = "Content-Security-Policy-Report-Only";
        String cspReportOnlyHeaderValue = "default-src 'none'; " +
                "script-src 'self' *; " +
                "frame-src 'self'; " +
                //"frame-src 'self' *; " +
                "font-src 'self'; " +
                "connect-src 'self' *; " +
                "img-src 'self' *; " +
                "object-src 'self'; " +
                "media-src 'self' blob:; " +
                "style-src 'self' 'unsafe-inline'; " +
                "report-uri /api/v1/configuration/csp-violation-report";

        //response.setHeader(cspReportOnlyHeaderName, cspReportOnlyHeaderValue);

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }
}
