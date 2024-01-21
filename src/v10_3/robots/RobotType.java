package v10_3.robots;

public enum RobotType {
    CornerFinder(new CornerFinder()),
    FlagPlacer(new FlagPlacer()),
    //Attacker(new Attacker()),
    Defender(new Defender()),
    Scouter(new Scouter()),
    Default(new Default()),
    AttackerTwo(new AttackerTwo()),
    FlagCarrier(new FlagCarrier());
//    FlagCarrier(new FlagCarrier());
//    BorderBuilder,

    private final AbstractRobot robot;

    RobotType(AbstractRobot robot) {
        this.robot = robot;
    }

    public AbstractRobot getRobot() {
        return robot;
    }
}