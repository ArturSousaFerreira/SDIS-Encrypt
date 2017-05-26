package protocols;

import backend.ConfigManager;
import backend.MasterRMI;
import backend.MulticastServer;
import utils.MasterInterface;
import utils.RMI_Interface;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Random;


public class Election {

    //Commands
    public static final String WAKEUP_CMD = "START";
    public static final String MASTER_CMD = "MASTER";
    public static final String CANDIDATE_CMD = "CANDIDATE";
    private static final int MASTER_KEEP_ALIVE_TIME = 30;
    private static String masterIP = null;
    private static int masterID;
    private static Election electionInstance = null;
    private static Boolean gotMaster = false;
    Registry reg;
    private Long up_time;
    private boolean electionRunning = false;
    private boolean imMaster = false;
    private long lastMasterCmdTime;
    private long masterUptime = 0;
    private Thread keepAliveThread = null;
    private boolean keepAliveFlag = false;

    private Thread masterCmdThread = null;
    private boolean masterCmdFlag = false;

    private Election() {
        up_time = (long) 0;
    }

    public static Election getElectionInstance() {
        if (electionInstance == null) {
            electionInstance = new Election();
        }
        return electionInstance;
    }

    public static void initMaster(String ip, int messageID) {
        gotMaster = true;
        masterIP = ip;
        masterID = messageID;
    }

    public boolean isMaster() {
        return imMaster;
    }

