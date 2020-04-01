package com.github.liuche51.sftpCpool.core;
import com.jcraft.jsch.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class SftpConnectionPool {
    private static Logger log = LoggerFactory.getLogger(SftpConnectionPool.class);
    private BlockingQueue<ChannelSftpInfo> pools = new LinkedBlockingQueue<>();
    private AtomicInteger currentNum = new AtomicInteger(0);//当前连接数
    private int min_count = 5;//连接池初始化最小连接数
    private int max_count = 30;//连接池初始化最大连接数
    private int timeout=30000;//连接超时时间毫秒
    private String host;
    private int port = 22;
    private String username;
    private String password;

    public BlockingQueue<ChannelSftpInfo> getPools() {
        return pools;
    }

    public AtomicInteger getCurrentNum() {
        return currentNum;
    }

    public SftpConnectionPool(String host, int port, String username, String password, int minCount, int maxCount, int timeout) throws JSchException {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.min_count = minCount;
        this.max_count = maxCount;
        init();
    }

    private void init() throws JSchException {
        for (int i = 0; i < min_count; i++) {
            createConnection();
        }
    }

    public void createConnection() throws JSchException {
        if (currentNum.get() >= max_count) return;
        try {
            JSch jsch = new JSch();
            Session sshSession = jsch.getSession(username, host, port);
            sshSession.setPassword(password);
            sshSession.setTimeout(timeout);//毫秒
            sshSession.setConfig("StrictHostKeyChecking", "no");
            sshSession.setConfig("kex", "diffie-hellman-group1-sha1");
            sshSession.connect();
            Channel channel = sshSession.openChannel("sftp");
            channel.connect();
            ChannelSftp sftp = (ChannelSftp) channel;
            ChannelSftpInfo info=new ChannelSftpInfo();
            info.setChannelSftp(sftp);
            info.setLastUseTime(LocalDateTime.now());
            pools.offer(info);//offer(E e)：如果队列没满，立即返回true； 如果队列满了，立即返回false-->不阻塞
            currentNum.getAndIncrement();
        }catch (Exception e){
            log.error("Sftp 连接创建失败！host="+host,e);
        }

    }
}
