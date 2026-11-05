package team.projectpulse.student;

import tools.jackson.databind.json.JsonMapper;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.course.Course;
import team.projectpulse.course.CourseRepository;
import team.projectpulse.section.Section;
import team.projectpulse.section.SectionRepository;
import team.projectpulse.system.StatusCode;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration tests for Student API endpoints")
class StudentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    String adminToken;

    String instructorBillToken;

    String studentJohnToken;

    String studentEricToken;

    String adminTimToken;

    @Autowired
    StudentRepository studentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    SectionRepository sectionRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;


    @BeforeEach
    void setUp() throws Exception {
        ResultActions resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.wei@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        MvcResult mvcResult = resultActions.andDo(print()).andReturn();
        String contentAsString = mvcResult.getResponse().getContentAsString();
        JSONObject json = new JSONObject(contentAsString);
        this.adminToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.gates@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.instructorBillToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.studentJohnToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("e.hudson@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.studentEricToken = "Bearer " + json.getJSONObject("data").getString("token");

        resultActions = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("t.cook@abc.edu", "123456"))); // httpBasic() is from spring-security-test.
        mvcResult = resultActions.andDo(print()).andReturn();
        contentAsString = mvcResult.getResponse().getContentAsString();
        json = new JSONObject(contentAsString);
        this.adminTimToken = "Bearer " + json.getJSONObject("data").getString("token");
    }

    @Test
    void testFindStudentsByCriteria() throws Exception {
        // Given
        Map<String, String> searchCriteria = new HashMap<>();
        searchCriteria.put("teamName", "Team1");
        String json = this.jsonMapper.writeValueAsString(searchCriteria);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("page", "0");
        requestParams.add("size", "30");
        requestParams.add("sort", "lastName,asc");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students/search").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find students successfully"))
                .andExpect(jsonPath("$.data.content", Matchers.hasSize(3)));
    }

    @Test
    void testInstructorBillFindStudentInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"));
    }

    @Test
    void testAdminTimFindStudentInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminTimToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    @Test
    void testStudentJohnFindOwnInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"));
    }

    @Test
    void testStudentJohnFindAnotherStudentsInfoById() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/students/5").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    @Test
    void testAddStudent() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "lucas");
        studentDto.put("firstName", "Lucas");
        studentDto.put("lastName", "Santos");
        studentDto.put("email", "l.santos@abc.edu");
        studentDto.put("password", "Abc123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "registrationToken");
        requestParams.add("role", "student");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Add student successfully"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.username").value("lucas"))
                .andExpect(jsonPath("$.data.firstName").value("Lucas"))
                .andExpect(jsonPath("$.data.lastName").value("Santos"))
                .andExpect(jsonPath("$.data.email").value("l.santos@abc.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void testAddStudentWithInvalidUserInvitation() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "marcos");
        studentDto.put("firstName", "Marcos");
        studentDto.put("lastName", "Grant");
        studentDto.put("email", "m.grant@abc.edu");
        studentDto.put("password", "123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "invalidRegistrationToken");
        requestParams.add("role", "student");

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("m.grant@abc.edu is not invited to register. Please contact the course admin."));
    }

    @Test
    void testAddStudentWithNoWrongUserInvitation() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "lucas");
        studentDto.put("firstName", "Lucas");
        studentDto.put("lastName", "Santos");
        studentDto.put("email", "l.santos@abc.edu");
        studentDto.put("password", "123456");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        MultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("courseId", "1");
        requestParams.add("sectionId", "2");
        requestParams.add("registrationToken", "registrationToken");
        requestParams.add("role", "instructor"); // This is wrong, Lucas should be a student.

        // When and then
        this.mockMvc.perform(post(this.baseUrl + "/students").contentType(MediaType.APPLICATION_JSON).content(json).params(requestParams).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.INVALID_ARGUMENT))
                .andExpect(jsonPath("$.message").value("You are not allowed to register as instructor for email: l.santos@abc.edu"));
    }

    @Test
    void testStudentJohnUpdatesOwnInfo() throws Exception {
        Map<String, Object> studentDto = new HashMap<>();
        studentDto.put("username", "john");
        studentDto.put("firstName", "John (updated)");
        studentDto.put("lastName", "Smith");
        studentDto.put("email", "j.smith@abc.edu");
        studentDto.put("enabled", false); // Student cannot change their enabled status.
        studentDto.put("roles", "instructor"); // Student cannot change their role.

        String json = this.jsonMapper.writeValueAsString(studentDto);

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/4").contentType(MediaType.APPLICATION_JSON).content(json).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Update student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.username").value("john"))
                .andExpect(jsonPath("$.data.firstName").value("John (updated)"))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("j.smith@abc.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.enabled").value(true)) // The enabled status should not be changed.
                .andExpect(jsonPath("$.data.roles").value("student")); // The role should not be changed.
    }

    @Test
    void testStudentJohnUpdatesAnotherStudentsInfo() throws Exception {
        Map<String, String> studentDto = new HashMap<>();
        studentDto.put("username", "eric");
        studentDto.put("firstName", "Eric (updated)");
        studentDto.put("lastName", "Hudson");
        studentDto.put("email", "e.hudson@abc.edu");

        String json = this.jsonMapper.writeValueAsString(studentDto);

        // When and then
        this.mockMvc.perform(put(this.baseUrl + "/students/5").contentType(MediaType.APPLICATION_JSON).content(json).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

//    @Test
//    void testAdminDeletesStudent() throws Exception {
//        this.mockMvc.perform(delete(this.baseUrl + "/students/3").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
//                .andExpect(jsonPath("$.flag").value(true))
//                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
//                .andExpect(jsonPath("$.message").value("Delete instructor successfully"));
//    }
//
//    @Test
//    void testStudentDeletesAnotherStudent() throws Exception {
//        this.mockMvc.perform(delete(this.baseUrl + "/students/4").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentToken))
//                .andExpect(jsonPath("$.flag").value(false))
//                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
//                .andExpect(jsonPath("$.message").value("No permission."))
//                .andExpect(jsonPath("$.data").value("Access Denied"));
//    }

    @Test
    void testStudentJohnFindStudentsInOwnTeam() throws Exception {
        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/1").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Find students by team id successfully"))
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)));
    }

    @Test
    void testStudentJohnFindStudentsInAnotherTeam() throws Exception {
        // When and then
        this.mockMvc.perform(get(this.baseUrl + "/students/teams/2").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN))
                .andExpect(jsonPath("$.message").value("No permission."))
                .andExpect(jsonPath("$.data").value("Access Denied"));
    }

    // UC-STU-deactivate-student. Every test runs in a transaction that rolls back, so the seed is left as found.
    // John (id 4) and Eric (id 5) are on Team1 in section 2 of course 1. Bingyang is course 1's admin and, with Bill,
    // assigned to section 2. Tim administers course 3 and teaches only its section, where Jana is a student.

    private String deactivateUrl(Integer studentId) {
        return this.baseUrl + "/students/" + studentId + "/deactivate";
    }

    private Integer idOf(String email) {
        return this.userRepository.findByEmail(email).orElseThrow().getId();
    }

    private boolean isEnabled(Integer studentId) {
        return this.studentRepository.findById(studentId).orElseThrow().isEnabled();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: the course admin deactivates a student, who can then no longer log in")
    void testCourseAdminDeactivatesStudent() throws Exception {
        this.mockMvc.perform(patch(deactivateUrl(4)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Deactivate student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.enabled").value(false));

        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: an instructor assigned to the student's course section deactivates her")
    void testSectionInstructorDeactivatesStudent() throws Exception {
        this.mockMvc.perform(patch(deactivateUrl(4)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value("Deactivate student successfully"))
                .andExpect(jsonPath("$.data.id").value(4))
                .andExpect(jsonPath("$.data.enabled").value(false));

        
        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("j.smith@abc.edu", "123456")))
                .andExpect(status().isUnauthorized());
        assertThat(isEnabled(4)).isFalse();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: the course admin is admitted even when not assigned to the student's course section")
    void testCourseAdminNotAssignedToSectionDeactivatesStudent() throws Exception {
        // A new course section of course 1 with no instructor assigned, and Tracy moved into it
        Course course1 = this.courseRepository.findAll().stream()
                .filter(course -> course.getCourseName().equals("COSC 40993 Senior Design")).findFirst().orElseThrow();
        Section unassigned = new Section("Unassigned", LocalDate.of(2024, 8, 19), LocalDate.of(2025, 5, 5), true, DayOfWeek.MONDAY, LocalTime.of(23, 59), DayOfWeek.TUESDAY, LocalTime.of(23, 59));
        course1.addSection(unassigned);
        this.sectionRepository.save(unassigned);
        Student tracy = this.studentRepository.findById(idOf("t.nicholson@abc.edu")).orElseThrow();
        tracy.setSection(unassigned);
        this.studentRepository.saveAndFlush(tracy);
        assertThat(unassigned.getInstructors()).isEmpty();

        this.mockMvc.perform(patch(deactivateUrl(tracy.getId())).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));
    }

    @Test
    @DisplayName("UC-STU-deactivate-student 3a: an already deactivated student gets 409 CONFLICT")
    void testDeactivateAlreadyDeactivatedStudent() throws Exception {
        this.mockMvc.perform(patch(deactivateUrl(4)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(status().isOk());

        this.mockMvc.perform(patch(deactivateUrl(4)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.CONFLICT))
                .andExpect(jsonPath("$.message").value("This student is already deactivated."));
        assertThat(isEnabled(4)).isFalse();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: a student may not deactivate herself")
    void testStudentCannotDeactivateHerself() throws Exception {
        this.mockMvc.perform(patch(deactivateUrl(4)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentJohnToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
        assertThat(isEnabled(4)).isTrue();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: a student may not deactivate a teammate")
    void testStudentCannotDeactivateAnotherStudent() throws Exception {
        this.mockMvc.perform(patch(deactivateUrl(4)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentEricToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
        assertThat(isEnabled(4)).isTrue();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: an instructor of another course section is refused")
    void testInstructorOfAnotherSectionCannotDeactivateStudent() throws Exception {
        Integer janaId = idOf("j.norton@abc.edu"); // course 3's section, which Bill is not assigned to
        this.mockMvc.perform(patch(deactivateUrl(janaId)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
        assertThat(isEnabled(janaId)).isTrue();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: the course admin of another course is refused")
    void testCourseAdminOfAnotherCourseCannotDeactivateStudent() throws Exception {
        this.mockMvc.perform(patch(deactivateUrl(4)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminTimToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
        assertThat(isEnabled(4)).isTrue();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: an id that names no student is refused, not reported missing")
    void testDeactivateUnknownStudent() throws Exception {
        this.mockMvc.perform(patch(deactivateUrl(999999)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: an unauthenticated caller gets 401")
    void testUnauthenticatedCannotDeactivateStudent() throws Exception {
        this.mockMvc.perform(patch(deactivateUrl(4)).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
        assertThat(isEnabled(4)).isTrue();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: a deactivated instructor's live token gets 401 before the route rule runs")
    void testDeactivatedInstructorCannotDeactivateStudent() throws Exception {
        PeerEvaluationUser bill = this.userRepository.findByEmail("b.gates@abc.edu").orElseThrow();
        bill.setEnabled(false);
        this.userRepository.saveAndFlush(bill);

        this.mockMvc.perform(patch(deactivateUrl(4)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.instructorBillToken))
                .andExpect(status().isUnauthorized());
        assertThat(isEnabled(4)).isTrue();
    }

    @Test
    @DisplayName("UC-STU-deactivate-student: the deactivated student stays on her team")
    void testDeactivatedStudentStaysOnHerTeam() throws Exception {
        this.mockMvc.perform(patch(deactivateUrl(4)).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.adminToken))
                .andExpect(status().isOk());

        this.mockMvc.perform(get(this.baseUrl + "/students/teams/1").accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, this.studentEricToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.data[?(@.id == 4)].enabled").value(Matchers.contains(false)));
    }

}

