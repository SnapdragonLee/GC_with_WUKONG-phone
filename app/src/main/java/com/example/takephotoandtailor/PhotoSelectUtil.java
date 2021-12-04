package com.example.takephotoandtailor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Created by jinhui on 2017/12/11.
 * <p>
 * CSDN_LQR
 * 图片选择工具类
 * <p>
 * 修改返回bitmap
 */

public class PhotoSelectUtil {

    private static final String TAG = PhotoSelectUtil.class.getSimpleName();

    public static final int REQ_TAKE_PHOTO = 10001;
    public static final int REQ_SELECT_PHOTO = 10002;
    public static final int REQ_ZOOM_PHOTO = 10003;

    private Activity mActivity;

    //FileProvider的主机名：一般是包名+".fileprovider"，严格上是build.gradle中defaultConfig{}中applicationId对应的值+".fileprovider"
    private String AUTHORITIES = BuildConfig.APPLICATION_ID + ".fileprovider";

    private String imgPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/photo.jpg";
    private String cropPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/crop_photo.jpg";

    File imgFile;
    private File cropFile = new File(cropPath);
    private Uri imageUri;
    private Uri cropImageUri;
    boolean mShouldCrop = false;  // 是否裁剪，默认是false


    //剪裁图片宽高比
    private int mAspectX = 1;
    private int mAspectY = 1;
    //剪裁图片大小
    private int mOutputX = 300;
    private int mOutputY = 300;
    PhotoSelectListener mListener;


    /**
     * 可指定是否在拍照或从图库选取照片后进行裁剪
     * <p>
     * 默认裁剪比例1:1，宽度为800，高度为480
     *
     * @param activity   上下文
     * @param listener   选取图片监听
     * @param shouldCrop 是否裁剪
     */
    public PhotoSelectUtil(Activity activity, PhotoSelectListener listener, boolean shouldCrop) {
        mActivity = activity;
        mListener = listener;
        mShouldCrop = shouldCrop;
        AUTHORITIES = activity.getPackageName() + ".fileprovider";
        imgPath = generateImgePath();
    }

    /**
     * 可以拍照或从图库选取照片后裁剪的比例及宽高
     *
     * @param activity 上下文
     * @param listener 选取图片监听
     * @param aspectX  图片裁剪时的宽度比例
     * @param aspectY  图片裁剪时的高度比例
     * @param outputX  图片裁剪后的宽度
     * @param outputY  图片裁剪后的高度
     */
    public PhotoSelectUtil(Activity activity, PhotoSelectListener listener, int aspectX, int aspectY, int outputX, int outputY) {
        this(activity, listener, true);
        mAspectX = aspectX;
        mAspectY = aspectY;
        mOutputX = outputX;
        mOutputY = outputY;
    }

    /**
     * 设置FileProvider的主机名：一般是包名+".fileprovider"，严格上是build.gradle中defaultConfig{}中applicationId对应的值+".fileprovider"
     * <p>
     * 该工具默认是应用的包名+".fileprovider"，如项目build.gradle中defaultConfig{}中applicationId不是包名，则必须调用此方法对FileProvider的主机名进行设置，否则Android7.0以上使用异常
     *
     * @param authorities FileProvider的主机名
     */
    public void setAuthorities(String authorities) {
        this.AUTHORITIES = authorities;
    }

    /**
     * 修改图片的存储路径（默认的图片存储路径是SD卡上 Android/data/应用包名/时间戳.jpg）
     *
     * @param imgPath 图片的存储路径（包括文件名和后缀）
     */
    public void setImgPath(String imgPath) {
        this.imgPath = imgPath;
    }

