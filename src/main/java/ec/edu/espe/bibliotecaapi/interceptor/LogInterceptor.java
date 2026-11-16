package ec.edu.espe.bibliotecaapi.interceptor; // Ajusta el nombre del paquete al de tu proyecto

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        System.out.println("➡️ [INTERCEPTOR] Petición HTTP recibida: " + request.getMethod() + " " + request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        System.out.println("⬅️ [INTERCEPTOR] Petición finalizada con estado: " + response.getStatus());
    }
}