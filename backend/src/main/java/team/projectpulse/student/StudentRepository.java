package team.projectpulse.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer>, JpaSpecificationExecutor<Student> {

    // Find all students in a section
    List<Student> findBySectionSectionId(Integer sectionId);

    List<Student> findByTeamTeamId(Integer teamId);

    /**
     * Disables the student only if she is still enabled, as one atomic conditional update, so that of two
     * concurrent deactivations exactly one wins (UC-STU-deactivate-student, extension 3a).
     *
     * @return the number of rows changed: 1 if this call deactivated her, 0 if she was already deactivated
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Student s SET s.enabled = false WHERE s.id = :studentId AND s.enabled = true")
    int disableIfEnabled(@Param("studentId") Integer studentId);
}
