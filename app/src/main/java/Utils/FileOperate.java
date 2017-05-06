package Utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Administrator on 2017/5/5 0005.
 * 用来存放接收到的文件
 */

public class FileOperate {
    private Context context;
    private String myFolderPath="";

    public FileOperate(Context context){
        this.context=context;
    }

    /**
     * @param folderName 文件夹名
     * @return 返回文件夹操作路径，后带斜杠
     */
    public boolean createPath(String folderName) {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            String mainPath = Environment.getExternalStorageDirectory().getPath() + File.separator + folderName;
            File destDir = new File(mainPath);
            if (!destDir.exists()) {
                //如果不存在则创建
                destDir.mkdirs();//在根创建了文件夹hello
            }
            //String folderPath = mainPath + File.separator;
            myFolderPath=mainPath + File.separator;
            return true;
            //return folderPath;
        }
        return false;
    }

    /**
     *
     * @param fileName 创建的文件名（带扩展名）
     * @param inputData 向文件中写入的内容
     */
    public void writeToFile(String fileName,String inputData){
        //把IP服务器的IP写入文件
        String toFile = myFolderPath + fileName;
        File myFile = new File(toFile);
        if (!myFile.exists()) {   //不存在则创建
            try {
                myFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            //传递一个true参数，代表不覆盖已有的文件。并在已有文件的末尾处进行数据续写,false表示覆盖写
            FileWriter fw = new FileWriter(myFile, false);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(inputData);
            //bw.write("测试文本");
            bw.flush();
            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void openAssignFolder(){

        File file = new File(myFolderPath);
        if(null==file || !file.exists()){
            return;
        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(file), "file/*");
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }
}
