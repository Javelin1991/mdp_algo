package Network;

import Map.Map;
import Map.MapDescriptor;
import Map.Direction;
import Robot.*;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import Robot.Robot;
import org.json.JSONArray;
import org.json.JSONObject;


public class RequestHandler extends Thread {

    private static final Logger LOGGER = Logger.getLogger(RequestHandler.class.getName());
    private BufferedWriter out;
    private BufferedReader in;
    private Socket socket;
    private int msgCounter = 0;
    private String prevMsg = null;
    private MapDescriptor MDF = new MapDescriptor();
    private Map realMap;
    private Map exploredMap;
    private Robot robot;

    // hard coded for testing checklist
//    private Point startPoint = new Point(7, 2);

    // hard coded for testing exploration
    private Point startPoint = new Point(1, 1);

    private Point wayPoint = new Point(13, 18);

    public RequestHandler(Socket socket) throws IOException {
        this.socket = socket;
        init();
    }

    private void init() throws IOException {
        out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
        in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        String path = System.getProperty("java.class.path");
        exploredMap = new Map();
        realMap = new Map();
        MDF.loadRealMap(realMap, "defaultMap.txt");
        robot = new Robot(true, false, 1, 1, Direction.UP);
        robot.setStartPos(startPoint.y, startPoint.x, exploredMap);
        robot.sense(exploredMap, realMap);
        System.out.println(exploredMap.getExploredPercentage());
        System.out.println(realMap.getExploredPercentage());
        System.out.println(realMap.getCell(8, 7));
    }

    public void sendStartMsg() {

        JSONObject startPoint = new JSONObject()
               .put(NetworkConstants.START_POINT_KEY, new JSONArray()
                       .put(new JSONObject()
                               .put("x", robot.getPos().x)
                               .put("y", robot.getPos().y)
                       )
               );
        send(startPoint.toString());
    }

    public void sendWayPoint() {

        JSONObject wayPoint = new JSONObject()
                .put(NetworkConstants.WAY_POINT_KEY, new JSONArray()
                        .put(new JSONObject()
                                .put("x", this.wayPoint.x)
                                .put("y", this.wayPoint.y)
                        )
                );
        send(wayPoint.toString());
    }

    @Override
    public void run() {
        try {
            System.out.println( "Received a connection" );
            String data;

//            TimeUnit.MILLISECONDS.sleep(1000);

            // For checklist
//            send(NetworkConstants.START_CHECKLIST);
//            sendStartMsg();

            // For exploration
            send(NetworkConstants.START_EXP);
            sendWayPoint();

            while (true) {
                // wait for incoming data
                do {
                    data = receive();
                } while(data == null);

                handle(data);
            }

            // Close our connection
//            in.close();
//            out.close();
//            socket.close();

//            System.out.println( "Connection closed" );
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }

    public void handle(String msg) throws InterruptedException {
        char firstChar;
        msg = msg.substring(1);
        firstChar = msg.charAt(0);
        LOGGER.info(Character.toString(firstChar));
        if (firstChar == '{') {
            System.out.println("Unhandled: " + msg);
        }
        else {
            String[] commands = msg.split("\\|");
            for (String cmd: commands) {
                execute_command(cmd);
            }
        }
    }

    private void execute_command(String cmd) throws InterruptedException {
        char firstChar = cmd.charAt(0);
        int step = 1;
        if (cmd.length() > 1) {
            step = Integer.parseInt(cmd.substring(1));
        }
        switch (firstChar) {
            case 'U':
                sendSensorRes();
                break;
            case 'W':
                robot.move(Command.FORWARD, step, exploredMap, RobotConstants.STEP_PER_SECOND);
                sendSensorRes();
                break;
            case 'X':
                robot.move(Command.BACKWARD, step, exploredMap, RobotConstants.STEP_PER_SECOND);
                sendSensorRes();
                break;
            case 'D':
                robot.turn(Command.TURN_RIGHT, RobotConstants.STEP_PER_SECOND);
                sendSensorRes();
                break;
            case 'A':
                robot.turn(Command.TURN_LEFT, RobotConstants.STEP_PER_SECOND);
                sendSensorRes();
                break;
            default:
                LOGGER.warning("Wrong char, do nothing");
                break;
        }
        robot.sense(exploredMap, realMap);
    }

    public void sendSensorRes() {
        HashMap<String, Integer> sensorRes = robot.getSensorRes(exploredMap, realMap);
        send(formatSensorRes(sensorRes));
    }

    public String formatSensorRes(HashMap<String, Integer> sensorRes) {
        StringBuilder sb = new StringBuilder();
        int obsBlock;
        for (String sname: robot.getSensorList()) {
            sb.append(sname);
            sb.append(":");
            sb.append(sensorRes.get(sname));
            sb.append("|");
        }
        return sb.toString();
    }

    /**
     * Sending a String type msg through socket
     * @param msg
     * @return true if the message is sent out successfully
     */
    public boolean send(String msg) {
        try {
            LOGGER.log(Level.FINE, "Sending Message...");
            out.write(msg);
            out.newLine();
            out.flush();
            msgCounter++;
            LOGGER.info(msgCounter +" Message Sent: " + msg);
            prevMsg = msg;
            return true;
        } catch (IOException e) {
            LOGGER.info("Sending Message Failed (IOException)!");
            return false;
        } catch (Exception e) {
            LOGGER.info("Sending Message Failed!");
            e.printStackTrace();
            return false;
        }
    }

    public String receive() {
        try {
            LOGGER.log(Level.FINE, "Receving Message...");
            String receivedMsg = in.readLine();
            if(receivedMsg != null && receivedMsg.length() > 0) {
                LOGGER.info("Received in receive(): " + receivedMsg);
                return receivedMsg;
            }
        } catch(IOException e) {
            LOGGER.info("Receiving Message Failed (IOException)!");
        } catch(Exception e) {
            LOGGER.info("Receiving Message Failed!");
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        RequestHandler handler = new RequestHandler(null);
    }
}
