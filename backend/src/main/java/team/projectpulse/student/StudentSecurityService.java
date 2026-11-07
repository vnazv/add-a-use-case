package team.projectpulse.student;

import team.projectpulse.system.UserUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Answers the questions a route asks about one named student: is that student the caller herself
 * ({@code isStudentSelf}), and is the caller an instructor of that student's course section
 * ({@code isCurrentUserInstructorOfStudentSection}).
 *
 * <p>Every check fails closed: an id that names no row, or a link not yet established, is denied rather than
 * throwing. These methods run inside an {@code AuthorizationManager}, upstream of the {@code DispatcherServlet},
 * so an exception thrown here escapes the filter chain as a 500 that also tells the caller which ids exist.
 */
@Service
@Transactional
public class StudentSecurityService {

    private final StudentRepository studentRepository;
    private final UserUtils userUtils;


    public StudentSecurityService(StudentRepository studentRepository, UserUtils userUtils) {
        this.studentRepository = studentRepository;
        this.userUtils = userUtils;
    }

    public boolean isStudentSelf(Integer studentId) {
        Integer studentIdFromJwt = this.userUtils.getUserId();
        return studentId.equals(studentIdFromJwt);
    }

    /**
     * Check if the user is the instructor of the section that the student is in
     *
     * @param studentId the id of the student we need to access, this is NOT the current user's Id
     * @return
     */
    public boolean isCurrentUserInstructorOfStudentSection(Integer studentId) {
        Integer instructorIdFromJwt = this.userUtils.getUserId();
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return false;
        }
        return student.getSection().getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(instructorIdFromJwt));
    }

    /**
     * May the caller deactivate (or reactivate) this student? BR-student-lifecycle: a course admin of the student's
     * course, or an instructor assigned to her course section.
     *
     * <p>Unlike the checks above, an id that names no student is admitted for an instructor caller, so the service
     * answers 404 (UC-STU-deactivate-student extension 6a) instead of a misleading 403. A student caller is never
     * admitted.
     */
    public boolean isCurrentUserCourseAdminOrInstructorOfStudentSection(Integer studentId) {
        if (!this.userUtils.hasRole("ROLE_instructor")) {
            return false;
        }
        Integer callerId = this.userUtils.getUserId();
        Student student = this.studentRepository.findById(studentId).orElse(null);
        if (student == null) {
            return true;
        }
        if (student.getSection() == null) {
            return false;
        }
        boolean sectionInstructor = student.getSection().getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(callerId));
        boolean courseAdmin = student.getSection().getCourse() != null
                && student.getSection().getCourse().getCourseAdmin() != null
                && callerId.equals(student.getSection().getCourse().getCourseAdmin().getId());
        return sectionInstructor || courseAdmin;
    }

}
