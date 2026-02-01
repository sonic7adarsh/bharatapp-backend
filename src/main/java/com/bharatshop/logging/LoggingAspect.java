package com.bharatshop.logging;

import com.bharatshop.security.UserPrincipal;
import com.bharatshop.tenant.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
public class LoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("within(com.bharatshop.web..*) && (@within(org.springframework.web.bind.annotation.RestController) || @within(org.springframework.stereotype.Controller))")
    public Object logController(ProceedingJoinPoint pjp) throws Throwable {
        return logAround("controller", pjp);
    }

    @Around("within(com.bharatshop.service..*)")
    public Object logService(ProceedingJoinPoint pjp) throws Throwable {
        return logAround("service", pjp);
    }

    private Object logAround(String layer, ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        String cls = sig.getDeclaringType().getSimpleName();
        String method = sig.getName();
        String tenant = TenantContext.getTenant();
        String userId = null;
        UserPrincipal up = null;
        try { up = UserPrincipal.current(); } catch (Exception ignored) {}
        if (up != null) userId = up.getUserId();

        String args = ArgSanitizer.sanitizeArgs(pjp.getArgs());
        log.info("{} enter: {}.{} tenant={} userId={} args=[{}]", layer, cls, method, tenant, userId, args);
        try {
            Object result = pjp.proceed();
            long durMs = (System.nanoTime() - start) / 1_000_000;
            String res = ArgSanitizer.sanitize(result);
            log.info("{} exit: {}.{} tenant={} userId={} took={}ms result={}", layer, cls, method, tenant, userId, durMs, res);
            return result;
        } catch (Throwable t) {
            long durMs = (System.nanoTime() - start) / 1_000_000;
            log.error("{} error: {}.{} tenant={} userId={} took={}ms error={}", layer, cls, method, tenant, userId, durMs, t.getMessage());
            throw t;
        }
    }
}