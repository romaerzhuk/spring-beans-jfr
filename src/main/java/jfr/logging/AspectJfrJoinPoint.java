package jfr.logging;

import jfr.api.JfrJoinPoint;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.Advised;

import java.util.Arrays;
import java.util.List;

/**
 * Реализация {@link JfrJoinPoint} для {@link JoinPoint}.
 *
 * @param index     индекс фрейма стека вызовов
 * @param joinPoint точка вызова
 * @author Roman_Erzhukov
 */
record AspectJfrJoinPoint(int index, JoinPoint joinPoint) implements JfrJoinPoint {
    @Override
    public Class<?> targetClass() {
        Object target = joinPoint.getTarget();
        return !(target instanceof Advised) ? target.getClass()
                : ((Advised) target).getTargetSource().getTargetClass();
    }

    @Override
    public String name() {
        return joinPoint.getSignature()
                .getName();
    }

    @Override
    public Object method() {
        Signature signature = joinPoint.getSignature();
        return signature instanceof MethodSignature
                ? ((MethodSignature) signature).getMethod()
                : signature.getName();
    }

    @Override
    public List<Object> args() {
        return Arrays.asList(joinPoint.getArgs());
    }
}
