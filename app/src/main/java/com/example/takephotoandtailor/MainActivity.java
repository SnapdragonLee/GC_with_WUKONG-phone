package com.example.takephotoandtailor;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.blankj.utilcode.util.ActivityUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.example.takephotoandtailor.activity.SelectPicActivity;
import com.example.takephotoandtailor.fragment.GalleryFragment;
import com.yalantis.ucrop.UCrop;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

/**
 * Android 7.0 拍照、图库裁剪
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Button mBtnTakePhoto;
    private Button mBtnSelectPhoto;
    private TextView mTvPath;
    private TextView mTvUri;
    private ImageView mIvPic;
    private Button btn_ucrop;
    private TextView mResult;

    private LQRPhotoSelectUtils mLqrPhotoSelectUtils;
    // 返回bitmap
    private PhotoSelectUtil photoSelectUtil;
    // 是否返回bitmap
    private boolean isBitmap = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        init(isBitmap);
        initListener();
    }

    private void init(boolean isBitmap) {
        if (isBitmap) {
            // TODO 返回bitmap
            photoSelectUtil = new PhotoSelectUtil(this, new PhotoSelectUtil.PhotoSelectListener() {
                @Override
                public void onFinish(Bitmap bitmap) {
                    mIvPic.setImageBitmap(bitmap);
                }

            }, 1, 1, 800, 480);
        } else {
            // 1、创建LQRPhotoSelectUtils（一个Activity对应一个LQRPhotoSelectUtils）
            // TODO 返回图片路径
            mLqrPhotoSelectUtils = new LQRPhotoSelectUtils(this, new LQRPhotoSelectUtils.PhotoSelectListener() {

                @Override
                public void onFinish(File outputFile, Uri outputUri) {
                    // 4、当拍照或从图库选取图片成功后回调
                    Log.i(TAG,"in onfinish");
                    mTvPath.setText(outputFile.getAbsolutePath());
                    mTvUri.setText(outputUri.toString());
                    Glide.with(MainActivity.this).load(outputUri).into(mIvPic);

                    MyClient client = new MyClient();
                    client.setImgPath(mTvPath.getText().toString());
                    new Thread(client).start();
                    Log.i(TAG,"thread begin");
                    String ans = client.getAns();
                    String solution = ansAnalyse(ans);
                    mResult.setText("re:  "+solution);


                }
            }, true);
            // mLqrPhotoSelectUtils.setAuthorities(MainActivity.this.getPackageName() + ".fileprovider");
            // mLqrPhotoSelectUtils.setImgPath(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg");
        }

    }
    private String ansAnalyse(String ans) {
        JsonTool jsonTool;
        if (ans.equals("")) {
            return "";
        }
        try {
            jsonTool = new JsonTool(ans);
        } catch (Exception e) {
            return "";
        }
        if (jsonTool.getNumbers() == 0) {
            return "没有找到垃圾哦";
        }
        List<JsonTool.DataEle> data = jsonTool.getData();
        StringBuilder re = new StringBuilder("我看到了" + jsonTool.getNumbers() + "个垃圾,他们是");
        for (JsonTool.DataEle dataEle : data) {
            re.append(dataEle.getName()).append("是").append(dataEle.getType()).append("。");
        }
        Log.i(TAG, re.toString());
        return re.toString();
    }

    private void initListener() {
        mBtnTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 3、申请权限，调用拍照方法
                PermissionGen.with(MainActivity.this)
                        .addRequestCode(LQRPhotoSelectUtils.REQ_TAKE_PHOTO)
                        .permissions(Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA)
                        .request();
            }
        });
        mBtnSelectPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 3、调用从图库选取图片方法
                PermissionGen.needPermission(MainActivity.this,
                        LQRPhotoSelectUtils.REQ_SELECT_PHOTO,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE}
                );
            }
        });

        btn_ucrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                ActivityUtils.startActivity(SelectPicActivity.class);
                // TODO 未配置读取权限，配置下即可;
                new GalleryFragment()
                        .setListener(new GalleryFragment.OnSelectedListener() {
                            @Override
                            public void onSelectedImage(String path) {
                                UCrop.Options options = new UCrop.Options();
                                // 设置图片处理的格式JPEG
                                options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
                                // 设置压缩后的图片精度
                                options.setCompressionQuality(96);

                                // 得到头像的缓存地址
                                File dPath = App.getPortraitTmpFile();
                                // 发起剪切
                               /* UCrop.of(Uri.fromFile(new File(path)), Uri.fromFile(dPath))
                                        .withAspectRatio(1, 1) // 1比1比例
                                        .withMaxResultSize(520, 520) // 返回最大的尺寸
                                        .withOptions(options) // 相关参数
                                        .start(MainActivity.this);*/
                                UCrop.of(Uri.fromFile(new File(path)), Uri.fromFile(dPath))
                                        .withAspectRatio(16, 9)
                                        .withMaxResultSize(520, 520)
                                        .start(MainActivity.this);
                            }
                        })
                        // show 的时候建议使用getChildFragmentManager，
                        // tag GalleryFragment class 名
                        .show(getSupportFragmentManager(), GalleryFragment.class.getName());
            }
        });
    }

    /**
     * 获取到图片进行裁剪
     *
     * @param path
     */
    private void uCrop(String path) {
        // 获取
        UCrop.Options options = new UCrop.Options();
        // 设置图片处理的格式JPEG
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        // 设置压缩后的图片精度
        options.setCompressionQuality(96);

        // 得到头像的缓存地址
        File dPath = App.getPortraitTmpFile();
        Log.e(TAG, "得到头像的缓存地址: " + dPath);
        // 发起剪切
        UCrop.of(Uri.fromFile(new File(path)), Uri.fromFile(dPath))
                .withAspectRatio(1, 1) // 1比1比例
                .withMaxResultSize(520, 520) // 返回最大的尺寸
                .withOptions(options) // 相关参数
                .start(MainActivity.this);
    }

    private void initView() {
        mBtnTakePhoto = (Button) findViewById(R.id.btnTakePhoto);
        mBtnSelectPhoto = (Button) findViewById(R.id.btnSelectPhoto);
        mTvPath = (TextView) findViewById(R.id.tvPath);
        mTvUri = (TextView) findViewById(R.id.tvUri);
        mIvPic = (ImageView) findViewById(R.id.ivPic);
        btn_ucrop = (Button) findViewById(R.id.btn_ucrop);
        mResult = (TextView) findViewById(R.id.tvResult);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 2、在Activity中的onActivityResult()方法里与LQRPhotoSelectUtils关联
        if (isBitmap) {
            photoSelectUtil.attachToActivityForResult(requestCode, resultCode, data);
        } else {
            mLqrPhotoSelectUtils.attachToActivityForResult(requestCode, resultCode, data);
        }

        // 处理裁剪结果
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            loadPortrait(resultUri);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            Log.e(TAG, "裁剪出现错误: " + cropError.toString());
        }

    }

    /**
     * 加载Uri到当前的头像中
     *
     * @param uri Uri
     */
    private void loadPortrait(Uri uri) {
        // 得到头像地址
        String headUrl = uri.getPath();
        Log.e(TAG, "得到头像地址 ->headUrl = " + headUrl);
        mTvUri.setText(uri.toString());
        Glide.with(this).load(uri.toString()).into(mIvPic);
    }


    @PermissionSuccess(requestCode = LQRPhotoSelectUtils.REQ_TAKE_PHOTO)
    private void takePhoto() {
        Log.e(TAG, "拍照权限请求成功");
        if (isBitmap) {
            photoSelectUtil.takePhoto();
        } else {
            mLqrPhotoSelectUtils.takePhoto();
        }
    }

    @PermissionSuccess(requestCode = LQRPhotoSelectUtils.REQ_SELECT_PHOTO)
    private void selectPhoto() {
        Log.e(TAG, "图库权限请求成功");
        if (isBitmap) {
            photoSelectUtil.selectPhoto();
        } else {
            mLqrPhotoSelectUtils.selectPhoto();
        }

    }

    @PermissionFail(requestCode = LQRPhotoSelectUtils.REQ_TAKE_PHOTO)
    private void showTip1() {
        //        Toast.makeText(getApplicationContext(), "不给我权限是吧，那就别玩了", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "拍照权限请求失败");
        showDialog();
    }

    @PermissionFail(requestCode = LQRPhotoSelectUtils.REQ_SELECT_PHOTO)
    private void showTip2() {
        //        Toast.makeText(getApplicationContext(), "不给我权限是吧，那就别玩了", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "图库权限请求失败");
        showDialog();
    }

    private void showDialog() {
        //创建对话框创建器
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //设置对话框显示小图标
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        //设置标题
        builder.setTitle("权限申请");
        //设置正文
        builder.setMessage("在设置-应用-权限 中开启相机、存储权限，才能正常使用拍照或图片选择功能");

        //添加确定按钮点击事件
        builder.setPositiveButton("去设置", new DialogInterface.OnClickListener() {//点击完确定后，触发这个事件

            @Override
            public void onClick(DialogInterface dialog, int which) {
                //这里用来跳到手机设置页，方便用户开启权限
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        //添加取消按钮点击事件
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        //使用构建器创建出对话框对象
        AlertDialog dialog = builder.create();
        dialog.show();//显示对话框
    }


}
