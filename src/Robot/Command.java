package Robot;

public enum Command {

    FORWARD, BACKWARD, TURN_LEFT, TURN_RIGHT, SEND_SENSORS, TAKE_IMG,  ALIGN_FRONT, ALIGN_RIGHT, FAST_FORWARD, FAST_BACKWARD, ERROR, START_EXP, ENDEXP, START_FAST, ENDFAST, ROBOT_POS;

    public enum AndroidMove {
        forward, back, left, right
    }

    public enum ArduinoMove {
        W, S, A, D, U, I, O, P, F, B
    }
}
