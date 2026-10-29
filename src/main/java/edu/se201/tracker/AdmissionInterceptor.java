package edu.se201.tracker;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdmissionInterceptor implements HandlerInterceptor {
    private final AdmissionService admission;
    private final TeacherService teachers;
    public AdmissionInterceptor(AdmissionService admission, TeacherService teachers) { this.admission=admission; this.teachers=teachers; }
    @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth==null || !(auth.getPrincipal() instanceof AccountPrincipal p)) return true;
        if (!teachers.exists(p.id())) {
            admission.release(p.id()); req.getSession().invalidate(); SecurityContextHolder.clearContext();
            res.sendRedirect("/login?deleted"); return false;
        }
        if (p.role()==Role.ADMIN || !req.getRequestURI().startsWith("/teacher/")) return true;
        if (req.getMethod().equals("GET")) {
            if (admission.enter(p.id(),true).state()==AdmissionService.State.ACTIVE) return true;
            res.sendRedirect("/waiting"); return false;
        }
        // An expired form submission must never be reported as saved, even if a place is now free.
        if (admission.touch(p.id())) return true;
        admission.enter(p.id(),false);
        res.setStatus(409); res.setContentType("text/html;charset=UTF-8");
        res.getWriter().write("<!doctype html><html lang='en'><meta charset='UTF-8'><meta name='viewport' content='width=device-width'><title>Profile not saved</title><link rel='stylesheet' href='/css/style.css'><main><h1>Your changes were not saved</h1><p>Your active place expired through inactivity. You are still signed in. Use Back to copy any unsaved work, then rejoin the waiting room and submit again.</p><a href='/waiting'>Rejoin the waiting room</a></main></html>");
        return false;
    }
}
