package v8.robots;

public enum RobotType {
    CornerFinder(new CornerFinder()),
    FlagPlacer(new FlagPlacer()),
    Attacker(new Attacker()),
    Defender(new Defender()),
    Default(new Default());
//    BorderBuilder,

    private final AbstractRobot robot;

    RobotType(AbstractRobot robot) {
        this.robot = robot;
    }

    public AbstractRobot getRobot() {
        return robot;
    }
}