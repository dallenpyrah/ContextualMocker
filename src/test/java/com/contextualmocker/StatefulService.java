package com.contextualmocker;

public interface StatefulService {
    boolean login(String user, String pass);
    boolean logout();
    String getSecret();
}