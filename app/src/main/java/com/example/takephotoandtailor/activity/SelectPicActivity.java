package com.example.takephotoandtailor.activity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.takephotoandtailor.App;
import com.example.takephotoandtailor.MainActivity;
import com.example.takephotoandtailor.R;
import com.example.takephotoandtailor.fragment.GalleryFragment;
import com.yalantis.ucrop.UCrop;

import java.io.File;

public class SelectPicActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_pic);

        getSupportFragmentManager().beginTransaction().add(R.id.rl_container, new GalleryFragment());
        selectPic();
    }

    private void selectPic() {
        // 通过ucrop库进行图片裁剪
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
                        UCrop.of(Uri.fromFile(new File(path)), Uri.fromFile(dPath))
                                .withAspectRatio(1, 1) // 1比1比例
                                .withMaxResultSize(520, 520) // 返回最大的尺寸
                                .withOptions(options) // 相关参数
                                .start(SelectPicActivity.this);
                    }
                })
                // show 的时候建议使用getChildFragmentManager，
                // tag GalleryFragment class 名
                .show(getSupportFragmentManager(), GalleryFragment.class.getName());
    }


}
