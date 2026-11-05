package team.projectpulse.security.authorizationmanagers;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import team.projectpulse.student.StudentSecurityService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * BR-student-lifecycle's route rule: the course admin of the student's course, or an instructor of her course
 * section, and no one else.
 */
@ExtendWith(MockitoExtension.class)
class StudentLifecycleAuthorizationManagerTest {

    @Mock
    StudentSecurityService studentSecurityService;

    @InjectMocks
    StudentLifecycleAuthorizationManager manager;


    private AuthorizationResult authorize(Map<String, String> variables) {
        return this.manager.authorize(() -> null, new RequestAuthorizationContext(mock(HttpServletRequest.class), variables));
    }

    @Test
    @DisplayName("Grants the course admin of the student's course, without asking about course-section assignment")
    void grantsCourseAdmin() {
        given(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).willReturn(true);

        assertThat(authorize(Map.of("studentId", "4")).isGranted()).isTrue();
    }

    @Test
    @DisplayName("Grants an instructor assigned to the student's course section")
    void grantsSectionInstructor() {
        given(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).willReturn(false);
        given(this.studentSecurityService.isCurrentUserInstructorOfStudentSection(4)).willReturn(true);

        assertThat(authorize(Map.of("studentId", "4")).isGranted()).isTrue();
    }

    @Test
    @DisplayName("Denies a caller who is neither, the student herself included")
    void deniesEveryoneElse() {
        given(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(4)).willReturn(false);
        given(this.studentSecurityService.isCurrentUserInstructorOfStudentSection(4)).willReturn(false);

        assertThat(authorize(Map.of("studentId", "4")).isGranted()).isFalse();
    }

    @Test
    @DisplayName("Denies when the matched route bound no usable studentId")
    void deniesUnresolvableId() {
        assertThat(authorize(Map.of()).isGranted()).isFalse();
        assertThat(authorize(Map.of("studentId", "abc")).isGranted()).isFalse();
        verifyNoInteractions(this.studentSecurityService);
    }

}
