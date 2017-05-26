package backend;

import protocols.ChunkBackup;
import protocols.ChunkRestore;
import protocols.Election;
import utils.MasterInterface;
import utils.Message;

import java.rmi.RemoteException;
import java.util.Random;

/**
 * Created by Duarte on 16-Mar-17.
 */
public class MCHandler implements Runnable {
    private Message mMessage;
    private Random random;
    private static final int TIMEOUT = 400;
    private String IP;

    public MCHandler(Message receivedMessage, String IP) {
        mMessage = receivedMessage;
        random = new Random();
        this.IP = IP;
    }

    public void run() {
        String[] headerParts = mMessage.getHeader().split(" ");

        String messageType = headerParts[0].trim();

        int messageID = Integer.parseInt(headerParts[2].trim());

        //TODO:TAKE ME OUT LATER, JUST FOR TESTING
        //System.out.println("MC Received Message: " + messageType + " from " + messageID);

        String fileID;
        int chunkNR;

        if (headerParts[1].trim().equals("1.0")) {
            switch (messageType) {
                case "STORED":

                    fileID = headerParts[3].trim();
                    chunkNR = Integer.parseInt(headerParts[4].trim());
                    //if the file is mine, ++ repCount of the chunk
                    System.out.println("Received STORED from " + messageID + " for chunk " + chunkNR);


                    try {
                        System.out.println("try");
                        ConfigManager.getConfigManager().incChunkReplication(fileID,
                                chunkNR);
                    } catch (ConfigManager.InvalidChunkException e) {
                        System.out.println("catch");
                        synchronized (MCListener.getInstance().pendingChunks) {
                            for (Chunk chunk : MCListener
                                    .getInstance().pendingChunks) {
                                if (fileID.equals(chunk.getFileID())
                                        && chunk.getChunkNo() == chunkNR) {
                                    System.out.println("Chunk " + chunk.getChunkNo() + " increasing from " + chunk.getCurrentReplicationDegree());
                                    chunk.incCurrentReplication();
                                    break;
                                }

                            }
                        }
                    }
                    break;
                case "GETCHUNK":
                    fileID = headerParts[3].trim();
                    chunkNR = Integer.parseInt(headerParts[4].trim());

                    Chunk chunkToGet = ConfigManager.getConfigManager().getSavedChunk(fileID, chunkNR);


                        //if I don't store the chunk I don't care about the rest
                        if (chunkToGet != null) {
                            ChunkRecord record = new ChunkRecord(fileID, chunkNR);
                            synchronized (MCListener.getInstance().watchedChunk) {
                                MCListener.getInstance().watchedChunk.add(record);
                            }
                            try {
                                Thread.sleep(random.nextInt(TIMEOUT));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //If no one responded to the GET, then I will do it
                            if (!record.isServed) {
                                ChunkRestore.getInstance().sendChunk(chunkToGet);
                            }
                            synchronized (MCListener.getInstance().watchedChunk) {
                                MCListener.getInstance().watchedChunk.remove(record);
                            }
                        }

                    break;


                case "DELETE":
                        fileID = headerParts[3].trim();
                        String username= headerParts[4].trim();
                    System.out.println("Received a DELETE for fileID " + fileID + "of user= "+ username);
                        ConfigManager.getConfigManager().deleteFile(fileID);

                        for (FileInfo info :
                                ConfigManager.getConfigManager().getSharedDatabase().getUserFiles(username)){
                            if (info.getFileId().equals(fileID)){
                                ConfigManager.getConfigManager().getSharedDatabase().removeFile(info,username);
                            }
                        }

                    break;
                case "REMOVED":
                    if (messageID != ConfigManager.getConfigManager().getMyID()) {
                        fileID = headerParts[3].trim();
                        chunkNR = Integer.parseInt(headerParts[4].trim());

                        Chunk removedChunk = ConfigManager.getConfigManager().getSavedChunk(fileID, chunkNR);
                        if (removedChunk != null) {
                            try {
                                removedChunk.decCurrentReplicationDegree();
                                Thread.sleep(random.nextInt(401));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (removedChunk.getCurrentReplicationDegree() < removedChunk.getWantedReplicationDegree()) {
                                ChunkBackup.getInstance().putChunk(removedChunk);
                            }
                        }
                    }
                    break;

                case Election.WAKEUP_CMD:
                    if (messageID != ConfigManager.getConfigManager().getMyID()) {
                        if (Election.getElectionInstance().isMaster()) {
                            System.out.println("MasterInterface responding to WAKEUP from " + messageID);
                            Election.getElectionInstance().sendMastercmd();
                        }
                    }
                    break;
                case Election.MASTER_CMD:

                    if (messageID != ConfigManager.getConfigManager().getMyID()) {
                        String masterIP = headerParts[3];
                        System.out.println("Received Master msg from " + messageID + " with IP= " + masterIP);
                        if (!Election.getElectionInstance().checkIfMaster(messageID)) {

                            //lidar com conflitos dos masters
                            if (Election.getElectionInstance().isMaster()) {
                                //I will trigger a new election if Im a master
                                System.out.println("Received a challenge to my MasterInterface. Starting Election!");
                                Election.getElectionInstance().candidate();
                            } else {
                                System.out.println("I got a new MasterInterface " + messageID);
                                Election.initMaster(masterIP, messageID);
                                try {
                                    ConfigManager.getConfigManager().setMasterDB(ConfigManager.getConfigManager().getSharedDatabase().sync(Election.getElectionInstance().getMasterStub().getMasterDatabase()));
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            System.out.println("My MasterInterface is in IP " + masterIP);
                        }
                    }
                    break;

                case Election.CANDIDATE_CMD:

                    long uptime = Long.parseLong(headerParts[3]);

                    String message_ip = headerParts[4];
                    if (messageID != ConfigManager.getConfigManager().getMyID()) {
                        System.out.println("Received a candidate from " + messageID + " with time= " + uptime + " vs my time is " + ConfigManager.getConfigManager().getUpTime());
                        try {
                            Election.getElectionInstance().setMasterInfo(uptime, message_ip, messageID);
                        } catch (Election.ElectionNotRunningException e) {
                            System.out.println("running new election thread");
                            new Thread(() -> Election.getElectionInstance().candidate()).start();

                            try {
                                Thread.sleep(400);
                                Election.getElectionInstance().setMasterInfo(uptime, message_ip, messageID);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            } catch (Election.ElectionNotRunningException e1) {
                                e1.printStackTrace();
                            }

                        }
                    }
                    break;
                case "ADDUSER":
                    if (messageID != ConfigManager.getConfigManager().getMyID()) {
                        username = headerParts[3];
                        String password = headerParts[4];

                        System.out.println("Received a ADDUSER msg for user=" + username);
                        User adduser = new User(username, password, true);
                        ConfigManager.getConfigManager().getSharedDatabase().addUser(adduser);

                    }
                    break;
                case "ADDFILE":
                    if (messageID != ConfigManager.getConfigManager().getMyID()) {
                        String userName = headerParts[3];
                        fileID = headerParts[4];
                        String fileName = headerParts[5];
                        chunkNR = Integer.parseInt(headerParts[6]);
                        long date = Long.parseLong(headerParts[7]);
                        System.out.println("Received a NEW FIle for "+ userName+ " fileID="+fileID+" filename="+fileName+" date"+date+" chunknr="+chunkNR);
                        ConfigManager.getConfigManager().getSharedDatabase().addFile(new FileInfo(fileID, fileName, date, chunkNR), userName);
                    }
                    break;
                default:
                    //unknown
                    System.out.println("Can't handle " + messageType + "type");
                    break;
            }
        } else System.out.println("MC: message with different version");
    }
}