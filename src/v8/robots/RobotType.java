package v8.robots;
import v8.robots.FlagPlacer;

public enum RobotType {
    CornerFinder(new CornerFinder()), FlagPlacer(new FlagPlacer()), Attacker(new Attacker());
//    BorderBuilder,

    private final AbstractRobot robot;

    RobotType(AbstractRobot robot) {
        this.robot = robot;
    }

    public AbstractRobot getRobot() {
        return robot;
    }
}