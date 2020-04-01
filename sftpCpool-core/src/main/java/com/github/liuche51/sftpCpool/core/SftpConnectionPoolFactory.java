package com.github.liuche51.sftpCpool.core;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Sftp连接池管理工程
 *
 * @author 01384865
 * 2020年1月15日
 */
public class SftpConnectionPoolFactory {
    private static Logger log = LoggerFactory.getLogger(SftpConnectionPoolFactory.class);
    private int min_count = 5;//连接池初始化最小连接数
    private int max_count = 30;//连接池初始化最大连接数
    private int timeout = 30000;//连接超时时间毫秒
    private long monitor_schedule_time = 5;//监控线程运行周期每5秒一次
    private long max_idle_close_time = 1;//连接空闲1分钟后自动关闭
    private Map<String, SftpConnectionPool> pools = new HashMap<>();
    private ScheduledExecutorService monitorExecutor;  //开启监控线程,对异常和空闲线程进行关闭.定期输出健康报告
    private ReentrantLock lock;

    public SftpConnectionPoolFactory() {
        lock = new ReentrantLock();
        monitorExecutor = Executors.newScheduledThreadPool(1);
        monitorExecutor.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                monitor();
            }
        }, monitor_schedule_time, monitor_schedule_time, TimeUnit.SECONDS);
    }

    public ChannelSftpInfo getConnection(String host, int port, String username, String password, int timeout) throws InterruptedException, JSchException {
        String key = host + ":" + port;
        SftpConnectionPool pool = pools.get(key);
        if (pool == null) {
            //防止多线程同时操作
            try {
                lock.lock();
                SftpConnectionPool pool2 = pools.get(key);
                if (pool2 == null) {
                    pool = new SftpConnectionPool(host, port, username, password, min_count, max_count, timeout == 0 ? this.timeout : timeout);
                    pools.put(key, pool);
                } else
                    pool = pool2;
            } finally {
                lock.unlock();
            }
        }
        if (pool.getPools()==null||pool.getPools().size()==0) {
            pool.createConnection();
        }
        ChannelSftpInfo sftpInfo = pool.getPools().take();
        return sftpInfo;
    }

    public void releaseConnection(ChannelSftpInfo sftpInfo, String host, int port) throws InterruptedException {
        String key = host + ":" + port;
        SftpConnectionPool pool = pools.get(key);
        if (pool != null) {
            pool.getPools().put(sftpInfo);
        }
    }

    private void monitor() {
        int closeCount = 0;
        try {
            StringBuilder config = new StringBuilder();
            config.append("{\"MIN_COUNT\":").append(min_count).append(",\"MAX_COUNT\":").append(max_count)
                    .append(",\"MONITOR_SCHEDULE_TIME\":").append(monitor_schedule_time).append("}");
            StringBuilder pool = new StringBuilder();
            Iterator<Map.Entry<String, SftpConnectionPool>> it = pools.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, SftpConnectionPool> entry = it.next();
                SftpConnectionPool tm = entry.getValue();
                if (tm.getPools().size() == 0)
                    it.remove();
                else {
                    BlockingQueue<ChannelSftpInfo> pools = tm.getPools();
                    for (ChannelSftpInfo sftpInfo : pools) {
                        ChannelSftp sftp = sftpInfo.getChannelSftp();
                        if (!sftp.getSession().isConnected()||LocalDateTime.now().minusMinutes(max_idle_close_time).compareTo(sftpInfo.getLastUseTime()) > 0) {
                            try {
                                sftp.getSession().disconnect();
                                sftp.exit();
                            } catch (Exception e) {
                                log.error("ChannelSftp 关闭异常!", e);
                            }
                            pools.remove(sftpInfo);
                            tm.getCurrentNum().getAndDecrement();
                            closeCount++;
                        }
                    }
                }
                pool.append("{\"route\":\"").append(entry.getKey()).append("\",\"available\":").append(tm.getPools().size())
                        .append(",\"currentNum\":").append(tm.getCurrentNum()).append("},");
            }
            log.info("Sftp连接池报告:\n配置信息{}\n池信息{}\n本次关闭池数:{}", config.toString(), pool.toString(), closeCount);
        } catch (Exception e) {
            log.error("Sftp连接池监控程序运行错误。", e);
        }
    }
}
