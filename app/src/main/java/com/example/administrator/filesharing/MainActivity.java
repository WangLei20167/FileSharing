package com.example.administrator.filesharing;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
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
import Utils.LocalInfor;
import msg.MsgValue;
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
    //用来实现左侧导航栏
    private DrawerLayout mDrawerLayout;

     //点两次返回按键退出程序的时间
    private long mExitTime;
    public Button bt_buildConnect;
    public Button bt_joinConnect;
    public Button bt_shareFile;


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

        //文件操作
        mAppFolder = new AppFolder();
        if (mAppFolder.createPath("hanhaiKuaiChuan")) {
            //记录下三个文件夹的路径
            myFolderPath = mAppFolder.FolderPath;
            myTempPath = mAppFolder.TempPath;
            myFileRevPath = mAppFolder.FileRevPath;

            //用以打开热点,如果ap已经打开则先关闭
            mAPHelper = new APHelper(MainActivity.this);
            if (mAPHelper.isApEnabled()) {
                mAPHelper.setWifiApEnabled(null, false);
            }
            //用以处理SocketServer
            mTCPServer = new TCPServer(MainActivity.this,myTempPath,myFileRevPath);
            //用以连接Server Socket
            mTcpClient = new TCPClient(MainActivity.this,myTempPath,myFileRevPath,handler);
        } else {
            Toast.makeText(this, "文件夹创建失败,app无法正常使用", Toast.LENGTH_SHORT).show();
        }


        //用来实现左侧导航栏
        Toolbar toolbar=(Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawerLayout=(DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navView=(NavigationView)findViewById(R.id.nav_view);
        ActionBar actionBar=getSupportActionBar();
        if(actionBar !=null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }
        //设置默认选中项
        navView.setCheckedItem(R.id.nav_openFolder);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener(){
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){
                    case R.id.nav_openFolder:
                        //打开应用文件夹
                        FileUtils.openAssignFolder(MainActivity.this, myFolderPath);
                        break;
                    case R.id.nav_feedback:
                        //用户反馈
                        Toast.makeText(MainActivity.this, "功能未实现", Toast.LENGTH_SHORT).show();

                        break;
                    case R.id.nav_softversion:
                        /**菜单中“版本”选项的弹出显示版本信息的对话框*/
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("软件版本")
                                .setMessage("版本号：" + BuildConfig.VERSION_CODE + "\n版本名：" + BuildConfig.VERSION_NAME)
                                .setPositiveButton("确定", null)
                                .show();
                        break;
                    case R.id.nav_softdescribe:
                        /**菜单中“软件描述”选项的弹出对话框*/
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("软件描述")
                                .setMessage("这是一个利用网络编码安全方案的文件共享项目。\n网址：https://github.com/WangLei20167/FileSharing")
                                .setPositiveButton("确定", null)
                                .show();
                        break;
                    case R.id.nav_aboutus:
                        //关于我们
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("关于我们")
                                .setMessage("瀚海制作 \n邮箱：1092951104@qq.com")
                                .setPositiveButton("确定", null)
                                .show();
                        break;
                    default:
                        //关闭导航栏
                        mDrawerLayout.closeDrawers();
                        break;
                }
                return true;
            }
        });



        bt_buildConnect=(Button)findViewById(R.id.button_buildConnect);
        bt_buildConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //打开监听端口
                //mTCPServer.StartServer();
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
                                APOpenSuceess.what = MsgValue.APOPENSUCCESS;
                                handler.sendMessage(APOpenSuceess);
                                //Toast.makeText(MainActivity.this, "热点开启", Toast.LENGTH_SHORT).show();
                            } else {
                                //失败
                                Message APOpenFailed = new Message();
                                APOpenFailed.what = MsgValue.APOPENFAILED;
                                handler.sendMessage(APOpenFailed);
                                // Toast.makeText(MainActivity.this, "打开热点失败", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            //
                        }
                    }
                }).start();
            }
        });
        bt_joinConnect=(Button)findViewById(R.id.button_joinConnect);
        bt_joinConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 如果热点已经打开，需要先关闭热点
                if (mAPHelper.isApEnabled()) {
                    mAPHelper.setWifiApEnabled(null, false);

                    mTCPServer.CloseServer();
                    //当手机当做热点时，自身IP地址为192.168.43.1
                    //GetIpAddress();
                    Toast.makeText(MainActivity.this, "热点关闭", Toast.LENGTH_SHORT).show();
                }
                //连接WiFi
                connectWifi();


            }
        });


        bt_shareFile = (Button) findViewById(R.id.button_shareFile);
        bt_shareFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //打开文件选择器，只是单选
                showFileChooser();
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
                //连接socket
                connectServerSocket();
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
//                    String path = FileUtils.getPath(this, uri);
//                    String fileName = FileUtils.getFileNameWithSuffix(path);
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

        //向连接的节点发送数据
        mTCPServer.SendFile(dataForSend,N);
        //String originData = decodingData(dataForSend, K);

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


    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgValue.REVFINISH:
                    //接收数据成功，开始解码操作
                    int fileNum=msg.arg1;
                    String tempDataPath=msg.obj.toString();
                    Toast.makeText(MainActivity.this, "接收数据成功开始解码", Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.APOPENSUCCESS:
                    Toast.makeText(MainActivity.this, "热点开启", Toast.LENGTH_SHORT).show();
                    break;
                case MsgValue.APOPENFAILED:
                    Toast.makeText(MainActivity.this, "打开热点失败", Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native byte[] NCDecoding(byte[] Data, int nLen);

    public native byte[] randomNC(byte[] Data, int row, int col, int N, int K);
}
