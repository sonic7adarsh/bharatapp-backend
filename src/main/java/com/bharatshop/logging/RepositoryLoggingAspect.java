package com.bharatshop.logging;

import com.bharatshop.entity.ProductEntity;
import com.bharatshop.entity.StoreEntity;
import com.bharatshop.tenant.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Aspect
@Component
public class RepositoryLoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(RepositoryLoggingAspect.class);

    @Around("execution(* com.bharatshop.repository.StoreRepository.findByTenantId(..))")
    public Object logStoreRepo(ProceedingJoinPoint pjp) throws Throwable {
        String tenant = TenantContext.getTenant();
        Object result = pjp.proceed();
        if (result instanceof List<?>) {
            List<?> list = (List<?>) result;
            List<String> ids = list.stream()
                    .filter(StoreEntity.class::isInstance)
                    .map(StoreEntity.class::cast)
                    .map(StoreEntity::getId)
                    .collect(Collectors.toList());
            log.info("[REPO_CALL] table=stores tenant={} ids={}", tenant, ids);
        } else {
            log.info("[REPO_CALL] table=stores tenant={} ids=(non-list)", tenant);
        }
        return result;
    }

    @Around("execution(* com.bharatshop.repository.ProductRepository.findByStoreIdAndTenantId(..))")
    public Object logProductRepo(ProceedingJoinPoint pjp) throws Throwable {
        String tenant = TenantContext.getTenant();
        Object result = pjp.proceed();
        if (result instanceof List<?>) {
            List<?> list = (List<?>) result;
            List<String> ids = list.stream()
                    .filter(ProductEntity.class::isInstance)
                    .map(ProductEntity.class::cast)
                    .map(ProductEntity::getId)
                    .collect(Collectors.toList());
            log.info("[REPO_CALL] table=products tenant={} ids={}", tenant, ids);
        } else {
            log.info("[REPO_CALL] table=products tenant={} ids=(non-list)", tenant);
        }
        return result;
    }
}