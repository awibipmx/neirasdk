package com.prasimax.applicationexample;

public class BaseMessage {
    String message;
    String terminal;
    long createdAt;

    String getMessage(){
        return message;
    }

    long getCreatedAt(){
        return createdAt;
    }

    String getSender(){
        return terminal;
    }
}
