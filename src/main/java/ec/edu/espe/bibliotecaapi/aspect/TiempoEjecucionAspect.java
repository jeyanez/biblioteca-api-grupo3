package ec.edu.espe.bibliotecaapi.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TiempoEjecucionAspect {

    @Around("execution(* ec.edu.espe.bibliotecaapi.service..*(..))")
    public Object medirTiempoEjecucion(ProceedingJoinPoint joinPoint) throws Throwable {
        long inicio = System.currentTimeMillis();

        Object resultado = joinPoint.proceed(); // Ejecuta el método del Service

        long fin = System.currentTimeMillis();
        long tiempoTotal = fin - inicio;

        System.out.println("⏱️ [ASPECTO AOP] El método " + joinPoint.getSignature().getName() +
                " tardó " + tiempoTotal + " ms en ejecutarse.");

        return resultado;
    }
}
