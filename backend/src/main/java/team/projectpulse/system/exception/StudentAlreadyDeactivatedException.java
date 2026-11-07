package team.projectpulse.system.exception;

/** UC-STU-deactivate-student extension 3a: the student is already deactivated; answered as 409. */
public class StudentAlreadyDeactivatedException extends RuntimeException {

    public StudentAlreadyDeactivatedException(Integer studentId) {
        super("Student " + studentId + " is already deactivated.");
    }

}
