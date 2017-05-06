#include <jni.h>
#include <string>
#include "gf.c"


extern "C" {
#include <android/log.h>
// 宏定义类似java 层的定义,不同级别的Log LOGI, LOGD, LOGW, LOGE, LOGF。 对就Java中的 Log.i log.d
#define LOG_TAG    "hpc -- JNILOG" // 这个是自定义的LOG的标识
//#undef LOG // 取消默认的LOG
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG, __VA_ARGS__)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN,LOG_TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG, __VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL,LOG_TAG, __VA_ARGS__)

#ifdef __cplusplus
}
#endif



extern "C" {
JNIEXPORT jstring JNICALL Java_com_example_administrator_filesharing_MainActivity_stringFromJNI( JNIEnv* env, jobject thiz );
JNIEXPORT jstring JNICALL Java_com_example_administrator_filesharing_MainActivity_InitGalois( JNIEnv* env, jobject thiz );
JNIEXPORT jstring JNICALL Java_com_example_administrator_filesharing_MainActivity_UninitGalois( JNIEnv* env, jobject thiz );
JNIEXPORT jint JNICALL Java_com_example_administrator_filesharing_MainActivity_ADD( JNIEnv* env, jobject thiz,jint i,jint j );

JNIEXPORT jbyteArray JNICALL Java_com_example_administrator_filesharing_MainActivity_randomNC( JNIEnv* env, jobject thiz, jbyteArray matrixData, jint row,jint col,jint N,jint K);
JNIEXPORT jbyteArray JNICALL Java_com_example_administrator_filesharing_MainActivity_NCDecoding( JNIEnv* env, jobject thiz, jbyteArray arrayData, jint nLen );
JNIEXPORT jbyteArray JNICALL Java_com_example_administrator_filesharing_MainActivity_InverseMatrix( JNIEnv* env, jobject thiz, jbyteArray arrayData, jint nK );

}

/*
 * jni测试
 */
JNIEXPORT jstring JNICALL Java_com_example_administrator_filesharing_MainActivity_stringFromJNI( JNIEnv* env, jobject thiz )
{
    return env->NewStringUTF("Hello22 from JNI bear c++");
}
/*
 * 对网络编码C++库的测试
 */
JNIEXPORT jint JNICALL Java_com_example_administrator_filesharing_MainActivity_ADD( JNIEnv* env, jobject thiz,jint i,jint j )
{
    gf_init(8, prim_poly[8]);
    jint add=gf_add(i,j);
    jint mul=gf_mul(34,45);
    // Release memory
    gf_uninit();
    return mul;
}
/*
 * gf(256)
 * 初始化有限域
 */
JNIEXPORT jstring JNICALL Java_com_example_administrator_filesharing_MainActivity_InitGalois( JNIEnv* env, jobject thiz )
{
    // Initialize the Galois field.
    gf_init(8, prim_poly[8]);
    /*	// test Galois field.
    jbyte a = gf_mul(18, 27);
    LOGD("Test Gf, %d",a);
    */
    return env->NewStringUTF("Hello22 from JNI bear c++");
}
/*
 * 释放有限域
 */
JNIEXPORT jstring JNICALL Java_com_example_administrator_filesharing_MainActivity_UninitGalois( JNIEnv* env, jobject thiz )
{
    // Release memory
    gf_uninit();
    return env->NewStringUTF("Hello22 from JNI bear c++");
}

/**
 * gf(256)
 *注：输入矩阵和输出矩阵都是一维的，需要把矩阵转化为一维数组
 *传入数据矩阵，N、K为编码系数的行列数，输出数据行的格式为K+编码系数+编码后数据
 */
