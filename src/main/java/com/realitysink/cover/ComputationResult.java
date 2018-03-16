package com.realitysink.cover;

public class ComputationResult {
    public boolean isPow;
    public boolean isBounty;
    public byte[] powHash;
    public byte[] targetWas;
    public int[] storage;
    public int[] personalized_ints;

    public ComputationResult copy(){
        ComputationResult res = new ComputationResult();
        res.isBounty = isBounty;
        res.isPow = isPow;
        res.powHash = powHash;
        res.targetWas = targetWas;
        res.storage = storage;
        res.personalized_ints = personalized_ints;
        return res;
    }
}