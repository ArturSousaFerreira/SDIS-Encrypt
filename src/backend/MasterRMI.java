package backend;

import utils.MasterInterface;
import utils.SharedDatabase;

import java.rmi.RemoteException;
import java.util.Date;

/**
 * Created by Duarte on 14-May-17.
 */
public class MasterRMI implements MasterInterface {
    public String sayHello() {
        return "Hello";
    }

    public long getNetworkClock() throws RemoteException {
        return new Date().getTime();
    }

    public SharedDatabase getMasterDatabase() throws RemoteException {
        return ConfigManager.getConfigManager().getSharedDatabase();
    }
}
