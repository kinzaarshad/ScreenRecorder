package com.virtoxed.screenrecorderlivestreamrecorder.helper;

import java.io.Serializable;

public class StreamProfile implements Serializable {
    private String mId, mStreamUrl, mSecureStreamUrl, mTitle, mDescription, mHost, mApp, mPlayPath;
    private int mPort;

    public StreamProfile(String id, String streamURL, String secureStreamUrl) {
        mId = id;
        setStreamUrl(streamURL);
        mSecureStreamUrl = secureStreamUrl;
    }

    public String getId() {
        return mId;
    }

    public void setId(String mStreamId) {
        this.mId = mStreamId;
    }

    public String getStreamUrl() {
        return mStreamUrl;
    }

    public void setStreamUrl(String mStreamUrl) {
        this.mStreamUrl = mStreamUrl;
        updateHostPortPlayPath();
    }

    private void updateHostPortPlayPath() {

        mHost = "live-api-s.facebook.com";
        mPort = 80;
        mApp = "rtmp";
        mPlayPath = mStreamUrl.substring(mStreamUrl.lastIndexOf('/')+1);
    }

    public String getSecureStreamUrl() {
        return mSecureStreamUrl;
    }

    public void setSecureStreamUrl(String mSecureStreamUrl) {
        this.mSecureStreamUrl = mSecureStreamUrl;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    public String getHost() {
        return mHost;
    }

    public void setHost(String host) {
        mHost = host;
    }

    public String getApp() {
        return mApp;
    }

    public void setApp(String app) {
        mApp = app;
    }

    public String getPlayPath() {
        return mPlayPath;
    }

    public void setPlayPath(String playPath) {
        mPlayPath = playPath;
    }

    public int getPort() {
        return mPort;
    }

    public void setPort(int port) {
        mPort = port;
    }

    @Override
    public String toString() {
        return "StreamProfile{" +
                "mId='" + mId + '\'' +
                ", mStreamUrl='" + mStreamUrl + '\'' +
                ", mSecureStreamUrl='" + mSecureStreamUrl + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mHost='" + mHost + '\'' +
                ", mApp='" + mApp + '\'' +
                ", mPlayPath='" + mPlayPath + '\'' +
                ", mPort=" + mPort +
                '}';
    }
}
