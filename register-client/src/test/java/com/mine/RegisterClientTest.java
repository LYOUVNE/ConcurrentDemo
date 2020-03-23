package com.mine;

public class RegisterClientTest {
    public static void main(String[] args) throws Exception {
        RegisterClient registerClient = new RegisterClient();
        registerClient.start();

        Thread.sleep(5000);

        registerClient.shutDown();
    }
}