JNIEXPORT jbyteArray JNICALL Java_com_example_administrator_filesharing_MainActivity_randomNC( JNIEnv* env, jobject thiz, jbyteArray matrixData, jint row,jint col,jint N,jint K)
{
    gf_init(8, prim_poly[8]);    //初始化有限域

    jbyte *olddata = (jbyte*)env->GetByteArrayElements(matrixData, 0);
    jsize  oldsize = row * col;
    //int nLen=row*col;

    unsigned char *pData= (unsigned char*)olddata;

    //存入需要编码的矩阵数据
    unsigned int matrix[row][col];
    for (int i=0; i<row; i++)
    {
        int temp=0;
        for (int j=0; j<col; j++)
        {
            //此处出错
            matrix[i][j] = pData[i*col  + j];		// 复制数据到二维数组
            temp=matrix[i][j];
        }
    }

    //生成N*K的随机矩阵
    unsigned char randomMatrix[N][K];
    srand((unsigned)time(NULL));
    for (int i = 0; i < N; i++)      //生成随机矩阵
    {
        int temp=0;
        for (int j = 0; j < K; j++)
        {
            //产生的随机数不能为0,0会造成信息丢失，无法解码
            do{
                randomMatrix[i][j] = rand() % 256;
            }while(randomMatrix[i][j]==0);

            temp=randomMatrix[i][j];
        }
    }

    //编码：随机矩阵*数据矩阵
    unsigned char temp_matrix[N][col];
    for (int i=0; i<N; i++)
    {
        for (int j=0;j<col; j++)
        {
            // Now, the main element must be nonsingular.
            int temp = 0;
            for (int k = 0; k < K; k++){
                temp = gf_add(temp, gf_mul(randomMatrix[i][k], matrix[k][j]));
            }
            temp_matrix[i][j]=temp;
        }
    }
    //把K值，编码系数矩阵和处理后的数据组装在一起生成编码后的矩阵
    unsigned char encodeMatrix[N][1+K+col];
    for(int i=0;i<N;++i){
        encodeMatrix[i][0]=K;
    }
    for(int i=0;i<N;++i){
        for(int j=1;j<=K;++j){
            encodeMatrix[i][j]=randomMatrix[i][j-1];
        }
    }
    for(int i=0;i<N;++i){
        for(int j=K+1;j<1+K+col;++j){
            encodeMatrix[i][j]=temp_matrix[i][j-K-1];
        }
    }

    unsigned char pOriginal[N*(1+K+col)];
    int ii=0;
    for(int i=0;i<N;++i){
        for(int j=0;j<1+K+col;++j){
            pOriginal[ii] = encodeMatrix[i][j];
            ++ii;
        }
    }

    jbyteArray jarrRV = env->NewByteArray(N*(1+K+col));
    jsize myLen = N*(1+K+col);
    jbyte *jby = (jbyte*)pOriginal;
    env->SetByteArrayRegion(jarrRV, 0, myLen, jby);

    env->ReleaseByteArrayElements(matrixData, olddata, 0);
    gf_uninit();
    return jarrRV;
}

/*
 * gf(256)
 * 注：输入矩阵和输出矩阵都是一维的，需要把矩阵转化为一维数组
 * 传入数据行的格式为K+编码系数+编码后数据，输出为解码后的原数据
 */
