package net.libraya.gobdarchive.service;

import net.libraya.gobdarchive.utils.Unicodes;

public class ServiceLogger {

    private final StringBuilder buffer = new StringBuilder();
    private int lastRenderLength = 0;
    
    
    public ServiceLogger() {
    }
    
    public void log(String msg) {
        buffer.append(msg).append("\n");
    }

    public boolean hasChanged() {
        return buffer.length() != lastRenderLength;
    }

    public void markRendered() {
        lastRenderLength = buffer.length();
    }

    public String getLog() {
        return buffer.toString();
    }

    public int length() {
        return buffer.length();
    }

    public void renderLog(String title, int port) {
        System.out.println("\n" + Unicodes.BULLET + " WebService: " + title + " (port:" + port + ") " + Unicodes.HORIZONTAL_BAR);
        System.out.println("\nUse A/D to switch logs or overview.\n");
        System.out.println(buffer.toString());
    }
}