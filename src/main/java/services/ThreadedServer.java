package services;


import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import server.ServerUI;
import utils.FirebaseUtil;
import utils.UserNode;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;


/**
 * A TCP server that runs on port 9090.  When a client connects, it
 * sends the client the current date and time, then closes the
 * connection with that client.  Arguably just about the simplest
 * server you can write.
 */
public class ThreadedServer {

    private ServerUI su;
    private FirebaseUtil fu = new FirebaseUtil();
    private static ObservableList<UserNode> onlineUsers;

    // encrypt & decrypt every communication or only the code?

    public ThreadedServer(ServerUI su) throws IOException, ExecutionException, InterruptedException {
        this.su = su;
        int port = 9090;
        onlineUsers = FXCollections.observableArrayList();
        ServerSocket serverSocket = new ServerSocket(port);

        onlineUsers.addListener((ListChangeListener<UserNode>) c -> Platform.runLater(() -> updateUI(su, onlineUsers)));

        System.out.println("Server started on 9090");
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Accept socket and new thread to listen to client: " + socket.toString());
                new ServerThread(socket, su, fu, this).start();
            }
        } finally {
            serverSocket.close();
        }
    }

    private void updateUI(ServerUI su, ObservableList<UserNode> onlineUsers) {
        su.chatters.getChildren().clear();
        if (!onlineUsers.isEmpty()) {
            for (UserNode un : onlineUsers) {
                su.chatters.getChildren().add(new Button(un.getUsername() + " | " + un.getSocket() + " | " + un.getConnectTime()));
            }
        }
    }

    public Socket getSocket(String username) {
        for (UserNode un : onlineUsers) {
            if (un.getUsername().equals(username)) {
                return un.getSocket();
            }
        }
        return null;
    }

    public boolean isOnline(String username) {
        for (UserNode un : onlineUsers) {
            if (un.getUsername().equals(username)) return true;
        }
        return false;
    }

    public String getAESKey(String username) {
        for (UserNode un : onlineUsers) {
            if (un.getUsername().equals(username)) return un.getAESKey();
        }
        return "offline";
    }

    public boolean updateOnlineUsers(UserNode user, String instruction) {
        if (instruction.equals("remove") && onlineUsers.contains(user)) {
            onlineUsers.remove(user);
            System.out.println("removed" + user);
//            su.chatters.appendText("\n\nUser went offline:\n" + user.getUsername() + "@" + user.getSocket());
            return true;
        } else if (instruction.equals("add") && !onlineUsers.contains(user)) {
            onlineUsers.add(user);
            System.out.println("added" + user);
//            su.chatters.appendText("\n\nUser went online:\n" + user.getUsername() + "@" + user.getSocket());
            return true;
        }
        return false;
    }

}