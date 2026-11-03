package team.projectpulse.student;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import team.projectpulse.course.Course;
import team.projectpulse.instructor.Instructor;
import team.projectpulse.section.Section;
import team.projectpulse.system.UserUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * The course-admin half of BR-student-lifecycle's route rule. Like every check in {@link StudentSecurityService},
 * it fails closed.
 */
@ExtendWith(MockitoExtension.class)
class StudentSecurityServiceTest {

    @Mock
    StudentRepository studentRepository;
    @Mock
    UserUtils userUtils;

    @InjectMocks
    StudentSecurityService studentSecurityService;


    /** A student in a course section of a course administered by the instructor with {@code adminId}. */
    private static Student studentInCourseAdministeredBy(int adminId) {
        Instructor admin = new Instructor("admin", "Ada", "Admin", "admin@abc.edu", "123456", true, "admin instructor");
        admin.setId(adminId);
        Course course = new Course("Course", "Course description");
        course.setCourseAdmin(admin);
        // The course admin is deliberately not assigned to this course section
        Section section = new Section("Section", LocalDate.of(2023, 8, 14), LocalDate.of(2024, 4, 29), true, DayOfWeek.MONDAY, LocalTime.of(23, 59), DayOfWeek.TUESDAY, LocalTime.of(23, 59));
        course.addSection(section);
        Student student = new Student("s", "Sam", "Student", "s@abc.edu", "123456", true, "student");
        student.setId(10);
        section.addStudent(student);
        return student;
    }

    @Test
    @DisplayName("The course admin of the student's course is admitted, though not assigned to her course section")
    void admitsTheCourseAdmin() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(10)).willReturn(Optional.of(studentInCourseAdministeredBy(1)));

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(10)).isTrue();
    }

    @Test
    @DisplayName("The course admin of another course is denied")
    void deniesTheAdminOfAnotherCourse() {
        given(this.userUtils.getUserId()).willReturn(3);
        given(this.studentRepository.findById(10)).willReturn(Optional.of(studentInCourseAdministeredBy(1)));

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(10)).isFalse();
    }

    @Test
    @DisplayName("An id that names no student is a denial, not an exception")
    void deniesAnUnknownStudent() {
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(999999)).willReturn(Optional.empty());

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(999999)).isFalse();
    }

    @Test
    @DisplayName("A student with no course section is a denial, not an exception")
    void deniesAStudentWithNoSection() {
        Student student = new Student("s", "Sam", "Student", "s@abc.edu", "123456", true, "student");
        given(this.userUtils.getUserId()).willReturn(1);
        given(this.studentRepository.findById(10)).willReturn(Optional.of(student));

        assertThat(this.studentSecurityService.isCurrentUserAdminOfStudentCourse(10)).isFalse();
    }

}
