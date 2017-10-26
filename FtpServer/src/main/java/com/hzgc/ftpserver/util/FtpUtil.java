package com.hzgc.ftpserver.util;

import com.hzgc.ftpserver.local.FileType;
import org.apache.ftpserver.util.IoUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FtpUtil implements Serializable {
    private static Logger LOG = Logger.getLogger(FtpUtil.class);

    public static boolean checkPort(int checkPort) throws Exception {
        return checkPort > 1024;
    }

    public static ByteArrayOutputStream inputStreamCacher(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        try {
            while ((len = is.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            IoUtils.close(baos);
            IoUtils.close(is);
        }
        return baos;
    }

    /**
     * @param pictureName determine the picture type based on the file name
     * @return equals 0, it is a picture
     * lager than 0, it is a face picture
     */
    public static int pickPicture(String pictureName) {
        int picType = 0;
        if (null != pictureName) {
            String tmpStr = pictureName.substring(pictureName.lastIndexOf("_") + 1, pictureName.lastIndexOf("."));
            try {
                picType = Integer.parseInt(tmpStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return picType;
    }

    public static String faceKey(int faceNum, String key) { //    3B0383FPAG00883_170523160015_0000004075_00_PC-PC
        StringBuilder faceKey = new StringBuilder();
        if (faceNum < 10) {
            key = key.substring(0, key.lastIndexOf("_"));
            String key1 = key.substring(0,key.lastIndexOf("_"));
            faceKey.append(key1).append("_0").append(faceNum).append("_").append(IpAddressUtil.getHostName());
        } else if (faceNum >= 10 && faceNum < 100) {
            key = key.substring(0, key.lastIndexOf("_"));
            String key1 = key.substring(0,key.lastIndexOf("_"));
            faceKey.append(key1).append("_").append(faceNum).append("_").append(IpAddressUtil.getHostName());
        } else {
            faceKey.append(key);
        }
        return faceKey.toString();
    }

    public static String transformNameToKey(String fileName) {
        StringBuilder key = new StringBuilder();

        if (fileName != null && fileName.length() > 0) {
            String ipcID = fileName.substring(1, fileName.indexOf("/", 1));
            String tempKey = fileName.substring(fileName.lastIndexOf("/"), fileName.lastIndexOf("_")).replace("/", "");
            String prefixName = tempKey.substring(tempKey.lastIndexOf("_") + 1, tempKey.length());
            String timeName = tempKey.substring(2, tempKey.lastIndexOf("_")).replace("_", "");

            StringBuffer prefixNameKey = new StringBuffer();
            prefixNameKey = prefixNameKey.append(prefixName).reverse();
            if (prefixName.length() < 10) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < 10 - prefixName.length(); i++) {
                    stringBuilder.insert(0, "0");
                }
                prefixNameKey.insert(0, stringBuilder);
            }
            key.append(ipcID).append("_").append(timeName).append("_").append(prefixNameKey).append("_00").append("_").append(IpAddressUtil.getHostName());
        } else {
            key.append(fileName);
        }
        return key.toString();
    }

    public static Map<String, String> getRowKeyMessage(String rowKey) {
        String ipcID = rowKey.substring(0, rowKey.indexOf("_"));
        String timeStr = rowKey.substring(rowKey.indexOf("_") + 1, rowKey.lastIndexOf("_"));

        String year = timeStr.substring(0, 2);
        String month = timeStr.substring(2, 4);
        String day = timeStr.substring(4, 6);
        String hour = timeStr.substring(6, 8);
        String minute = timeStr.substring(8, 10);
        String second = timeStr.substring(10, 12);

        StringBuilder time = new StringBuilder();
        time = time.append(20).append(year).append("-").append(month).append("-").append(day).append(" ").append(hour).append(":").append(minute).append(":").append(second);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        StringBuilder sj = new StringBuilder();
        sj = sj.append(hour).append(minute);

        Map<String, String> map = new HashMap<>();
        try {
            Date date = sdf.parse(time.toString());
            long timeStamp = date.getTime();
            map.put("ipcID", ipcID);
            map.put("time", String.valueOf(timeStamp));
            map.put("mqkey", time.toString());
            map.put("sj", sj.toString());//sj为动态库同步ES所需字段
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 通过rowKey解析到文件保存的绝对路径
     *
     * @param rowKey rowKey
     * @param type   文件类型
     * @return 绝对路径
     */
    public static String key2absolutePath(String rowKey, FileType type) {
        String ipcId = rowKey.substring(0, rowKey.indexOf("_"));
        String timeStr = rowKey.substring(rowKey.indexOf("_") + 1, rowKey.length());
        String year = timeStr.substring(0, 2);
        String month = timeStr.substring(2, 4);
        String day = timeStr.substring(4, 6);
        String hour = timeStr.substring(6, 8);
        String minute = timeStr.substring(8, 10);
        //String second = timeStr.substring(10, 12);

        String hostName = rowKey.substring(rowKey.lastIndexOf("_") + 1,rowKey.length());

        StringBuilder fileName = new StringBuilder();
        fileName = fileName.append("/opt/data/").append(hostName).append("/").append(ipcId).
                append("/20").append(year).append("/").append(month).append("/").append(day).
                append("/").append(hour).append("/").append(minute).append("/");

        String rowkey1 = rowKey.substring(0,rowKey.lastIndexOf("_"));
        int numType = Integer.parseInt(rowkey1.substring(rowkey1.lastIndexOf("_") + 1, rowkey1.length()));

        if (type == FileType.PICTURE) {
            if (numType == 0) {
                fileName = fileName.append(rowKey).append(".jpg");
            } else if (numType > 0) {
                rowkey1 = rowkey1.substring(0, rowkey1.lastIndexOf("_") + 1) + "00";
                fileName = fileName.append(rowkey1).append("_").append(hostName).append(".jpg");
            } else {
                LOG.warn("rowKey format error :" + rowKey);
            }
        } else if (type == FileType.FACE) {
            if (numType == 0) {
                LOG.info("picture rowKey cannot analysis to face filePath !");
            } else if (numType > 0) {
                fileName = fileName.append(rowKey).append(".jpg");
            } else {
                LOG.warn("rowKey format error :" + rowKey);
            }
        } else if (type == FileType.JSON) {
            if (numType == 0) {
                fileName = fileName.append(rowKey).append(".json");
            } else if (numType > 0) {
                rowkey1 = rowkey1.substring(0, rowkey1.lastIndexOf("_") + 1) + "00";
                fileName = fileName.append(rowkey1).append("_").append(hostName).append(".json");
            } else {
                LOG.warn("rowKey format error :" + rowKey);
            }
        }
        return fileName.toString();
    }

    /**
     * 通过rowKey解析文件保存相对路径
     *
     * @param rowKey rowKey
     * @return 相对路径
     */
    public static String key2relativePath(String rowKey) {
        String ipcId = rowKey.substring(0, rowKey.indexOf("_"));
        String timeStr = rowKey.substring(rowKey.indexOf("_") + 1, rowKey.length());
        String year = timeStr.substring(0, 2);
        String month = timeStr.substring(2, 4);
        String day = timeStr.substring(4, 6);
        String hour = timeStr.substring(6, 8);
        String minute = timeStr.substring(8, 10);
        //String second = timeStr.substring(10, 12);

        String hostName = rowKey.substring(rowKey.lastIndexOf("_") + 1,rowKey.length());

        StringBuilder filePath = new StringBuilder();
        filePath = filePath.append("F:\\data\\").append(hostName).append("\\").append(ipcId).
                append("\\20").append(year).append("\\").append(month).append("\\").append(day).
                append("\\").append(hour).append("\\").append(minute);
        return filePath.toString();
    }

    /**
     * 通过rowKey解析文件名称
     *
     * @param rowKey rowKey
     * @param type   文件类型
     * @return 文件名称
     */
    public static String key2fileName(String rowKey, FileType type) {
        StringBuilder fileName = new StringBuilder();

        String rowkey1 = rowKey.substring(0,rowKey.lastIndexOf("_"));
        int numType = Integer.parseInt(rowkey1.substring(rowkey1.lastIndexOf("_") + 1, rowkey1.length()));

        if (type == FileType.PICTURE) {
            if (numType == 0) {
                fileName = fileName.append(rowKey).append(".jpg");
            } else if (numType > 0) {
                String hostName = rowKey.substring(rowKey.lastIndexOf("_") + 1,rowKey.length());
                rowkey1 = rowkey1.substring(0, rowkey1.lastIndexOf("_") + 1) + "00";
                fileName = fileName.append(rowkey1).append("_").append(hostName).append(".jpg");
            } else {
                LOG.warn("rowKey format error : " + rowKey);
            }
        } else if (type == FileType.FACE) {
            if (numType == 0) {
                LOG.info("picture rowKey cannot analysis to face fileName !");
            } else if (numType > 0) {
                fileName = fileName.append(rowKey).append(".jpg");
            } else {
                LOG.warn("rowKey format error :" + rowKey);
            }
        } else if (type == FileType.JSON) {
            if (numType == 0) {
                fileName = fileName.append(rowKey).append(".json");
            } else if (numType > 0) {
                String hostName = rowKey.substring(rowKey.lastIndexOf("_") + 1,rowKey.length());
                rowkey1 = rowkey1.substring(0, rowkey1.lastIndexOf("_") + 1) + "00";
                fileName = fileName.append(rowkey1).append("_").append(hostName).append(".json");
            } else {
                LOG.warn("rowKey format error : " + rowKey);
            }
        } else {
            LOG.warn("method param is error.");
        }
        return fileName.toString();
    }

    /**
     * 保存字节数组到本地文件
     *
     * @param bytes 字节数组
     * @param rowKey rowKey
     * @param fileName 文件名称
     */
    public static void bytesToFile(byte[] bytes, String rowKey, String fileName) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        //File file = null;
        String filePath = key2relativePath(rowKey);
        File dir = new File(filePath);
        if (!dir.exists()) {//判断文件目录是否存在
            dir.mkdirs();
        }
        try {
            //file = new File(filePath + "/" + fileName);
            fos = new FileOutputStream( dir.getPath()+ File.separator + fileName);
            bos = new BufferedOutputStream(fos);
            bos.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
    private static String key = "3B0383FPAG00883_170926192916_0000000524_00_s100";

    public static void main(String[] args) {
        System.out.println("rowkey        : " + key);
        String picPath = key2absolutePath(key, FileType.PICTURE);
        System.out.println("picture  path : " + picPath);
        String facePath = key2absolutePath(key, FileType.FACE);
        System.out.println("face     Path : " + facePath);
        String jsonPath = key2absolutePath(key, FileType.JSON);
        System.out.println("json     Path : " + jsonPath);
        String relativePath = key2relativePath(key);
        System.out.println("relative Path : " + relativePath);
        String picName = key2fileName(key, FileType.PICTURE);
        System.out.println("picture  Name : " + picName);
        String faceName = key2fileName(key, FileType.FACE);
        System.out.println("face     Name : " + faceName);
        String jsonName = key2fileName(key, FileType.JSON);
        System.out.println("json     Name : " + jsonName);
    }
}
