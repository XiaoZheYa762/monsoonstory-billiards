package dev.monsoonstory.billiards;


import java.util.Arrays;

/** Bounded, allocation-free tick sampling; values cover management, physics and network work. */
public final class Stats {
    private static final long[] samples=new long[1200];
    private static int cursor,count;
    public static long totalNanos,ticks,physicsNanos,physicsTicks;
    public static void tick(long ns){samples[cursor++%samples.length]=ns;count=Math.min(count+1,samples.length);totalNanos+=ns;ticks++;}
    public static void supplement(long ns){if(cursor>0){samples[(cursor-1)%samples.length]+=ns;totalNanos+=ns;}}
    public static double[] summary(){long[] a=Arrays.copyOf(samples,count);Arrays.sort(a);if(count==0)return new double[4];return new double[]{Arrays.stream(a).average().orElse(0)/1e6,a[(int)((count-1)*.95)]/1e6,a[(int)((count-1)*.99)]/1e6,a[count-1]/1e6};}
    public static void clear(){cursor=count=0;totalNanos=ticks=physicsNanos=physicsTicks=0;Network.sentBytes=Network.sentPackets=0;}
    public static String report(){double[] v=summary();return String.format(java.util.Locale.ROOT,"Billiards: mean %.3f / p95 %.3f / p99 %.3f / max %.3f ms; %d bytes / %d packets; %d viewers",v[0],v[1],v[2],v[3],Network.sentBytes,Network.sentPackets,Network.viewerCount());}
    private Stats(){}
}
