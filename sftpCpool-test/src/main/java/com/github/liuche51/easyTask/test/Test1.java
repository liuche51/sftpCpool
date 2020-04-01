package com.github.liuche51.easyTask.test;

import com.github.liuche51.sftpCpool.core.ChannelSftpInfo;
import com.github.liuche51.sftpCpool.core.SftpConnectionPoolFactory;
import com.jcraft.jsch.ChannelSftp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

public class Test1 {
    public static List<String> getSftpListFiles() throws Exception {
        SftpConnectionPoolFactory sftpConnectionPoolFactory=SftpConnectionPoolFactory.getInstance();
        sftpConnectionPoolFactory.setMin_count(7);
        ChannelSftpInfo sftpInfo = sftpConnectionPoolFactory.getConnection("203.195.177.228", 22, "admin", "JFDE@568UGFR",30000);
        List<ChannelSftp.LsEntry> files = new LinkedList<ChannelSftp.LsEntry>();
        try {
            Vector<ChannelSftp.LsEntry> fs = sftpInfo.getChannelSftp().ls("/WebSite");
            for (ChannelSftp.LsEntry entry : fs) {
                if (!entry.getAttrs().isDir()) {
                    files.add(entry);
                }
            }
        } finally {
            sftpConnectionPoolFactory.releaseConnection(sftpInfo, "203.195.177.228",22);
        }
        List<String> list = new ArrayList<String>();
        if (null != files && files.size() > 0) {
            for (ChannelSftp.LsEntry f : files) {
                list.add(f.getFilename());
            }
        }
        return list;
    }
}
