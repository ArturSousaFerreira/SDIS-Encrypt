package backend;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User implements Serializable {

    private String username;
    private String password;

    public User(String userName, String password, Boolean external) {
        if (external) {
            this.username = userName;
            this.password = password;
        } else {
            this.username = userName;
            this.password = getHashedPassword(password);
        }

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String newusername) {
        this.username = newusername;
    }

    public String getPassword() {
        return password;
    }

    public String getHashedPassword(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(1);
        }
        md.update(password.getBytes());
        byte[] pass = md.digest();

        //convert the byte to hex format method 2
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < pass.length; i++) {
            hexString.append(Integer.toHexString(0xFF & pass[i]));
        }

        return hexString.toString();
    }

    public boolean login(String username, String password) {
        return (this.username.equals(username)
                && getHashedPassword(password).equals(this.password));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof User && ((User) obj).getUsername().equals(this.username);
    }

}
