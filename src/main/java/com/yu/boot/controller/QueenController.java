package com.yu.boot.controller;

import java.util.concurrent.ArrayBlockingQueue;


/****
 * 消费者模式
 * ArrayBlockingQueue
 * */
public class QueenController implements Runnable {

    //容器  
    private final ArrayBlockingQueue<String> queue;

    public QueenController(ArrayBlockingQueue<String> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            produce();
        }
    }

    public void produce() {
        /**
         * put()方法是如果容器满了的话就会把当前线程挂起
         * offer()方法是容器如果满的话就会返回false，也正是我在前一篇中实现的那种策略。
         * 
         *take()方法和put()方法是对应的，从中拿一个数据，如果拿不到线程挂起 
         * poll()方法和offer()方法是对应的，从中拿一个数据，如果没有直接返回null 
         */
        try {
            queue.put("你好");  //放数据
            String aa = queue.take(); //拿数据
            System.out.println("==" + aa);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int capacity = 10;
        ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(capacity);
        new Thread(new QueenController(queue)).start();
    }
}
