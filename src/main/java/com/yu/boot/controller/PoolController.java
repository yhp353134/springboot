package com.yu.boot.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PoolController extends Thread {

    ExecutorService pool1 = Executors.newFixedThreadPool(5);  
    ExecutorService pool = Executors.newCachedThreadPool();
    
    
    @Override  
    public void run() {  
        pool.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("==");
                
            }
        });
        
    }
    
}
