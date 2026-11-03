package team.projectpulse.security.authorizationmanagers;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import team.projectpulse.student.StudentSecurityService;

import java.util.function.Supplier;

/**
 * Guards changing a student's account state (deactivating or reactivating her), admitting the course admin of her
 * course or an instructor assigned to her course section, per BR-student-lifecycle.
 *
 * <p>There is no self clause and nothing is granted by role alone: a student may never change her own account
 * state, and a course admin of another course is refused. Reads {@code studentId} and delegates to
 * {@link StudentSecurityService}; an id naming no student is denied.
 */
@Component
public class StudentLifecycleAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final StudentSecurityService studentSecurityService;


    public StudentLifecycleAuthorizationManager(StudentSecurityService studentSecurityService) {
        this.studentSecurityService = studentSecurityService;
    }

    @Override
    public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
        Integer studentId = PathVariables.readId(context, "studentId");
        if (studentId == null) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(
                this.studentSecurityService.isCurrentUserAdminOfStudentCourse(studentId)
                        || this.studentSecurityService.isCurrentUserInstructorOfStudentSection(studentId));
    }

}
