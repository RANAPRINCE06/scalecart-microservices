package com.nahid.product.aspects;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(com.nahid.product.controller..*) ")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String methodName = joinPoint.getSignature().toShortString();
        HttpServletRequest request = getCurrentRequest();

        String httpMethod = getNonBlankValue(request != null ? request.getMethod() : null);
        String requestUri = getNonBlankValue(request != null ? request.getRequestURI() : null);

        log.info("➡ {} {} | Entering {} with args {}", httpMethod, requestUri, methodName, formatArguments(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            long timeTaken = System.currentTimeMillis() - start;

            log.info("⬅ {} {} | Exiting {} | ExecutionTime={}ms", httpMethod, requestUri, methodName, timeTaken);

            return result;
        } catch (Exception e) {
            log.error("❌ {} {} | Exception in {} | Message={}", httpMethod, requestUri, methodName, e.getMessage(), e);
            throw e;
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (attrs != null) ? attrs.getRequest() : null;
    }

    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            builder.append(safeToString(args[i]));
            if (i < args.length - 1) {
                builder.append(", ");
            }
        }
        builder.append(']');
        return builder.toString();
    }

    private String safeToString(Object value) {
        if (value == null) {
            return "null";
        }

        String text = String.valueOf(value);
        if (text.length() > 255) {
            return text.substring(0, 252) + "...";
        }
        return text;
    }

    private String getNonBlankValue(String value) {
        return (value != null && !value.isBlank()) ? value : "N/A";
    }
}

