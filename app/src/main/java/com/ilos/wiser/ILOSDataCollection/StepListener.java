package com.ilos.wiser.ILOSDataCollection;

//TODO an interface for the step counter

public interface StepListener {
    //Interface for the pedometer
    void step(long timeNs);
}
