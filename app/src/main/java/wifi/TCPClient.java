package wifi;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by Administrator on 2017/4/28 0028.
 * 用于连接socket服务器
 */

public class TCPClient {
    private Socket socket = null;
    private DataInputStream in = null;   //接收
    private DataOutputStream out = null; //发送
    private Context context;
    public TCPClient(Context context){
        this.context=context;
    }

    //连接SocketServer
    public boolean connectServer() {
        try {
            socket = new Socket(Constant.TCP_ServerIP, Constant.TCP_ServerPORT);
            in=new DataInputStream(socket.getInputStream());     //接收
            out = new DataOutputStream(socket.getOutputStream());//发送
        } catch (IOException e) {
            e.printStackTrace();
            //连接失败
            return false;
        }
        //连接成功
        return true;
    }
    //断开连接
    public void disconnectServer(){
        try {
            //关闭流
            out.close();
            in.close();
            //关闭Socket
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //接收来自Server的文件
    public void receiveFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] getBytes=new byte[255];
                while (true) {
                    if (socket.isConnected()) {
                        if (!socket.isInputShutdown()) {
                            try {
                                if(in.read(getBytes,0,255)> -1){
                                    int length=getBytes[0];
                                    byte[] b=new byte[length];
                                    for(int i=0;i<length;++i){
                                        b[i]=getBytes[i+1];
                                    }
                                    String s=new String(b);
                                    Message getServerMsg = new Message();
                                    getServerMsg.what = GETSERVERMSG;
                                    getServerMsg.obj = s;
                                    handler.sendMessage(getServerMsg);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();
    }

    //向Server发送文件
    public void sendFile() {
        byte[] b="这是一个Client发给Server的测试文本".getBytes();
        byte[] b1=new byte[b.length+1];
        b1[0]=(byte)b.length;
        for(int i=0;i<b.length;++i){
            b1[i+1]=b[i];
        }
        try {
            out.write(b1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static final int GETSERVERMSG=1;
    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GETSERVERMSG:
                    Toast.makeText(context, msg.obj.toString(), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };



}
