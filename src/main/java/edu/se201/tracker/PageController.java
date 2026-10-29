package edu.se201.tracker;


import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {
    private final TeacherService teachers;
    private final SchoolService schools;
    private final AdmissionService admission;
    public PageController(TeacherService teachers, SchoolService schools, AdmissionService admission) {
        this.teachers=teachers; this.schools=schools; this.admission=admission;
    }
    @GetMapping("/") public String index() { return "redirect:/login"; }
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/home") public String home(@AuthenticationPrincipal AccountPrincipal p) {
        return p.role()==Role.ADMIN ? "redirect:/admin/teachers" : "redirect:/teacher/dashboard";
    }
    @GetMapping("/register") public String register(Model model) {
        model.addAttribute("form",new Forms.Registration()); model.addAttribute("schools",schools.findAll()); return "register";
    }
    @PostMapping("/register") public String register(@Valid @ModelAttribute("form") Forms.Registration form,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        if (form.getSchoolId()!=null && schools.findAll().stream().noneMatch(s -> s.id()==form.getSchoolId()))
            errors.rejectValue("schoolId","invalid","Choose an existing school.");
        if (!errors.hasErrors()) {
            try { teachers.register(form); redirect.addFlashAttribute("notice","Account created. Your application is Pending. Please sign in."); return "redirect:/login"; }
            catch (DataIntegrityViolationException e) { errors.reject("duplicate","That email address is already registered, or the selected school was removed. Check your details."); }
        }
        form.setPassword(""); model.addAttribute("schools",schools.findAll()); return "register";
    }
    @GetMapping("/teacher/dashboard") public String dashboard(@AuthenticationPrincipal AccountPrincipal p, Model model) {
        model.addAttribute("teacher",teachers.findById(p.id())); return "dashboard";
    }
    @GetMapping("/teacher/profile") public String profile(@AuthenticationPrincipal AccountPrincipal p, Model model) {
        model.addAttribute("form",Forms.Profile.from(teachers.findById(p.id())));
        model.addAttribute("schools",schools.findAll()); return "profile";
    }
    @PostMapping("/teacher/profile") public String profile(@AuthenticationPrincipal AccountPrincipal p,
            @Valid @ModelAttribute("form") Forms.Profile form, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (form.getSchoolId()!=null && schools.findAll().stream().noneMatch(s -> s.id()==form.getSchoolId()))
            errors.rejectValue("schoolId","invalid","Choose an existing school.");
        if (!errors.hasErrors()) {
            try { teachers.update(p.id(),form); redirect.addFlashAttribute("notice","Your profile was saved."); return "redirect:/teacher/dashboard"; }
            catch (DataIntegrityViolationException e) { errors.reject("duplicate","That email is already registered, or the school is no longer available."); }
        }
        model.addAttribute("schools",schools.findAll()); return "profile";
    }
    @GetMapping("/waiting") public String waiting(@AuthenticationPrincipal AccountPrincipal p, Model model) {
        var result=admission.enter(p.id(),false);
        if (result.state()==AdmissionService.State.ACTIVE) return "redirect:/teacher/dashboard";
        model.addAttribute("admission",result); return "waiting";
    }
    @GetMapping("/admission/status") @ResponseBody public AdmissionService.Admission status(
            @AuthenticationPrincipal AccountPrincipal p, @RequestParam(defaultValue="false") boolean join) {
        return join ? admission.enter(p.id(),false) : admission.inspect(p.id());
    }
    @PostMapping("/admission/activity") @ResponseBody public org.springframework.http.ResponseEntity<Void> activity(@AuthenticationPrincipal AccountPrincipal p) {
        return org.springframework.http.ResponseEntity.status(admission.touch(p.id()) ? 204 : 409).build();
    }
}
