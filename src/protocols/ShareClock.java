package protocols;

import utils.MasterInterface;

import java.rmi.RemoteException;
import java.util.Date;

public class ShareClock {

    private static int TWO_MINUTES = 2 * 60 * 1000;
    private static ShareClock clockInstance = null;
    private Date date;
    private boolean synced = false;
    private long networkTime = 0;
    private long endSyncTime = 0;

    private ShareClock() {
        date = new Date();
    }

    public static ShareClock getClock() {
        if (clockInstance == null) {
            clockInstance = new ShareClock();
        }
        return clockInstance;
    }

    public long getTime() throws notSyncedException {
        if (!synced) {
            throw new notSyncedException();
        }
        long now = date.getTime();
        return networkTime + now - endSyncTime;
    }

    public void startSync() {
        new Thread(new networkTimeThread()).start();
    }

    private class networkTimeThread implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    MasterInterface masterServices= Election.getElectionInstance().getMasterStub();
                    if (masterServices == null) {
                        System.err.println("Error getting master RMI.");
                        try {
                            Thread.sleep(TWO_MINUTES);
                        } catch (InterruptedException e) {
                            System.err.println("Thread interrupted. Exiting...");
                            System.exit(1);
                        }
                        continue;
                    }
                    long getTime = Election.getElectionInstance().getMasterStub().getNetworkClock();
                    long startSyncTime = date.getTime();

                    endSyncTime= date.getTime();
                    networkTime = getTime + endSyncTime - startSyncTime;
                    System.out.println("Got network time. Updated from "+date.getTime()+ " to "+ new Date(networkTime).getTime());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(TWO_MINUTES);
                } catch (InterruptedException e) {
                    System.err.println("Thread interrupted. Exiting...");
                    System.exit(1);
                }
            }
        }
    }

    public static class notSyncedException extends Exception {
    }
}

