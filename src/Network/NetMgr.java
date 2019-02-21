package Network;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

/**
 * Socket client class to connect to RPI
 */
public class NetMgr {

    private static final Logger LOGGER = Logger.getLogger(NetMgr.class.getName());

    private String ip;
    private int port;
    private static Socket socket = null;
    private String prevMsg = null;

    private BufferedWriter out;
    private BufferedReader in;
    private int msgCounter = 0;

    private Timer wait = new Timer();


    private static NetMgr netMgr = null;

    public NetMgr(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public static NetMgr getInstance(String ip, int port) {
        if (netMgr == null) {
            netMgr = new NetMgr(ip, port);
        }
        return netMgr;
    }

    public String getIp() {
        return this.ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Initiate a connection with RPI if there isn't already one
     * @return true if connection established with RPI
     */
    public boolean initConn() {
        if(isConnect()) {
            LOGGER.info("Already connected with RPI");
            return true;
        }
        else {
            try {
                LOGGER.info("Initiating Connection with RPI...");
                socket = new Socket(ip, port);
                out = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                LOGGER.info("Connection with RPI established!");
                return true;
            } catch (UnknownHostException e) {
                LOGGER.warning("Connection Failed: UnknownHostException\n" + e.toString());
                return false;
            } catch (IOException e) {
                LOGGER.warning("Connection Failed: IOException\n" + e.toString());
                return false;
            } catch (Exception e) {
                LOGGER.warning("Connection Failed!\n" + e.toString());
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Close the connection with RPI
     * @return True if there is no more connection with RPI
     */
    public boolean closeConn() {
        LOGGER.info("Closing connection... ");
        if(!isConnect()) {
            LOGGER.warning("No connection with RPI");
            return true;
        }
        else {
            try {
                socket.close();
                out.close();
                in.close();
                socket = null;
                return true;
            } catch (IOException e) {
                LOGGER.warning("Unable to close connection: IOException\n" + e.toString());
                e.printStackTrace();
                return false;
            }
        }
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
            if(socket.isConnected())
                LOGGER.info("Connection still Established!");
            else {
                while(true)
                {
                    LOGGER.info("Connection disrupted! Trying to Reconnect!");
                    if(netMgr.initConn()) {
                        break;
                    }
                }
            }
            return netMgr.send(msg);
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

    /**
     * Check if there are existing connection with RPI
     * @return
     */
    public boolean isConnect() {
        if(socket == null) {
            return false;
        }
        else {
            return true;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String ip = "192.168.9.9";
//        String ip = "127.0.0.1";
        int port = 1273;
        String data;
        NetMgr netMgr = new NetMgr(ip, port);
        netMgr.initConn();

        while(true){
            //String msg = Command.FORWARD.toString();
            do {
                data = netMgr.receive();
            } while(data == null);

            System.out.println("\nReceived: " + data);
            String msg = "AW3|D|W3|D|W3|D|W3|D|";
            if (data.equals("checklist")) {
                netMgr.send(msg);
            }
            //TimeUnit.MILLISECONDS.sleep(1000);

//            netMgr.closeConn();
        }

    }

}