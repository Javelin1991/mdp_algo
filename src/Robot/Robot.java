package Robot;

import Map.Map;
import Map.Direction;
import Map.MapDescriptor;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import Helper.*;
import Network.NetMgr;
import Network.NetworkConstants;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;


public class Robot {

    private static final Logger LOGGER = Logger.getLogger(Robot.class.getName());

    private boolean sim;            // true if in simulator mode, false otherwise (actual)
    private boolean findingFP;      // true if doing fastest path, false otherwise (exploration)
    private boolean reachedGoal;
    private Point pos;
    private Direction dir;
    private String status;

    private Command preMove;

    private ArrayList<String> sensorList;
    private HashMap<String, Sensor> sensorMap;
//    private static PrintManager printer = new PrintManager();

    // for delay in sim
    private long tempStartTime, tempEndTime, tempDiff;

    // for converting map to send to android
    private MapDescriptor MDF = new MapDescriptor();

    public Robot(boolean sim, boolean findingFP, int row, int col, Direction dir) {
        this.sim = sim;
        this.findingFP = findingFP;
        this.pos = new Point(col, row);
        this.dir = dir;
        this.reachedGoal = false;  // may need to amend
        this.sensorList = new ArrayList<String>();
        this.sensorMap = new HashMap<String, Sensor>();
        initSensors();
        this.status = String.format("Initialization completed.\n");
//        printer.setText(printer.getText() + this.status + "\n");
        // remember to set start position outside
    }

    @Override
    public String toString() {
        String s = String.format("Robot at %s facing %s\n", pos.toString(), dir.toString());
        return s;
    }

    // Getters and setters

    public boolean getSim() {
        return this.sim;
    }

    public void setSim(boolean sim) {
        this.sim = sim;
    }

    public boolean isFindingFP() {
        return this.findingFP;
    }

    public void setFindingFP(boolean findingFP) {
        this.findingFP = findingFP;
    }

    public Point getPos() {
        return this.pos;
    }

    public void setPos(int row, int col) {
        // to be changed when sensor is added
        this.pos = new Point(col, row);
    }

    public void setPos(Point pos) {
        // to be changed when sensor is added
        this.pos = pos;
    }


    public Direction getDir() {
        return this.dir;
    }

