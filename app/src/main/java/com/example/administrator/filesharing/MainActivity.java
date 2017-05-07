package com.example.administrator.filesharing;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import Utils.AppFolder;
import Utils.FileUtils;
import Utils.IntAndBytes;
import wifi.APHelper;
import wifi.Constant;
import wifi.TCPClient;
import wifi.TCPServer;
import wifi.WifiAdmin;


public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    //所需要申请的权限数组
    private static final String[] permissionsArray = new String[]{
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.WAKE_LOCK
    };
    //还需申请的权限列表
    private List<String> permissionsList = new ArrayList<String>();
    //申请权限后的返回码
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;
    /**
     * 点两次返回按键退出程序的时间
     */
    private long mExitTime;
    public Button bt_shareFile;
    public Button bt_receiveFile;
    public APHelper mAPHelper = null;
    public TCPServer mTCPServer = null;
    public boolean OpenSocketServerPort = false;   //作为端口是否开启的标志
    public TCPClient mTcpClient = null;
    public AppFolder mAppFolder = null;        //用以文件操作
    public String myFolderPath = "";      //app目录
    public String myTempPath = "";        //暂存目录
    public String myFileRevPath = "";     //已接收文件目录

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //检查和申请权限
        checkRequiredPermission(MainActivity.this);
        //用以打开热点
        mAPHelper = new APHelper(MainActivity.this);
        //如果热点已经打开，需要先关闭热点
        if (mAPHelper.isApEnabled()) {
            mAPHelper.setWifiApEnabled(null, false);
        }
        //用以处理SocketServer
        mTCPServer = new TCPServer(MainActivity.this);
        //用以连接Server Socket
        mTcpClient = new TCPClient(MainActivity.this);
        //文件操作
        mAppFolder = new AppFolder();
        if (mAppFolder.createPath("hanhaiKuaiChuan")) {
            //记录下三个文件夹的路径
            myFolderPath = mAppFolder.FolderPath;
            myTempPath = mAppFolder.TempPath;
            myFileRevPath = mAppFolder.FileRevPath;
        } else {
            Toast.makeText(this, "文件夹创建失败", Toast.LENGTH_SHORT).show();
        }
        FileUtils.creatTimeFolder(myTempPath);

        bt_shareFile = (Button) findViewById(R.id.button_shareFile);
        bt_shareFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //打开监听端口
                if (!OpenSocketServerPort) {
                    mTCPServer.StartServer();
                    OpenSocketServerPort = true;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (!mAPHelper.isApEnabled()) {
                            //打开热点
                            if (mAPHelper.setWifiApEnabled(APHelper.createWifiCfg(), true)) {
                                //成功
                                Message APOpenSuceess = new Message();
                                APOpenSuceess.what = APOPENSUCCESS;
                                handler.sendMessage(APOpenSuceess);
                                //Toast.makeText(MainActivity.this, "热点开启", Toast.LENGTH_SHORT).show();
                            } else {
                                //失败
                                Message APOpenFailed = new Message();
                                APOpenFailed.what = APOPENFAILED;
                                handler.sendMessage(APOpenFailed);
                                // Toast.makeText(MainActivity.this, "打开热点失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            //
                        }
                    }
                }).start();

                //向所有连接的节点发送数据
                //mTCPServer.SendFile();

                //打开文件选择器，只是单选
                showFileChooser();
            }
        });
        bt_receiveFile = (Button) findViewById(R.id.button_receiveFile);
        bt_receiveFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 如果热点已经打开，需要先关闭热点
                if (mAPHelper.isApEnabled()) {
                    mAPHelper.setWifiApEnabled(null, false);
                    //当手机当做热点时，自身IP地址为192.168.43.1
                    //GetIpAddress();
                    Toast.makeText(MainActivity.this, "热点关闭", Toast.LENGTH_SHORT).show();
                }
                //这里做接收文件处理
                connectWifi();

                //TCPClient mTcpClient=new TCPClient();
                //此处应作一个判断，当连接上指定WiFi时，连接socketServer
                connectServerSocket();
                //mFileOperate.writeToFile("test1.txt","这是一个测试文本14");
                //mFileOperate.openAssignFolder();
            }
        });
    }

    //连接热点
    public void connectWifi() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //连接指定wifi
                WifiAdmin mWifiAdmin = new WifiAdmin(MainActivity.this);
                mWifiAdmin.openWifi();
                mWifiAdmin.connectAppointedNet();
            }
        }).start();
    }

    //连接创建热点的节点
    public void connectServerSocket() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //连接Server Socket
                mTcpClient.connectServer();
                mTcpClient.receiveFile();
                mTcpClient.sendFile();
            }
        }).start();
    }


    //用于打开文件选择器，选择文件并返回文件地址
    public static final int FILE_SELECT_CODE = 1;

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    String path = FileUtils.getPath(this, uri);
                    String fileName = FileUtils.getFileNameWithSuffix(path);
                    //String fileName=uri.getLastPathSegment();
                    randomNC_file(uri, 4, 4);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * @param uri 待发送的文件地址
     * @param N   生成的编码文件份数
     * @param K   文件切割的份数
     * @return
     */
    public byte[][] randomNC_file(Uri uri, int N, int K) {
        String path = FileUtils.getPath(this, uri);

        File f = new File(path);
        //Java中int的取值范围是2的32次方，最大值是2的31次方，最小值是负值的2的31次方-1
        //2^32次方字节等于4GB,理论可处理的最长文件值
        int fileLen = (int) f.length();
        byte fileContent[] = new byte[fileLen];     //创建合适文件大小的数组
        try {
            InputStream in = new FileInputStream(f);
            //b = new byte[fileLen];
            in.read(fileContent);    //读取文件中的内容到b[]数组
            in.close();
        } catch (IOException e) {
            Toast.makeText(this, "读取文件异常", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return null;
        }

        //把文件名与数据封装在一起
        String fileName = FileUtils.getFileNameWithSuffix(path);
        byte[] fileNameBytes = fileName.getBytes();
        int nameLen = fileNameBytes.length;
        int totalLen = 1 + nameLen + fileLen;     //其中0下标用来存文件名的长度
        int colLen = totalLen / K + (totalLen % K != 0 ? 1 : 0);
        byte[] fileNameAndContent = new byte[K * colLen];
        //存入文件名长度
        fileNameAndContent[0] = (byte) nameLen;
        //存入文件名
        for (int i = 1; i <= nameLen; ++i) {
            fileNameAndContent[i] = fileNameBytes[i - 1];
        }
        //存入文件内容
        for (int i = nameLen + 1; i <= nameLen + fileLen; ++i) {
            fileNameAndContent[i] = fileContent[i - nameLen - 1];
        }
        for (int i = nameLen + fileLen + 1; i < totalLen; ++i) {
            fileNameAndContent[i] = (byte) 0;
        }

        byte[] encodeData = randomNC(fileNameAndContent, K, colLen, N, K);
        int NCol = 4 + 1 + K + colLen;   //其中0-3下标存每行数据长度,4存K值,1+K+colLen为jni返回时数据的列数
        byte[][] dataForSend = new byte[N][NCol];
        byte[] NColBytes = IntAndBytes.int2byte(NCol);
        for (int i = 0; i < N; ++i) {
            for (int j = 0; j < 4; ++j) {
                dataForSend[i][j] = NColBytes[j];
            }
        }
        for (int i = 0; i < N; i++) {
            for (int j = 4; j < NCol; j++) {
                dataForSend[i][j] = encodeData[i * (1 + K + colLen) + j - 4];
            }
        }


        String originData = decodingData(dataForSend, K);

        return null;
    }


    public String decodingData(byte[][] encodeData, int K) {
        //拆分数据
        byte[] colBytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            colBytes[i] = encodeData[0][i];
        }
        int col = IntAndBytes.byte2int(colBytes);
        //把需要解码的数据取出
        int iData = 0;
        byte[] data = new byte[K * (col - 4)];
        for (int i = 0; i < K; i++) {
            for (int j = 4; j < col; j++) {
                data[iData] = encodeData[i][j];
                ++iData;
            }
        }
        byte[] originData = NCDecoding(data, col - 4);
        //读出文件名
        int nameLen = originData[0];
        byte[] nameBytes = new byte[nameLen];
        for (int i = 0; i < nameLen; ++i) {
            nameBytes[i] = originData[i + 1];
        }
        String fileName = new String(nameBytes);
        //此处当fileLen大于Integer.MAX_VALUE(4G)时，会怎么样
        int fileLen = originData.length - (1 + nameLen);
        byte[] inputData = new byte[fileLen];
        for (int i = 0; i < fileLen; ++i) {
            inputData[i] = originData[i + nameLen + 1];
        }
        //mFileOperate.writeToFile(fileName, inputData);
        return fileName;
    }

    //当手机当做热点时，自身IP地址为192.168.43.1
    public String GetIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int i = wifiInfo.getIpAddress();
        String a = (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 24) & 0xFF);
        String b = "0.0.0.0";
        if (a.equals(b)) {
            //a = "192.168.43.1";// 当手机当作WIFI热点的时候，自身IP地址为192.168.43.1
            a = Constant.TCP_ServerIP;
        }
        return a;
    }

    public static final int APOPENSUCCESS = 1;
    public static final int APOPENFAILED = 2;
    public static final int PORTCANUSE = 3;
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case APOPENSUCCESS:
                    Toast.makeText(MainActivity.this, "热点开启", Toast.LENGTH_SHORT).show();
                    break;
                case APOPENFAILED:
                    Toast.makeText(MainActivity.this, "打开热点失败", Toast.LENGTH_SHORT).show();
                    break;
                case PORTCANUSE:
                    bt_shareFile.setEnabled(true);
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    //检查和申请权限
    private void checkRequiredPermission(final Activity activity) {
        for (String permission : permissionsArray) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsList.add(permission);
            }
        }
        //若是permissionsList为空，则说明所有权限已用
        if (permissionsList.size() == 0) {
            return;
        }
        ActivityCompat.requestPermissions(activity, permissionsList.toArray(new String[permissionsList.size()]), REQUEST_CODE_ASK_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int i = 0; i < permissions.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(MainActivity.this, "做一些申请成功的权限对应的事！" + permissions[i], Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "权限被拒绝： " + permissions[i], Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    /**
     * 点击两次退出程序
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();

            } else {
                //执行退出操作
                finish();
                //Dalvik VM的本地方法完全退出app
                android.os.Process.killProcess(android.os.Process.myPid());    //获取PID
                System.exit(0);   //常规java、c#的标准退出法，返回值为0代表正常退出
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native byte[] NCDecoding(byte[] Data, int nLen);

    public native byte[] randomNC(byte[] Data, int row, int col, int N, int K);
}
