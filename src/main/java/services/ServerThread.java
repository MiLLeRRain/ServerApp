package services;


/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import javafx.application.Platform;
import lombok.SneakyThrows;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import server.ServerUI;
import utils.*;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

import static server.ServerUI.*;


/**
 * This thread is responsible to handle client connection.
 *
 * @author www.codejava.net
 */
public class ServerThread extends Thread {
    Socket socket;
    ServerUI SUI;
    FirebaseUtil fu;
    PrintWriter output;
    BufferedReader input;
    ThreadedServer threadedServer;
    UserNode usernode;
    String AESKey;
    String loginToken = "loginToken";

    public ServerThread(Socket s, ServerUI su, FirebaseUtil fu, ThreadedServer threadedServer) throws IOException {
        this.socket = s;
        this.SUI = su;
        this.fu = fu;
        this.threadedServer = threadedServer;
        output = new PrintWriter(socket.getOutputStream(), true);
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @SneakyThrows
    public void run() {
        try {

            String enquiry;

            while (socket.isConnected()) {
                if ((enquiry = input.readLine()) == null)
                    continue;
                System.out.println("Start reading from client");

                updateInfoPanel();

                System.out.println("Client says: " + enquiry + "END");
                JSONObject jsonReceived = new JSONObject(enquiry);
                System.out.println("Client says 2: " + jsonReceived + "END");

//                JSONObject inputJson = listenToClient();
                JSONObject outputJson = processInput(jsonReceived);

                System.out.println("I am replying to " + socket + ": " + outputJson);
                output.println(outputJson);
//        socket.close();
                Thread.sleep(200);
            }
        } finally {
            CloseUtil.closeAll(socket, output, input);
            threadedServer.updateOnlineUsers(usernode, "remove");
        }
    }

    private void updateInfoPanel() {
        Platform.runLater(() -> SUI.statusPanel.setText("Got a connection from: " + socket));
    }

//    private JSONObject listenToClient() throws IOException, JSONException {
//        // This input is a JsonObject(String)
//
//        String enquiry = input.readLine();
//
//        System.out.println("Client says: " + enquiry + "END");
//        JSONObject jsonReceived = new JSONObject(enquiry);
//        System.out.println("Client says 2: " + jsonReceived + "END");
//
//        return jsonReceived;
//    }

    /**
     * Digest the incoming Json msg
     *
     * @param incomingJson from User Client
     * @return an output Json
     * @throws DocumentException
     * @throws JSONException
     */

    private JSONObject processInput(JSONObject incomingJson) throws Exception {

        JSONObject toReturn = null;
        String header = (String) incomingJson.get("header");
        Object msg = (Object) incomingJson.get("data");
        String clientToken = null;
        if (incomingJson.has("token")) {
            clientToken = (String) incomingJson.get("token");
        }

        switch (header) {
            case "pubKey":
                publicKey = (String) msg;
                toReturn = prepareAESKey();
                break;
            case "login":
                JSONObject userpass = (JSONObject) msg;
                toReturn = validate(userpass);
                break;
            case "chat":
                String encryptedChats = (String) msg;
                toReturn = chatService(encryptedChats);
                break;
            // autologin need valid loginToken otherwise will disc and request client to require the logintoken
            case "autoLogin":
                JSONObject userpassAuto = (JSONObject) msg;
                String username = AESUtil.decryptByECB((String) userpassAuto.get("username"), AESKey);
                loginToken = fu.getLoginToken(username);
                assert clientToken != null;
                if (!clientToken.equals(loginToken)) {
                    CloseUtil.closeAll(socket, output, input);
                    toReturn = JsonAssembler("invalidLogin", "N/A");
                    System.out.println("replying" + toReturn);
                }
                else toReturn = JsonAssembler("validAutoLogin", "N/A");
                System.out.println("replying" + toReturn);
                break;
            case "300":
                break;
        }

        return toReturn;
    }

    /**
     * Check if receiver is online? return a success signal : return a store to database signal
     *
     * @param encryptedChats
     * @return
     */
    private JSONObject chatService(String encryptedChats) throws Exception {
        System.out.println("chatService 0: " + encryptedChats);
        String decryptChats = AESUtil.decryptByECB(encryptedChats, AESKey);
        System.out.println("chatService: " + decryptChats);
        JSONObject chatDataJson = new JSONObject(decryptChats);
        System.out.println("chatService 2: " + chatDataJson);

        String sender = (String) chatDataJson.get("sender");
        String receiver = (String) chatDataJson.get("receiver");
        String sendTime = (String) chatDataJson.get("time");
        String msg = (String) chatDataJson.get("msg");


        if (threadedServer.isOnline(receiver)) {
            Socket receiverSocket = threadedServer.getSocket(receiver);
            sendMsg(receiverSocket, receiver, sender, msg, sendTime);
            return JsonAssembler("deliveryUpdate", "to online user: " + receiver);
        }
        sendOfflineMsg(receiver, msg, sendTime);
        return JsonAssembler("deliveryUpdate", "to offline user: " + receiver);
    }

    private void sendOfflineMsg(String receiver, String msg, String sendTime) {
        // Send to Firebase;
    }

    private void sendMsg(Socket receiverSocket, String receiver, String sender, String msg, String sendTime) throws Exception {
        PrintWriter pw = new PrintWriter(receiverSocket.getOutputStream(), true);
        Map<String, String> msgMap = new HashMap<>();
        msgMap.put("sender", sender);
        msgMap.put("sendTime", sendTime);
        msgMap.put("msg", msg);
        JSONObject encryptPackage = new JSONObject(msgMap);
        String encryptMsg = AESUtil.encryptByECB(encryptPackage.toString(), threadedServer.getAESKey(receiver));
        JSONObject msgJson = JsonAssembler("incomingMsg", encryptMsg);
        System.out.println(msgJson);
        pw.println(msgJson);
    }

    private void updateOnlineUsers(String username) {
        this.usernode = new UserNode(username, socket, AESKey);
        if (!threadedServer.isOnline(username)) {
            threadedServer.updateOnlineUsers(usernode, "add");
        }
    }

    private JSONObject prepareAESKey() throws Exception {
        AESKey = AESUtil.createAesKey();
        System.out.println("AESCREATE" + AESKey);
        String encryptedAESKey = RSAUtil.encrypt(AESKey, publicKey);
        JSONObject toReturn = JsonAssembler("AESKey", encryptedAESKey);
        return toReturn;
    }

    private JSONObject JsonAssembler(String header, String data) throws JSONException {
        JSONObject toReturn = new JSONObject();
        toReturn.put("header", header);
        toReturn.put("data", data);
        return toReturn;
    }

    private JSONObject validate(JSONObject userpass) throws Exception {
        JSONObject loginReplyJson = new JSONObject();
        System.out.println("AESKEY?" + AESKey);
        String username = AESUtil.decryptByECB((String) userpass.get("username"), AESKey);
        String password = AESUtil.decryptByECB((String) userpass.get("password"), AESKey);
        boolean valid = fu.validateUserPass(username, password);
        // if valid return loginToken with info
        if (valid) {
            loginToken = fu.getLoginToken(username);
            System.out.println("I got loginToken from Firebase: " + loginToken);
            String encryptToken = AESUtil.encryptByECB(loginToken, AESKey);
            loginReplyJson = JsonAssembler("validLogin", encryptToken);
            updateOnlineUsers(username);
        }
        // if not valid return failed info
        else {
            System.out.println("Login credentials are invalid.");
            loginReplyJson = JsonAssembler("invalidLogin", "Check the credentials, and login again.");
        }

        return loginReplyJson;
    }
}