JNIEXPORT jbyteArray JNICALL Java_com_example_administrator_filesharing_MainActivity_NCDecoding( JNIEnv* env, jobject thiz, jbyteArray arrayData, jint nLen )
{
    gf_init(8, prim_poly[8]);    //初始化有限域

    jbyte *olddata = (jbyte*)env->GetByteArrayElements(arrayData, 0);
    jsize  oldsize = env->GetArrayLength(arrayData);
    unsigned char *pData = (unsigned char*)olddata;

    // LOGD("WJB, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d", pData[0], pData[1], pData[2], pData[3], pData[4], pData[5], pData[6], pData[7], pData[8], pData[9], pData[10], pData[11]);
    int k = (unsigned char)pData[0];    //k值存入数据个数
    int dataLen = nLen-k-1;               //存入数据长度，nLen为单个编码包长度
    // Get the size of sub block.
    // int nBlockSize = ((unsigned char)pData[1])*65536 + ((unsigned char)pData[2])*256 + (unsigned char)pData[3];

    int nRow = k;
    int nCol = k;

    // LOGI("I: hello, Lei");
//	LOGD("hello, %d, %d, %d, %d", nBlockSize, nRow, nCol, nLen);

/*	jbyte **M = new jbyte*[nRow];
	for(int i=0; i<nRow; i++)
	{
		M[i] = new jbyte[nCol];
	}
*/
    //存入编码系数
    unsigned int M[k][k];
    for (int i=0; i<k; i++)
    {
        int temp=0;
        for (int j=0; j<k; j++)
        {
            M[i][j] = pData[i*nLen + 1 + j];		// Copy the coefficient to M.
            temp=M[i][j];
        }
    }
    //存入编码数据
    unsigned char encodeData[k][dataLen];
    for(int i=0;i<k;++i){
        int temp=0;
        for(int j=0;j<dataLen;++j){
            encodeData[i][j]=pData[i*nLen+1+k+j];
            temp=encodeData[i][j];
        }
    }
//	LOGD("Matrix, %d",nLen);

//	LOGD("Coeff, %d,  %d",M[0][0],M[0][1]);
//	LOGD("Coeff, %d,  %d",M[1][0],M[1][1]);


    // Calculate the inverse matrix of M.
/*	jbyte **IM = new jbyte*[nRow];
	for(int i=0; i<nRow; i++)
	{
		M[i] = new jbyte[nCol];
	}
*/
    unsigned int IM[k][k];

    // Init
    for (int i=0; i<k; i++)
    {
        for	(int j=0; j<k; j++)
        {
            if	(i==j)
            {
                IM[i][j] = 1;
            }
            else
            {
                IM[i][j] = 0;
            }
        }
    }

    /*
    LOGD("IM, %d,  %d,  %d",IM[0][0],IM[0][1],IM[0][2]);
    LOGD("IM, %d,  %d,  %d",IM[1][0],IM[1][1],IM[1][2]);
    LOGD("IM, %d,  %d,  %d",IM[2][0],IM[2][1],IM[2][2]);
    */
    // Calculate the inverse matrix. The matrix must be full rank, which could be guaranteed by the property of MDS code.


    /************************************************************************/
    /* Step 1. Change to a lower triangle matrix.                           */
    /************************************************************************/
    for (int i=0; i<nCol; i++)
    {
        for (int j=i+1; j<nCol; j++)
        {
            // Now, the main element must be nonsingular.
            GFType temp = gf_div(M[j][i], M[i][i]);

            for (int z=0; z<nCol; z++)
            {
                M[j][z] = gf_add(M[j][z], gf_mul(temp, M[i][z]));
                IM[j][z] = gf_add(IM[j][z], gf_mul(temp, IM[i][z]));
            }
        }
    }
    /************************************************************************/
    /* Step 2. Only the elements on the diagonal are non-zero.                  */
    /************************************************************************/
    for (int i=1;i<nCol;i++)
    {
        for (int j=0; j<i;j++)
        {
            GFType temp = gf_div(M[j][i], M[i][i]);
            for (int z=0; z<nCol; z++)
            {
                M[j][z] = gf_add(M[j][z], gf_mul(temp, M[i][z]));
                IM[j][z] = gf_add(IM[j][z], gf_mul(temp, IM[i][z]));
            }
        }
    }
    /************************************************************************/
    /* Step 3. The elements on the diagonal are 1.                  */
    /************************************************************************/
    for (int i=0; i<nCol; i++)
    {
        if (M[i][i]!=1)
        {
            GFType temp = M[i][i];
            for (int z=0; z<nCol; z++)
            {
                M[i][z] = gf_div(M[i][z], temp);
                IM[i][z] = gf_div(IM[i][z], temp);
            }
        }
    }
//	LOGD("InverseMatrix, %d",IM[0][0]);
/*	LOGD("2Coeff, %d,  %d,  %d",M[0][0],M[0][1],M[0][2]);
	LOGD("2Coeff, %d,  %d,  %d",M[1][0],M[1][1],M[1][2]);
	LOGD("2Coeff, %d,  %d,  %d",M[2][0],M[2][1],M[2][2]);
	// M is an identity matrix, now.

	LOGD("IM, %d,  %d,  %d",IM[0][0],IM[0][1],IM[0][2]);
	LOGD("IM, %d,  %d,  %d",IM[1][0],IM[1][1],IM[1][2]);
	LOGD("IM, %d,  %d,  %d",IM[2][0],IM[2][1],IM[2][2]);
*/
    /************************************************************************/
    /* The inverse matrix is in matrix IM, now.                                  */
    /************************************************************************/
    int nAverageLen = dataLen;					// nAverageLen is the length of the original data.
    // New memory to store original data.
    unsigned char pOriginal[nAverageLen*k];
    int ii=0;
    for (int i=0; i<k; i++)
    {
        for (int j=0;j<dataLen; j++)
        {
            // Now, the main element must be nonsingular.
            int temp = 0;
            for (int ki = 0; ki < k; ki++){
                temp = gf_add(temp, gf_mul(IM[i][ki], encodeData[ki][j]));
            }
            pOriginal[ii]= temp;
            ++ii;
        }
    }


//	LOGD("WJB2, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d", pOriginal[0], pOriginal[1], pOriginal[2], pOriginal[3], pOriginal[4], pOriginal[5], pOriginal[6], pOriginal[7], pOriginal[8], pOriginal[9], pOriginal[10], pOriginal[11]);

//	LOGD("hello, %d, %d, %d, %d, %d, %d, %d", nAverageLen*k, pOriginal[0], pOriginal[1], pOriginal[2], pOriginal[3], pOriginal[4], pOriginal[5]);

    jbyteArray jarrRV = env->NewByteArray(nAverageLen*k);
    jsize myLen = nAverageLen*k;
    jbyte *jby = (jbyte*)pOriginal;
    env->SetByteArrayRegion(jarrRV, 0, myLen, jby);

    // Delete reference
    //env->DeleteLocalRef(arrayData);

    env->ReleaseByteArrayElements(arrayData, olddata, 0);
    gf_uninit();
    return jarrRV;
}

