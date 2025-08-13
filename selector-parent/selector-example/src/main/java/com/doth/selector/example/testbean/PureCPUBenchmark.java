package com.doth.selector.example.testbean;

import java.util.ArrayList;
import java.util.List;

public class PureCPUBenchmark {
    // public static void main(String[] args) {
    //  long start = System.currentTimeMillis();
    //     List<byte[]> list = new ArrayList<>();
    //     for (int i = 0; i < 1000; i++) { // 1000次
    //         byte[] arr = new byte[1024 * 1024]; // 1MB
    //         list.add(arr);
    //         if (list.size() > 200) { // 保持只有200MB在堆里
    //             list.remove(0);
    //         }
    //     }
    //     long end = System.currentTimeMillis();
    //     System.out.println("分配并回收1GB内存耗时: " + (end - start) + " ms");
    // }
     public static void main(String[] args) {
        List<byte[]> list = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            list.add(new byte[1024 * 1024]);
        }
        System.out.println("分配完成");
        System.gc();
        long start = System.currentTimeMillis();
        list.clear();
        System.gc();
        long end = System.currentTimeMillis();
        System.out.println("GC清理耗时: " + (end - start) + " ms");
    }
}
