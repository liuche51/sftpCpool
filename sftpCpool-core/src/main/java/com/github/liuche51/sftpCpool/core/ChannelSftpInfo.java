package com.github.liuche51.sftpCpool.core;

import com.jcraft.jsch.ChannelSftp;
import java.time.LocalDateTime;


public class ChannelSftpInfo {
    private ChannelSftp channelSftp;
    private LocalDateTime lastUseTime=LocalDateTime.now();//最近一次使用时间

    public ChannelSftp getChannelSftp() {
        return channelSftp;
    }

    public void setChannelSftp(ChannelSftp channelSftp) {
        this.channelSftp = channelSftp;
    }

    public LocalDateTime getLastUseTime() {
        return lastUseTime;
    }

    public void setLastUseTime(LocalDateTime lastUseTime) {
        this.lastUseTime = lastUseTime;
    }
}