    /**
     * 拍照
     * 1.现象
     * 在项目中调用相机拍照和录像的时候，android4.x,Android5.x,Android6.x均没有问题,在Android7.x下面直接闪退
     * 2.原因分析
     * Android升级到7.0后对权限又做了一个更新即不允许出现以file://的形式调用隐式APP，需要用共享文件的形式：content:// URI
     * 3.解决方案
     * 下面是打开系统相机的方法，做了android各个版本的兼容:
     */
    public void takePhoto() {
        imgFile = new File(imgPath);
        if (!imgFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            imgFile.getParentFile().mkdirs();
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // 这里用这种传统的方法无法调起相机
            imageUri = FileProvider.getUriForFile(mActivity, AUTHORITIES, imgFile);
            Log.e(TAG, "imageUri = " + imageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //添加这一句表示对目标应用临时授权该Uri所代表的文件
        } else {
            imageUri = Uri.fromFile(imgFile);
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        mActivity.startActivityForResult(intent, REQ_TAKE_PHOTO);
    }

    /**
     * 从图库获取
     */
    public void selectPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        mActivity.startActivityForResult(intent, REQ_SELECT_PHOTO);
    }

    public void attachToActivityForResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PhotoSelectUtil.REQ_TAKE_PHOTO: // 拍照
                    // TODO 返回bitmap
                    cropImageUri = Uri.fromFile(cropFile);
                    // 裁剪方法
                    cropPhoto(imageUri, cropImageUri);
                    break;
                case PhotoSelectUtil.REQ_SELECT_PHOTO:// 图库
                    // TODO 返回bitmap
                    if (hasSdcard()) {
                        cropImageUri = Uri.fromFile(cropFile);
                        Uri newUri = Uri.parse(PhotoUtils.getPath(mActivity, data.getData()));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            newUri = FileProvider.getUriForFile(mActivity, AUTHORITIES, new File(newUri.getPath()));
                        cropPhoto(newUri, cropImageUri);
                    } else {
                        Toast.makeText(mActivity, "设备没有SD卡!", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case PhotoSelectUtil.REQ_ZOOM_PHOTO:// 裁剪
                    Log.e(TAG, "裁剪");
                    // TODO 返回bitmap
                    Bitmap bitmap = PhotoUtils.getBitmapFromUri(cropImageUri, mActivity);
                    if (bitmap != null) {
                        if (mListener != null) {
                            mListener.onFinish(bitmap);
                        }
                    }
                    break;
            }
        }
    }

    /**
     * 检查设备是否存在SDCard的工具方法
     */
    public static boolean hasSdcard() {
        String state = Environment.getExternalStorageState();
        return state.equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * 测试裁剪方法
     *
     * @param imgUri
     * @param cropImageUri
     */
    private void cropPhoto(Uri imgUri, Uri cropImageUri) {
        Log.e(TAG, "测试裁剪方法");
        Intent intent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(imgUri, "image/*");
        intent.putExtra("crop", "true");
        //设置剪裁图片宽高比
        intent.putExtra("mAspectX", mAspectX);
        intent.putExtra("mAspectY", mAspectY);
        //设置剪裁图片大小
        intent.putExtra("mOutputX", mOutputX);
        intent.putExtra("mOutputY", mOutputY);
        intent.putExtra("scale", true);
        //将剪切的图片保存到目标Uri中
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cropImageUri);
        intent.putExtra("return-data", false);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        mActivity.startActivityForResult(intent, REQ_ZOOM_PHOTO);
    }


    /**
     * 裁剪图片
     *
     * @param inputFile
     * @param outputFile
     */
    private void zoomPhoto(File inputFile, File outputFile) {
        Log.e(TAG, "zoomPhoto");
        File parentFile = outputFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        Log.e(TAG, "parentFile =" + parentFile);
        Intent intent = new Intent("com.android.camera.action.CROP");
        // Android7.0以上URI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setDataAndType(cropImageUri, "image/*");
        } else {
//            intent.setDataAndType(Uri.fromFile(inputFile), "image/*");
            intent.setDataAndType(Uri.fromFile(inputFile), "image/*");
        }
        intent.putExtra("crop", "true");

        //设置剪裁图片宽高比
        intent.putExtra("mAspectX", mAspectX);
        intent.putExtra("mAspectY", mAspectY);

        //设置剪裁图片大小
        intent.putExtra("mOutputX", mOutputX);
        intent.putExtra("mOutputY", mOutputY);

        // 是否返回uri
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputFile));
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        mActivity.startActivityForResult(intent, REQ_ZOOM_PHOTO);
    }


    /**
     * 产生图片的路径，带文件夹和文件名，文件名为当前毫秒数
     */
    private String generateImgePath() {
        return getExternalStoragePath() + File.separator + String.valueOf(System.currentTimeMillis()) + ".jpg";
        //        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + String.valueOf(System.currentTimeMillis()) + ".jpg";//测试用
    }


    /**
     * 获取SD下的应用目录
     */
    private String getExternalStoragePath() {
        StringBuilder sb = new StringBuilder();
        sb.append(Environment.getExternalStorageDirectory().getAbsolutePath());
        sb.append(File.separator);
        String ROOT_DIR = "Android/data/" + mActivity.getPackageName();
        sb.append(ROOT_DIR);
        sb.append(File.separator);
        return sb.toString();
    }

    // 回调接口
    public interface PhotoSelectListener {
        void onFinish(Bitmap bitmap);
    }

}