    public void setDir(Direction dir) {
        this.dir = dir;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isReachedGoal() {
        return this.reachedGoal;
    }

    public void setReachedGoal(boolean reachedGoal) {
        this.reachedGoal = reachedGoal;
    }

    public ArrayList<String> getSensorList() {
        return sensorList;
    }

    public HashMap<String, Sensor> getSensorMap() {
        return sensorMap;
    }

    public Sensor getSensor(String sensorId) {
        return sensorMap.get(sensorId);
    }


    /**
     * Initialization of the Sensors
     *
     * ID for the sensors: XX
     * 1st Letter: F - Front, L - Left, R - Right
     * 2nd Letter: Identifier
     * L1 is long IR, the rest is short IR
     *
     * Sensor list includes:
     * Front: 3 short range sensors
     * Right: 2 short range sensors
     * Left: 1 long range sensors
     *
     */
    private void initSensors() {
        int row = pos.y;
        int col = pos.x;

        // Front Sensors
        Sensor SF1 = new Sensor("F1", RobotConstants.SHORT_MIN, RobotConstants.SHORT_MAX, row + 1, col - 1,
                Direction.UP);
        Sensor SF2 = new Sensor("F2", RobotConstants.SHORT_MIN, RobotConstants.SHORT_MAX, row + 1, col, Direction.UP);
        Sensor SF3 = new Sensor("F3", RobotConstants.SHORT_MIN, RobotConstants.SHORT_MAX, row + 1, col + 1,
                Direction.UP);

        // RIGHT Sensor
        Sensor SR1 = new Sensor("R1", RobotConstants.SHORT_MIN, RobotConstants.SHORT_MAX, row - 1, col + 1,
                Direction.RIGHT);
        Sensor SR2 = new Sensor("R2", RobotConstants.SHORT_MIN, RobotConstants.SHORT_MAX, row + 1, col + 1,
                Direction.RIGHT);

        // LEFT Sensor
        Sensor LL1 = new Sensor("L1", RobotConstants.LONG_MIN, RobotConstants.LONG_MAX, row + 1, col - 1,
                Direction.LEFT);

        sensorList.add(SF1.getId());
        sensorList.add(SF2.getId());
        sensorList.add(SF3.getId());
        sensorList.add(SR1.getId());
        sensorList.add(SR2.getId());
        sensorList.add(LL1.getId());
        sensorMap.put(SF1.getId(), SF1);
        sensorMap.put(SF2.getId(), SF2);
        sensorMap.put(SF3.getId(), SF3);
        sensorMap.put(SR1.getId(), SR1);
        sensorMap.put(SR2.getId(), SR2);
        sensorMap.put(LL1.getId(), LL1);

        if (dir != Direction.UP) {
            rotateSensors(dir);
        }

        this.status = "Sensor initialized\n";
//        printer.setText(printer.getText() + this.status + "\n");

    }

    private void setSensorPos(int rowDiff, int colDiff) {
        int row, col;
        Sensor s;
        for (String sname: sensorList) {
            s = sensorMap.get(sname);
            s.setPos(s.getRow() + rowDiff, s.getCol() + colDiff);
        }
    }


    private void locateSensorAfterRotation(Sensor s, double angle) {
        // pos
        int newCol, newRow;
        newCol = (int) Math.round((Math.cos(angle) * (s.getCol() - pos.x) - Math.sin(angle) * (s.getRow() - pos.y) + pos.x));
        newRow = (int) Math.round((Math.sin(angle) * (s.getCol() - pos.x) - Math.cos(angle) * (s.getRow() - pos.y) + pos.y));
        s.setPos(newRow, newCol);
    }

    /**
     * Change sensor var (dir, pos) when the robot turns
     * @param turn_dir turning direction of the robot (left or right only)
     */
    private void internalRotateSensor(Direction turn_dir) {
        double angle = 0;

        // turn_dir
        switch (turn_dir) {
            case LEFT:
                angle = Math.PI / 2;
                for (String sensorId : sensorList) {
                    Sensor s = sensorMap.get(sensorId);
                    s.setSensorDir(Direction.getAntiClockwise(s.getSensorDir()));
                    locateSensorAfterRotation(s, angle);
                }
                break;
            case RIGHT:
                angle = -Math.PI / 2;
                for (String sensorId : sensorList) {
                    Sensor s = sensorMap.get(sensorId);
                    s.setSensorDir(Direction.getClockwise(s.getSensorDir()));
                    locateSensorAfterRotation(s, angle);
                }
                break;
            default:
                LOGGER.warning("No rotation done. Wrong input direction: " + turn_dir);
        }
    }


    /**
     * Change sensor var (dir, pos) when the robot turns
     * @param turn_dir turning direction of the robot (left, right, down)
     */
    private void rotateSensors(Direction turn_dir) {
        switch (turn_dir) {
            case LEFT:
                internalRotateSensor(Direction.LEFT);
                break;
            case RIGHT:
                internalRotateSensor(Direction.RIGHT);
                break;
            case DOWN:
                internalRotateSensor(Direction.RIGHT);
                internalRotateSensor(Direction.RIGHT);
                break;
            default:
                break;
        }
    }


    /**
     * Robot movement with direction (forward, backward) and steps) and Map updated.
     * @param cmd FORWARD or BACKWARD
     * @param steps number of steps moved by the robot
     * @param exploredMap current explored environment of the robot
     */
    public void move(Command cmd, int steps, Map exploredMap, int stepsPerSecond) throws InterruptedException {

        tempStartTime = System.currentTimeMillis();

        if (!sim) {
            // TODO to send fast forward
            // send command to Arduino
            String cmdStr = getCommand(cmd, steps);
            LOGGER.info("Command String: " + cmdStr);
            NetMgr.getInstance().send(NetworkConstants.ARDUINO + cmdStr);

        }

        int rowInc = 0, colInc = 0;

        switch(dir) {
            case UP:
                rowInc = 1;
                colInc = 0;
                break;
            case DOWN:
                rowInc = -1;
                colInc = 0;
                break;
            case LEFT:
                rowInc = 0;
                colInc = -1;
                break;
            case RIGHT:
                rowInc = 0;
                colInc = 1;
                break;
        }

        switch (cmd) {
            case FORWARD:
                break;
            case BACKWARD:
                rowInc *= -1;
                colInc *= -1;
                break;
            default:
                status = String.format("Invalid command: %s! No movement executed.\n", cmd.toString());
//                printer.setText(printer.getText() + status + "\n");
                LOGGER.warning(status);
                return;
        }

        int newRow = pos.y + rowInc * steps;
        int newCol = pos.x + colInc * steps;

        if(exploredMap.checkValidMove(newRow, newCol)) {

            preMove = cmd;
            status = String.format("%s for %d steps\n", cmd.toString(), steps);
            //printer.setText(printer.getText() + status + "\n" + pos.toString() + "\n");
            LOGGER.info(status);
            LOGGER.info("row = " + newRow + ", col = " + newCol);
//            logSensorInfo();

            // delay for sim
            if (sim) {
                tempEndTime = System.currentTimeMillis();
                tempDiff = RobotConstants.WAIT_TIME / stepsPerSecond * steps - (tempEndTime - tempStartTime);
                if (tempDiff > 0) {
//                System.out.println(tempDiff);
                    TimeUnit.MILLISECONDS.sleep(tempDiff);
                }
            }
            this.setPosition(newRow, newCol);
            if(!findingFP) {
                for (int i = 0; i < steps; i++) {
                    exploredMap.setPassThru(newRow - rowInc * i, newCol - colInc * i);
                }
            }
        }
    }


    /**
     * move method when cmd is about turning (TURN_LEFT, TURN RIGHT)
     * @param cmd
     */
    public void turn(Command cmd, int stepsPerSecond) throws InterruptedException {

        tempStartTime = System.currentTimeMillis();
        if (!sim) {
            // send command to Arduino
            // TODO: add turning degree
            String cmdStr = getCommand(cmd, 1);
            LOGGER.info("Command String: " + cmdStr);
            NetMgr.getInstance().send(NetworkConstants.ARDUINO + cmdStr);


        }
        switch(cmd) {
            case TURN_LEFT:
                dir = Direction.getAntiClockwise(dir);
                rotateSensors(Direction.LEFT);
                break;
            case TURN_RIGHT:
                dir = Direction.getClockwise(dir);
                rotateSensors(Direction.RIGHT);
                break;
            default:
                status = "Invalid command! No movement executed.\n";
//                printer.setText(printer.getText() + status + "\n");
                LOGGER.warning(status);
                return;
        }
        preMove = cmd;
        status = cmd.toString() + "\n";
        //printer.setText(printer.getText() + status + "\n" + pos.toString() + "\n");
        LOGGER.info(status);
        LOGGER.info(pos.toString());
//        logSensorInfo();

        // delay for simulator
        if (sim) {
            tempEndTime = System.currentTimeMillis();
            tempDiff = RobotConstants.WAIT_TIME / stepsPerSecond - (tempEndTime - tempStartTime);
            if (tempDiff > 0) {
                TimeUnit.MILLISECONDS.sleep(tempDiff);
            }
        }

    }


    /**
     * Set starting position, assuming direction unchanged
     * @param col
     * @param row
     * @param exploredMap
     */
    public void setStartPos(int row, int col, Map exploredMap) {
        setPosition(row, col);
        exploredMap.setAllExplored(false);
        exploredMap.setAllMoveThru(false);
        for (int r = row - 1; r <= row + 1; r++) {
            for (int c = col - 1; c <= col + 1; c++) {
                exploredMap.getCell(r, c).setExplored(true);
                exploredMap.getCell(r, c).setMoveThru(true);
            }
        }
    }

    /**
     * Set robot position, assuming direction unchanged
     * @param col
     * @param row
     */
    public void setPosition(int row, int col) {
        int colDiff = col - pos.x;
        int rowDiff = row - pos.y;
        pos.setLocation(col, row);
        setSensorPos(rowDiff, colDiff);
    }

    public void logSensorInfo() {
        for (String sname : sensorList) {
            Sensor s = sensorMap.get(sname);
            String info = String.format("id: %s\trow: %d; col: %d\tdir: %s\n", s.getId(), s.getRow(), s.getCol(), s.getSensorDir());
            LOGGER.info(info);
        }
    }

    public Point parseStartPointJson(String jsonMsg) {

        // double check to make sure that it is a start msg
        if (jsonMsg.contains(NetworkConstants.START_POINT_KEY)) {
            // parse json
            JSONObject startPointJson = new JSONObject(new JSONTokener(jsonMsg));
            JSONArray startPointArray = (JSONArray) startPointJson.get(NetworkConstants.START_POINT_KEY);
            startPointJson = (JSONObject) startPointArray.get(0);
            Point startPoint = new Point((int) startPointJson.get("x"), (int) startPointJson.get("y"));
            return startPoint;
        }
        else {
            LOGGER.warning("Not a start point msg. Return null.");
            return null;
        }
    }

    public Point parseWayPointJson(String jsonMsg) {

        // double check to make sure that it is a start msg
        if (jsonMsg.contains(NetworkConstants.WAY_POINT_KEY)) {
            // parse json
            JSONObject wayPointJson = new JSONObject(new JSONTokener(jsonMsg));
            JSONArray wayPointArray = (JSONArray) wayPointJson.get(NetworkConstants.WAY_POINT_KEY);
            wayPointJson = (JSONObject) wayPointArray.get(0);
            Point wayPoint = new Point((int) wayPointJson.get("x"), (int) wayPointJson.get("y"));
            return wayPoint;
        }
        else {
            LOGGER.warning("Not a start point msg. Return null.");
            return null;
        }
    }

    /**
     * Getting sensor result from RPI/Arduino
     * @return HashMap<SensorId, ObsBlockDis>
     */
    public HashMap<String, Integer> getSensorRes(String msg) {
        int obsBlock;
        HashMap<String, Integer> sensorRes = new HashMap<String, Integer>();
        if (msg.charAt(0) != 'F') {
            // not sensor info sent from arduino
            return null;
        }
        else {
            String[] sensorStrings = msg.split("\\|");
            for (String sensorStr: sensorStrings) {
                String[] sensorInfo = sensorStr.split("\\:");
                String sensorID = sensorInfo[0];
                int grid = Integer.parseInt(sensorInfo[1]);
                if (grid >= sensorMap.get(sensorID).getMinRange() && grid <= sensorMap.get(sensorID).getMaxRange()) {
                    sensorRes.put(sensorID, grid);
                }
                else {
                    sensorRes.put(sensorID, -1);
                }
            }
        }

        return sensorRes;
    }

    /**
     * Getting sensor result for simulator
     * @param exploredMap
     * @param realMap
     * @return HashMap<SensorId, ObsBlockDis>
     */
    public HashMap<String, Integer> getSensorRes(Map exploredMap, Map realMap) {
        int obsBlock;
        HashMap<String, Integer> sensorRes = new HashMap<String, Integer>();
        for(String sname: sensorList) {
            obsBlock = sensorMap.get(sname).detect(realMap);
            sensorRes.put(sname, obsBlock);
        }
        return sensorRes;
    }

    /**
     * Robot sensing surrounding obstacles for simulator
     * @param exploredMap
     * @param realMap
     */
    public void sense(Map exploredMap, Map realMap){

        HashMap<String, Integer> sensorRes;
        int obsBlock;
        int rowInc=0, colInc=0, row, col;

        if(sim) {
            sensorRes = getSensorRes(exploredMap, realMap);
        }
        else {
            String msg = NetMgr.getInstance().receive();
            sensorRes = getSensorRes(msg);
            if(sensorRes == null) {
                LOGGER.warning("Invalid msg. Map not updated");
                return;
            }
        }

        for(String sname: sensorList) {
            Sensor s = sensorMap.get(sname);
            obsBlock = sensorRes.get(sname);

            // Assign the rowInc and colInc based on sensor Direction
            switch (s.getSensorDir()) {
                case UP:
                    rowInc = 1;
                    colInc = 0;
                    break;

                case LEFT:
                    rowInc = 0;
                    colInc = -1;
                    break;

                case RIGHT:
                    rowInc = 0;
                    colInc = 1;
                    break;

                case DOWN:
                    rowInc = -1;
                    colInc = 0;
                    break;
            }

            for (int j = s.getMinRange(); j <= s.getMaxRange(); j++) {

                row = s.getRow() + rowInc * j;
                col = s.getCol() + colInc * j;

                // check whether the block is valid otherwise exit (Edge of Map)
                if(exploredMap.checkValidCell(row, col)) {
                    exploredMap.getCell(row, col).setExplored(true);

                    if(j == obsBlock && !exploredMap.getCell(row, col).isMoveThru()) {
                        exploredMap.getCell(row, col).setObstacle(true);
                        exploredMap.setVirtualWall(exploredMap.getCell(row, col));
                        break;
                    }
                }
                else {
                    break;
                }

            }
        }

        // send to Android
        if (!sim) {
            JSONObject androidJson = new JSONObject();

            // robot
            JSONArray robotArray = new JSONArray();
            JSONObject robotJson = new JSONObject()
                    .put("x", pos.x + 1)
                    .put("y", pos.y + 1)
                    .put("direction", dir.toString().toLowerCase());
            robotArray.put(robotJson);

            // map
            String obstacleString = MDF.generateMDFString2(exploredMap);
            JSONArray mapArray = new JSONArray();
            JSONObject mapJson = new JSONObject()
                    .put("explored", MDF.generateMDFString1(exploredMap))
                    .put("obstacle", obstacleString)
                    .put("length", obstacleString.length() * 4);
            mapArray.put(mapJson);

            androidJson.put("map", mapArray).put("robot", robotArray);
            NetMgr.getInstance().send(NetworkConstants.ANDROID + androidJson.toString());
        }

    }

//    /**
//     * Robot sensing surrounding obstacles for actual run
//     * @param exploredMap
//     */
//    public void sense(Map exploredMap){
//        // TODO
//        // build JSON
//        // Take note of setting obstacles on and off (different from simulator)
//    }

    /**
     * Get the turn Command(s) for the robot to face the newDir
     * @param newDir Direction robot should face after the command(s) being executed
     * @return
     */
    public ArrayList<Command> getTurn(Direction newDir) {
        ArrayList<Command> commands = new ArrayList<Command>();

        if (newDir == Direction.getAntiClockwise(dir)) {
            commands.add(Command.TURN_LEFT);
        }
        else if (newDir == Direction.getClockwise(dir)) {
            commands.add(Command.TURN_RIGHT);
        }
        else if (newDir == Direction.getOpposite(dir)) {
            commands.add(Command.TURN_RIGHT);
            commands.add(Command.TURN_RIGHT);
        }
        return commands;
    }

    public String getCommand(Command cmd, int steps) {
        StringBuilder cmdStr = new StringBuilder();

        cmdStr.append(Command.ArduinoMove.values()[cmd.ordinal()]);
        if (steps > 1) {
            cmdStr.append(steps);
        }
        cmdStr.append('|');

        return cmdStr.toString();
    }

    public static void main(String[] args) throws InterruptedException{
        Robot robot = new Robot(true, true,1, 1, Direction.UP);
        System.out.println(robot.status);

        robot.turn(Command.TURN_RIGHT, 1);
        robot.logSensorInfo();
        LOGGER.info(robot.status);
        LOGGER.info(robot.toString());
//        printer.setText(printer.getText() + robot.status + "\n" + robot.toString() + "\n");

        robot.move(Command.FORWARD, 1, null, 1);
//        robot.logSensorInfo();
//        LOGGER.info(robot.status);
//        LOGGER.info(robot.toString());

    }

}