    public void startUp() {
        String msg = "";

        InetAddress multCtrlAddr = ConfigManager.getConfigManager().getMcAddr();
        int multCtrlPort = ConfigManager.getConfigManager().getmMCport();

        MulticastServer sender = new MulticastServer(multCtrlAddr, multCtrlPort);

        msg = WAKEUP_CMD + " " + "1.0" + " " + ConfigManager.getConfigManager().getMyID() + MulticastServer.CRLF + MulticastServer.CRLF;
        System.out.println("Sending WAKEUP to get Master");

        int counter = 0;
        do {
            try {
                sender.sendMessage(msg.getBytes(MulticastServer.ASCII_CODE));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(500 * (int) Math.pow(2, counter));
                System.out.println("waiting: " + 500 * (int) Math.pow(2, counter));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            counter++;
            synchronized (gotMaster) {
                if (gotMaster) {
                    break;
                }
            }
        }
        while (counter < 3);

        //If I didnt find a MasterInterface, start election
        if (!gotMaster) {
            System.out.println("No master response.Starting election!");
            candidate();
            //start new db
        } else {
            keepAliveFlag = true;
            keepAliveThread = new Thread(new masterKeepAlive());
            keepAliveThread.start();
        }
        ConfigManager.getConfigManager().startClocks();
    }

    public boolean checkIfMaster(int messageID) {
        lastMasterCmdTime = new Date().getTime();
        return masterID == messageID;
    }

    public void candidate() {
        System.out.println("Initiating Canditate protocol");

        //shutdown services
        if (imMaster) {
            try {
                reg.unbind(String.valueOf(ConfigManager.getConfigManager().getMyID()));
                UnicastRemoteObject.unexportObject(reg, true);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }

        electionRunning = true;
        long uptime = ConfigManager.getConfigManager().getUpTime();
        if (imMaster) {
            masterCmdFlag = false;
        } else {
            keepAliveFlag = false;
        }
        gotMaster = false;
        imMaster = false;
        masterID = -1;
        masterIP = null;
        masterUptime = 0;

        synchronized (up_time) {
            up_time = uptime;
        }
        InetAddress multCtrlAddr = ConfigManager.getConfigManager().getMcAddr();
        int multCtrlPort = ConfigManager.getConfigManager().getmMCport();
        MulticastServer sender = new MulticastServer(multCtrlAddr, multCtrlPort);

        String msg = "";
        Random r = new Random();
        msg = CANDIDATE_CMD + " " + "1.0" + " " + ConfigManager.getConfigManager().getMyID() + " " + uptime + " "
                + ConfigManager.getConfigManager().getMyIP().getHostAddress() + MulticastServer.CRLF + MulticastServer.CRLF;

        int wait = r.nextInt(400);
        try {
            Thread.sleep(wait);
            setMasterInfo(uptime, ConfigManager.getConfigManager().getMyIP().getHostAddress(), ConfigManager.getConfigManager().getMyID());
            System.out.println("Candidating with time = " + uptime);
            if (uptime > masterUptime) {
                sender.sendMessage(msg.getBytes(MulticastServer.ASCII_CODE));
            }
            Thread.sleep(500 - wait);
        } catch (UnsupportedEncodingException | InterruptedException e) {
            e.printStackTrace();
        } catch (ElectionNotRunningException e) {
            e.printStackTrace();
        }

        if (masterID == ConfigManager.getConfigManager().getMyID()) {
            imMaster = true;
        }

        if (masterID == -1 || masterID == ConfigManager.getConfigManager().getMyID()) {
            //I won and I'm the new master
            System.out.println("Im the new master");
            imMaster = true;
            gotMaster = true;
            electionRunning = false;
            masterID = ConfigManager.getConfigManager().getMyID();
            masterIP = ConfigManager.getConfigManager().getMyIP().getHostAddress();
            masterUptime = uptime;
            keepAliveFlag = false;
            masterCmdFlag = true;
            masterCmdThread = new Thread(new masterCmdMessg());
            masterCmdThread.start();

            try {
                setupMasterRmi();
            } catch (imNotMasterException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("My new master is " + masterID);
            electionRunning = false;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            keepAliveThread = new Thread(new masterKeepAlive());
            ShareClock.getClock().startSync();
            keepAliveThread.start();
        }
    }

    public void sendMastercmd() {
        if (!imMaster) {
            System.out.println("Can't send MASTER response because im not imMaster!");
            return;
        }
        InetAddress multCtrlAddr = ConfigManager.getConfigManager().getMcAddr();
        int multCtrlPort = ConfigManager.getConfigManager().getmMCport();

        MulticastServer sender = new MulticastServer(multCtrlAddr, multCtrlPort);
        String msg = "";

        msg = MASTER_CMD + " " + "1.0" + " " + ConfigManager.getConfigManager().getMyID() + " " + ConfigManager.getConfigManager().getMyIP().getHostAddress() + MulticastServer.CRLF + MulticastServer.CRLF;

        try {
            sender.sendMessage(msg.getBytes(MulticastServer.ASCII_CODE));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }

    private class masterCmdMessg implements Runnable {
        @Override
        public void run() {
            while (masterCmdFlag) {
                System.out.println("sending master");
                sendMastercmd();
                try {
                    Thread.sleep(1000 * MASTER_KEEP_ALIVE_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private class masterKeepAlive implements Runnable {
        @Override
        public void run() {
            System.out.println("starting keep alive master");
            while (keepAliveFlag) {
                try {
                    System.out.println("Checking master cmd");
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    System.out.println("keep alive interrupted");
                }
                long now = new Date().getTime();
                if ((now - lastMasterCmdTime) > (MASTER_KEEP_ALIVE_TIME * 1000 + 2 * 1000)) {
                    System.out.println("I lost my master " + masterID + ". candidating()");
                    break;
                }
            }
            candidate();
        }
    }

    public void setMasterInfo(long uptime, String message_ip, int messageID) throws ElectionNotRunningException {
        System.out.println("electionMode = " + electionRunning);
        if (!electionRunning) {
            throw new ElectionNotRunningException();
        }
        if (uptime > masterUptime) {
            synchronized (gotMaster) {
                gotMaster = true;
                masterIP = message_ip;
                masterID = messageID;
            }
            masterUptime = uptime;
        }
    }

    private void setupMasterRmi() throws imNotMasterException {
        if (!imMaster) {
            throw new imNotMasterException();
        }
        try {
            System.setProperty("java.rmi.server.hostname", ConfigManager.getConfigManager().getMyIP().getHostAddress());
            MasterInterface stub = (MasterInterface) UnicastRemoteObject.exportObject(new MasterRMI(), 0);
            try {
                reg = null;
                reg = LocateRegistry.createRegistry(12345);
            } catch (RemoteException e) {
                System.out.println("RMI already created");
                reg = LocateRegistry.getRegistry(ConfigManager.getConfigManager().getMyIP().getHostAddress(), 12345);
            }
            reg.rebind(String.valueOf(ConfigManager.getConfigManager().getMyID()), stub);
        } catch (RemoteException e1) {
            System.out.println("RMI problem");
        }
    }


    public MasterInterface getMasterStub() {
        try {
            reg = LocateRegistry.getRegistry(masterIP, 12345);
            return (MasterInterface) reg.lookup(String.valueOf(masterID));
        } catch (RemoteException e) {
            System.err.println("Error getting stub of " + masterID + " from RMI Registry. Candidate...");
            candidate();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class ElectionNotRunningException extends Exception {
    }

    public class imNotMasterException extends Exception {
    }
}