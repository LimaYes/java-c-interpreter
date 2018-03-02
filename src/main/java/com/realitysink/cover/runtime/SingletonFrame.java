package com.realitysink.cover.runtime;


import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;

public class SingletonFrame {
    private static MaterializedFrame me = null;

    public static MaterializedFrame getMe() {
        if(me==null){
            me = Truffle.getRuntime().createMaterializedFrame(null);
        }
        return me;
    }
}