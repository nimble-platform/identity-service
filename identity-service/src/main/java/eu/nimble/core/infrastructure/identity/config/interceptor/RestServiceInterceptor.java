package eu.nimble.core.infrastructure.identity.config.interceptor;

import eu.nimble.utility.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class RestServiceInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private ExecutionContext executionContext;

    @Override
    public boolean preHandle (HttpServletRequest request, HttpServletResponse response, Object handler) {

        // set language id
        executionContext.setLanguageId(request.getLocale().getLanguage());
        return true;

    }


}
