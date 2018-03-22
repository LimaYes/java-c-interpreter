package com.realitysink.cover;

public class ComputationResult {
    public boolean isPow;
    public boolean isBounty;
    public byte[] powHash;
    public byte[] targetWas;
    public int[] storage;
    public int[] personalized_ints;
    public int storage_size;

    public ComputationResult(){
        isBounty = false;
        isPow = false;
        powHash = new byte[16];
        targetWas = new byte[16];
        storage = new int[0];
        personalized_ints = new int[12];
        storage_size = 0;
    }

    public ComputationResult copy(){
        ComputationResult res = new ComputationResult();
        res.isBounty = isBounty;
        res.isPow = isPow;
        res.powHash = powHash;
        res.targetWas = targetWas;
        res.storage = storage;
        res.personalized_ints = personalized_ints;
        res.storage_size = storage_size;
        return res;
    }
}