package edu.se201.tracker;


import jakarta.validation.Valid;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final TeacherService teachers;
    private final SchoolService schools;
    private final AdmissionService admission;
    private final SettingsService settings;
    public AdminController(TeacherService teachers, SchoolService schools, AdmissionService admission, SettingsService settings) {
        this.teachers=teachers; this.schools=schools; this.admission=admission; this.settings=settings;
    }
    @GetMapping("/teachers") public String teachers(@RequestParam(defaultValue="") String q,
            @RequestParam(required=false) ApplicationStatus status, Model model) {
        model.addAttribute("teachers",teachers.search(q,status)); model.addAttribute("q",q); model.addAttribute("status",status);
        model.addAttribute("statuses",ApplicationStatus.values()); model.addAttribute("teacherCount",teachers.count());
        model.addAttribute("schoolCount",schools.findAll().size()); model.addAttribute("pendingCount",teachers.pendingCount()); return "admin-teachers";
    }
    @GetMapping("/teachers/new") public String newTeacher(Model model) {
        model.addAttribute("form",new Forms.Registration()); model.addAttribute("schools",schools.findAll()); return "admin-new";
    }
    @PostMapping("/teachers/new") public String newTeacher(@Valid @ModelAttribute("form") Forms.Registration form,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        validateSchool(form,errors);
        if (!errors.hasErrors()) {
            try { long id=teachers.register(form); redirect.addFlashAttribute("notice","Teacher account created with Pending status."); return "redirect:/admin/teachers/"+id; }
            catch (DataIntegrityViolationException e) { errors.reject("duplicate","Email already registered, or school no longer available."); }
        }
        form.setPassword(""); model.addAttribute("schools",schools.findAll()); return "admin-new";
    }
    @GetMapping("/teachers/{id}") public String detail(@PathVariable long id, Model model) {
        var teacher=teachers.findById(id); model.addAttribute("teacher",teacher);
        model.addAttribute("form",Forms.Profile.from(teacher));
        Forms.Review review=new Forms.Review(); review.setStatus(teacher.status()); review.setFeedback(teacher.feedback());
        model.addAttribute("review",review); details(model); return "admin-detail";
    }
    @PostMapping("/teachers/{id}") public String edit(@PathVariable long id,
            @Valid @ModelAttribute("form") Forms.Profile form, BindingResult errors, Model model, RedirectAttributes redirect) {
        validateSchool(form,errors);
        if (!errors.hasErrors()) {
            try { teachers.update(id,form); redirect.addFlashAttribute("notice","Teacher details saved."); return "redirect:/admin/teachers/"+id; }
            catch (DataIntegrityViolationException e) { errors.reject("duplicate","Email already registered, or school no longer available."); }
        }
        var teacher=teachers.findById(id); model.addAttribute("teacher",teacher);
        Forms.Review review=new Forms.Review(); review.setStatus(teacher.status()); review.setFeedback(teacher.feedback());
        model.addAttribute("review",review); details(model); return "admin-detail";
    }
    @PostMapping("/teachers/{id}/review") public String review(@PathVariable long id,
            @Valid @ModelAttribute("review") Forms.Review review, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            var teacher=teachers.findById(id); model.addAttribute("teacher",teacher); model.addAttribute("form",Forms.Profile.from(teacher));
            details(model); return "admin-detail";
        }
        teachers.review(id,review.getStatus(),review.getFeedback()); redirect.addFlashAttribute("notice","Application review saved.");
        return "redirect:/admin/teachers/"+id;
    }
    @PostMapping("/teachers/{id}/delete") public String delete(@PathVariable long id, RedirectAttributes redirect) {
        teachers.delete(id); admission.release(id); redirect.addFlashAttribute("notice","Teacher account deleted."); return "redirect:/admin/teachers";
    }
    @GetMapping("/schools") public String schools(Model model) {
        model.addAttribute("schools",schools.findAll()); model.addAttribute("form",new Forms.SchoolForm()); return "admin-schools";
    }
    @PostMapping("/schools") public String addSchool(@Valid @ModelAttribute("form") Forms.SchoolForm form,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) { model.addAttribute("schools",schools.findAll()); return "admin-schools"; }
        schools.add(form.getName(),form.getCity()); redirect.addFlashAttribute("notice","School added."); return "redirect:/admin/schools";
    }
    @GetMapping("/schools/{id}") public String school(@PathVariable long id, Model model) {
        var s=schools.findById(id); Forms.SchoolForm form=new Forms.SchoolForm(); form.setName(s.name()); form.setCity(s.city());
        model.addAttribute("form",form); model.addAttribute("schoolId",id); return "admin-school-edit";
    }
    @PostMapping("/schools/{id}") public String editSchool(@PathVariable long id,
            @Valid @ModelAttribute("form") Forms.SchoolForm form, BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) { model.addAttribute("schoolId",id); return "admin-school-edit"; }
        schools.update(id,form.getName(),form.getCity()); redirect.addFlashAttribute("notice","School saved."); return "redirect:/admin/schools";
    }
    @PostMapping("/schools/{id}/delete") public String deleteSchool(@PathVariable long id, RedirectAttributes redirect) {
        try { schools.delete(id); redirect.addFlashAttribute("notice","School deleted."); }
        catch (IllegalArgumentException | DataIntegrityViolationException e) { redirect.addFlashAttribute("problem","This school is used by teachers. Move them to another school before deleting it."); }
        return "redirect:/admin/schools";
    }
    @GetMapping("/admission") public String settings(Model model) {
        model.addAttribute("form",Forms.SettingsForm.from(settings.get())); queueModel(model); return "admin-admission";
    }
    @PostMapping("/admission") public String settings(@Valid @ModelAttribute("form") Forms.SettingsForm form,
            BindingResult errors, Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) { queueModel(model); return "admin-admission"; }
        admission.configure(new AdmissionSettings(form.getMaxActive(),form.getInactivitySeconds(),form.getQueueCapacity()));
        redirect.addFlashAttribute("notice","Waiting-room settings saved."); return "redirect:/admin/admission";
    }
    private void queueModel(Model model) {
        var snapshot=admission.snapshot();
        var byId=teachers.search("",null).stream().collect(Collectors.toMap(TeacherProfile::accountId,t -> t));
        model.addAttribute("queue",snapshot);
        model.addAttribute("activeTeachers",snapshot.active().stream().map(byId::get).filter(java.util.Objects::nonNull).toList());
        model.addAttribute("waitingTeachers",snapshot.waiting().stream().map(byId::get).filter(java.util.Objects::nonNull).toList());
    }
    private void details(Model model) { model.addAttribute("schools",schools.findAll()); model.addAttribute("statuses",ApplicationStatus.values()); }
    private void validateSchool(Forms.Profile form, BindingResult errors) {
        if (form.getSchoolId()!=null && schools.findAll().stream().noneMatch(s -> s.id()==form.getSchoolId()))
            errors.rejectValue("schoolId","invalid","Choose an existing school.");
    }
}
