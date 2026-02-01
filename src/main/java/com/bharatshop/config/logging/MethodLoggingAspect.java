package com.bharatshop.config.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;

@Aspect
public class MethodLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(MethodLoggingAspect.class);

    @Around("within(com.bharatshop.web..*)")
    public Object logController(ProceedingJoinPoint pjp) throws Throwable {
        String signature = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Controller enter: {} args={}", signature, safeArgs(pjp.getArgs()));
        }
        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;
            if (result instanceof ResponseEntity<?> re) {
                if (log.isInfoEnabled()) {
                    log.info("Controller exit: {} -> status={} ({} ms)", signature, re.getStatusCode().value(), duration);
                }
            } else {
                if (log.isInfoEnabled()) {
                    log.info("Controller exit: {} ({} ms)", signature, duration);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Controller result: {} => {}", signature, summarize(result));
            }
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            log.error("Controller error: {} ({} ms) - {}", signature, duration, t.toString());
            throw t;
        }
    }

    @Around("within(com.bharatshop.service..*)")
    public Object logService(ProceedingJoinPoint pjp) throws Throwable {
        String signature = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Service enter: {} args={}", signature, safeArgs(pjp.getArgs()));
        }
        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;
            if (log.isInfoEnabled()) {
                log.info("Service exit: {} ({} ms)", signature, duration);
            }
            if (log.isDebugEnabled()) {
                log.debug("Service result: {} => {}", signature, summarize(result));
            }
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            log.error("Service error: {} ({} ms) - {}", signature, duration, t.toString());
            throw t;
        }
    }

    @Around("within(com.bharatshop.factory..*)")
    public Object logFactory(ProceedingJoinPoint pjp) throws Throwable {
        String signature = pjp.getSignature().toShortString();
        long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Factory enter: {} args={}", signature, safeArgs(pjp.getArgs()));
        }
        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - start;
            if (log.isInfoEnabled()) {
                log.info("Factory exit: {} ({} ms)", signature, duration);
            }
            if (log.isDebugEnabled()) {
                log.debug("Factory result: {} => {}", signature, summarize(result));
            }
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - start;
            log.error("Factory error: {} ({} ms) - {}", signature, duration, t.toString());
            throw t;
        }
    }

    @AfterThrowing(pointcut = "within(com.bharatshop.web..*) || within(com.bharatshop.service..*) || within(com.bharatshop.factory..*)", throwing = "ex")
    public void logExceptions(Throwable ex) {
        log.error("Unhandled exception: {}", ex.toString());
    }

    private String safeArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        String joined = Arrays.stream(args)
                .map(this::summarize)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        return "[" + joined + "]";
    }

    private String summarize(Object obj) {
        if (obj == null) return "null";
        try {
            if (obj instanceof CharSequence s) {
                return truncate(s.toString());
            }
            if (obj.getClass().isArray()) {
                Object[] arr = (Object[]) obj;
                return obj.getClass().getSimpleName() + "[len=" + arr.length + "]";
            }
            if (obj instanceof Collection<?> c) {
                return obj.getClass().getSimpleName() + "[size=" + c.size() + "]";
            }
            if (obj instanceof ResponseEntity<?> re) {
                return "ResponseEntity(status=" + re.getStatusCode().value() + ")";
            }
            String s = obj.toString();
            return obj.getClass().getSimpleName() + "(" + truncate(s) + ")";
        } catch (Exception e) {
            return obj.getClass().getSimpleName();
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        int max = 512;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}