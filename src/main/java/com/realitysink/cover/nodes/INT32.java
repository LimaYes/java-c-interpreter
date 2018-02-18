package com.realitysink.cover.nodes;

public class INT32 {
    public int value = 0;

    public INT32(int value) {
        this.value = value;
    }

    public static INT32 gen(int x){
        return new INT32(x);
    }
}