/*
 * 注：矩阵转化为一维数组
 * 矩阵在有限域上的求逆方法
 */
JNIEXPORT jbyteArray JNICALL Java_com_example_administrator_filesharing_MainActivity_InverseMatrix( JNIEnv* env, jobject thiz, jbyteArray arrayData, jint nK )
{
    jbyte *olddata = (jbyte*)env->GetByteArrayElements(arrayData, 0);

    jsize  oldsize = env->GetArrayLength(arrayData);
    unsigned char *pData = (unsigned char*)olddata;
    int k = nK;
    int nCol = nK;

    unsigned int M[k][k];
    // k = nCol = nRow;
    for (int i=0; i<k; i++)
    {
        for (int j=0; j<k; j++)
        {
            M[i][j] = pData[i*k+j];  // Copy the coefficient to M.
        }
    }

    unsigned int IM[k][k];

    // Init
    for (int i=0; i<k; i++)
    {
        for	(int j=0; j<k; j++)
        {
            if	(i==j)
            {
                IM[i][j] = 1;
            }
            else
            {
                IM[i][j] = 0;
            }
        }
    }
    /************************************************************************/
    /* Step 1. Change to a lower triangle matrix.                           */
    /************************************************************************/
    for (int i=0; i<nCol; i++)
    {
        for (int j=i+1; j<nCol; j++)
        {
            // Now, the main element must be nonsingular.
            GFType temp = gf_div(M[j][i], M[i][i]);

            for (int z=0; z<nCol; z++)
            {
                M[j][z] = gf_add(M[j][z], gf_mul(temp, M[i][z]));
                IM[j][z] = gf_add(IM[j][z], gf_mul(temp, IM[i][z]));
            }
        }
    }
    /************************************************************************/
    /* Step 2. Only the elements on the diagonal are non-zero.                  */
    /************************************************************************/
    for (int i=1;i<nCol;i++)
    {
        for (int j=0; j<i;j++)
        {
            GFType temp = gf_div(M[j][i], M[i][i]);
            for (int z=0; z<nCol; z++)
            {
                M[j][z] = gf_add(M[j][z], gf_mul(temp, M[i][z]));
                IM[j][z] = gf_add(IM[j][z], gf_mul(temp, IM[i][z]));
            }
        }
    }
    /************************************************************************/
    /* Step 3. The elements on the diagonal are 1.                  */
    /************************************************************************/
    for (int i=0; i<nCol; i++)
    {
        if (M[i][i]!=1)
        {
            GFType temp = M[i][i];
            for (int z=0; z<nCol; z++)
            {
                M[i][z] = gf_div(M[i][z], temp);
                IM[i][z] = gf_div(IM[i][z], temp);
            }
        }
    }
/*
	LOGD("2Coeff, %d,  %d,  %d",IM[0][0],IM[0][1],IM[0][2]);
	LOGD("2Coeff, %d,  %d,  %d",IM[1][0],IM[1][1],IM[1][2]);
	LOGD("2Coeff, %d,  %d,  %d",IM[2][0],IM[2][1],IM[2][2]);
*/
    unsigned char IMCopy[k*k];
    for (int i=0; i<k; i++)
    {
        for	(int j=0;j<k;j++)
        {
            IMCopy[i*k+j] = IM[i][j];
        }
    }

    jbyteArray jarrRV = env->NewByteArray(k*k);
    jsize myLen = k*k;
    jbyte *jby = (jbyte*)IMCopy;
    env->SetByteArrayRegion(jarrRV, 0, myLen, jby);

    env->ReleaseByteArrayElements(arrayData, olddata, 0);
    // Release memory
    gf_uninit();
    return jarrRV;
}


