package team.projectpulse.system.exception;

/**
 * UC-STU-deactivate-student extension 3a: the student is already deactivated. Mapped to HTTP 409 {@code CONFLICT}.
 */
public class StudentAlreadyDeactivatedException extends RuntimeException {

    public StudentAlreadyDeactivatedException() {
        super("This student is already deactivated.");
    }

}
