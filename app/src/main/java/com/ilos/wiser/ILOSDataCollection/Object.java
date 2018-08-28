package com.ilos.wiser.ILOSDataCollection;

import org.opencv.core.Scalar;

//TODO this class contains all the fields for what the openCV step counter detects as an object on screen i.e. the coloured circles

public class Object {
    public String type = "";
    public Scalar colour;
    public Scalar HSVMin;
    public Scalar HSVMax;
    public double xPos;
    public double yPos;
    public int frame;
    public Object(int frame) {
        this.type = "Object";
        this.frame = frame;
        colour = new Scalar(0,0,0);

    }
    public Object(String name, int frame) {
        this.type = name;
        this.frame = frame;
        if (name == "blue") {
            this.HSVMin = new Scalar(88, 60, 40);
            this.HSVMax = new Scalar(110, 255, 255);
            colour = new Scalar(0,0,255);
        }
        if (name == "red") {
            this.HSVMin = new Scalar(120, 80, 45);
            this.HSVMax = new Scalar(180, 255, 255);
            colour = new Scalar(255,0,0);
        }
        if (name == "green") {
            this.HSVMin = new Scalar(35, 65, 65);
            this.HSVMax = new Scalar(70, 255, 255);
            colour = new Scalar(0,255,0);
        }
    }
    public double getXPos() {
        return this.xPos;
    }
    public double getYPos() {
        return this.yPos;
    }
    public void setXPos(double position) {
        this.xPos = position;
    }
    public void setYPos(double position) {
        this.yPos = position;
    }

}
