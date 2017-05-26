
package utils;


import backend.FileInfo;
import backend.SavedFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Scanner;

import static java.lang.System.exit;

public class Client {


    static RMI_Interface stub;
    static boolean islogin = false;
    private static String username = null;
    private static boolean stop = false;
    private static ArrayList<String> userlist;

    public static void main(String[] args) {
        //Client Acess_Point Command operand1 operand2

        if (args.length !=1){
            //usage
            exit(1);
        }

        try {
            Registry registry = LocateRegistry.getRegistry(RMI_Interface.RMI_PORT);
            stub = (RMI_Interface) registry.lookup(args[0]);
        } catch (Exception e) {
            System.err.println("utils.Client exception: " + e.toString());
            e.printStackTrace();
        }

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        Scanner scanner = new Scanner(System.in);
        String readline = null;
        int menuoption, noptions, command;

        while (!stop) {

            String[] arguments = null;
            boolean invalid = false;
            do {
                invalid = false;
                if (islogin) {

                    System.out.println("\n           CLIENT MENU");
                    try {
                        if (stub.getUserFiles(username).isEmpty())
                            System.out.println("\n" + username + " don't have any files in the database");
                        else
                            System.out.println("\nShared database files related to " + username + " :");

                        int count = 1;
                        userlist = new ArrayList<String>();
                        for (FileInfo file : stub.getUserFiles(username)) {
                            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
                            userlist.add(file.getFileId());
                            System.out.format("%5s | %15s | %20s\n", "Index" , "File name", "Data");
                           System.out.format("%5s | %15s | %20s\n", count , file.getFileName(), sdfDate.format(file.getData()));
                            count++;
                        }
                    } catch (RemoteException e) {
                        System.out.println(e.getMessage());
                    }
                    System.out.println("\n1- Backup");
                    System.out.println("2- Restore");
                    System.out.println("3- Reclaim");
                    System.out.println("4- Delete");
                    System.out.println("5- State Local Database");
                    System.out.println("6- State Shared Database");
                    System.out.println("7- Hello");
                    System.out.println("8- Quit");
                    command = 4;
                    noptions = 8;

                } else {
                    System.out.println("\n               MENU\n");
                    System.out.println("1- Login");
                    System.out.println("2- Create User");
                    System.out.println("3- State shared DB");
                    System.out.println("4- Quit");
                    command = 0;
                    noptions = 4;
                }
                System.out.print("\n[1-" + noptions + "] : ");
                menuoption = scanner.nextInt() + command;

                if (!(menuoption >= 1 && menuoption <= noptions + command)) {
                    invalid = true;
                    System.out.println("Invalid input - option [1-" + noptions + "]");
                }


            } while (invalid);

            boolean response = false;

            switch (menuoption) {
                case 1: // LOGIN
                    System.out.print("Login arguments - username password : ");
                    try {
                        readline = stdin.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    arguments = readline.split(" ");
                    if (!islogin && arguments.length == 2) {
                        try {
                            response = stub.login(arguments[0], arguments[1]);
                            if (response) {
                                islogin = true;
                                username = arguments[0];
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        System.out.println("response: " + response);
                    } else
                        System.out.println("Invalid arguments for login or already login! - LOGIN <username> <password>");
                    break;
                case 2: // CREATE USER
                    System.out.print("Create user arguments - username password : ");
                    try {
                        readline = stdin.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    arguments = readline.split(" ");
                    if (!islogin && arguments.length == 2) {
                        try {
                            response = stub.createUser(arguments[0], arguments[1]);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        System.out.println("response: " + response);
                    } else
                        System.out.println("Invalid arguments for create a user! - CREATEUSER <username> <password>");
                    break;
                case 3: // HELLO
                    try {
                        stub.stateSharedDatabase();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case 4: // QUIT
                    stop = true;
                    break;
                case 5: // BACKUP
                    System.out.print("Backup arguments - filename replication_degree : ");
                    try {
                        readline = stdin.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    arguments = readline.split(" ");
                    if (islogin && arguments.length == 2) {
                        try {
                            System.out.print("Backup...");
                            response = stub.backupFile(arguments[0], Integer.parseInt(arguments[1]), username);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        System.out.println("response: " + response);
                    } else System.out.println("invalid arguments");
                    break;
                case 6: // RESTORE
                    System.out.print("Restore arguments - File Index : ");
                    try {
                        readline = stdin.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    arguments = readline.split(" ");
                    if (islogin && arguments.length == 1) {
                        try {
                            if (Integer.parseInt(arguments[0]) >= 0 && Integer.parseInt(arguments[0]) <= userlist.size())
                                response = stub.restoreFile(username,userlist.get(Integer.parseInt(arguments[0]) - 1));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        System.out.println("response: " + response);
                    } else System.out.println("invalid arguments");
                    break;
                case 7: // RECLAIM
                    System.out.print("Reclaim arguments - filename : ");
                    try {
                        readline = stdin.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    arguments = readline.split(" ");
                    if (islogin && arguments.length == 1) {
                        try {
                            response = stub.spaceReclaim(Integer.parseInt(arguments[0]));
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } else System.out.println("invalid arguments or not login");
                    break;
                case 8: // DELETE
                    System.out.print("Delete arguments - Index : ");
                    try {
                        readline = stdin.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    arguments = readline.split(" ");
                    if (islogin && arguments.length == 1) {
                        try {
                            if (Integer.parseInt(arguments[0]) >= 0 && Integer.parseInt(arguments[0]) <= userlist.size())
                                response = stub.deleteFile(userlist.get(Integer.parseInt(arguments[0]) - 1), username);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        System.out.println("response: " + response);
                    } else System.out.println("invalid arguments");
                    break;
                case 9: // STATE LOCAL DATABASE
                    try {
                        stub.statemydatabase();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case 10: // STATE SHARED DATABASE
                    try {
                        stub.stateSharedDatabase();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case 11: // HELLO
                    try {
                        stub.stateSharedDatabase();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
                case 12: // QUIT
                    stop = true;
                    break;
                default:
                    System.out.println("Command not recognized");
            }
        }
    }

}
