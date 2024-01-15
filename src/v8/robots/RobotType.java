package v8.robots;

import v8.robots.AbstractRobot;
import v8.robots.CornerFinder;

public enum RobotType {
    CornerFinder(new CornerFinder());
//    BorderBuilder,

    private final AbstractRobot robot;

    RobotType(AbstractRobot robot) {
        this.robot = robot;
    }

    public AbstractRobot getRobot() {
        return robot;
    }
}