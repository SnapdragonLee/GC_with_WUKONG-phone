package com.example.takephotoandtailor;


import java.io.*;
import java.net.Socket;

import android.util.Base64;
import android.util.Log;

/**
 * 使用方法，在声明类后，设定imgPath，新建线程并start该线程
 * 样例代码如下
 * String imgPath = "your Img Path"
 * MyClient client = new MyClient();
 * client.setImgPath(imgPath);
 * new Thread(client).start();
 * String ans = client.getAns();
 * 调用getAns得到答案
 */
public class MyClient implements Runnable {
    private static final String TAG = MyClient.class.getSimpleName();
    private final static String BASE_PATH = "";
    private Socket socket = null;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private final String ADDRESS = "192.168.43.31";
    private final int PORT = 1017;
    private String imgPath;
    private String ans = null;

    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    public String getAns() {
        synchronized (this) {
            return ans;
        }
    }

    /**
     * 声明类后调用该方法连接服务端并得到答案，答案存储在ans中
     */
    @Override
    public void run() {
        try {
            synchronized (this) {
                Log.i(TAG,"connected");
                byte[] a=readImg(imgPath);
                Log.i(TAG,a.toString());
                socket = new Socket(ADDRESS, PORT);
                Log.i(TAG,"s"+socket.toString());
                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();
                ans = callServer(BASE_PATH + this.imgPath);
                endConnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG,e.getMessage());
        }
    }

    /**
     * 与服务端交互得到 答案json
     * json形式请参考 server分支下的example.json
     *
     * @param imgData 图片的二进制字节流数据，可通过下文的 readImg()从绝对路径读取，也可以自行构建
     * @return String格式的Json数据，可以用java内的json相关包处理
     * @throws IOException 若网络或服务器出现问题则会产生
     */
    private String callServer(byte[] imgData) throws IOException {
        byte[] b64img = Base64.encode(imgData, Base64.NO_WRAP);

        outputStream.write(intToByteArray(b64img.length));
        try {
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        outputStream.write(b64img);
        byte[] bytes = new byte[1024 * 1024];
        int len = inputStream.read(bytes);
        if (len < 0) {
            return "";
        }
        return new String(new String(bytes, 0, len).getBytes(), "UTF-8");
    }

    /**
     * 与服务端交互得到 答案json
     * json形式请参考 example.json
     *
     * @param imgPath 照片的路径，最好为绝对路径，相对路径可能带来未知的错误
     * @return String格式的Json数据，可以用java内的json相关包处理
     * @throws IOException 若网络或服务器出现问题则会产生
     *                     当文件不存在时产生
     *                     同时 当照片格式不合法时会产生 RuntimeException("Illegal IMG file name")
     *                     当照片尾缀为不合法类型会由 ImageIO.write 抛出异常
     */
    private String callServer(String imgPath) throws IOException {
        return callServer(readImg(imgPath));
    }

    /**
     * 通过绝对路径读取照片二进制字节流数据
     *
     * @param imgPath 照片的路径，最好为绝对路径，相对路径可能带来未知的错误
     * @return 照片的二进制字节流数据
     * @throws IOException 当文件不存在时产生
     *                     同时 当照片格式不合法时会产生 RuntimeException("Illegal IMG file name")
     *                     当照片尾缀为不合法类型会由 ImageIO.write 抛出异常
     */
    private byte[] readImg(String imgPath) throws IOException {
        this.imgPath = imgPath;
        return readLocalFile();
    }

    private byte[] readLocalFile() throws IOException {
        File file = new File(imgPath);
        InputStream inputStream = new FileInputStream(file);
        byte[] data = toByteArray(inputStream);
        inputStream.close();
        return data;
    }

    private byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 4];
        int n = 0;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * 结束连接时调用此方法，client将不可以再执行操作，需要重新调用 run
     *
     * @throws IOException socket若尚未连接则会产生
     */
    private void endConnect() throws IOException {
        socket.close();
    }

    /**
     * 工具功能，返回数字的大端存储bytes
     *
     * @param i 数字
     * @return 答案数组
     */
    public static byte[] intToByteArray(int i) {
        byte[] result = new byte[4];
        result[0] = (byte) ((i >> 24) & 0xFF);
        result[1] = (byte) ((i >> 16) & 0xFF);
        result[2] = (byte) ((i >> 8) & 0xFF);
        result[3] = (byte) (i & 0xFF);
        return result;
    }
}
