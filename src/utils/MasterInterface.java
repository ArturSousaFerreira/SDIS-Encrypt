package utils;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by Duarte on 14-May-17.
 */
public interface MasterInterface extends Remote{
    int master_Port = 12345;
    String ID = "MasterRMI";

    String sayHello()throws RemoteException;
    long getNetworkClock() throws RemoteException;
    SharedDatabase getMasterDatabase() throws RemoteException;
}
