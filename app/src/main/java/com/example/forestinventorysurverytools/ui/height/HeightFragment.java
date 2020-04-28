package com.example.forestinventorysurverytools.ui.height;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.forestinventorysurverytools.CameraAPI;
import com.example.forestinventorysurverytools.MainActivity;
import com.example.forestinventorysurverytools.MySensorEventListener;
import com.example.forestinventorysurverytools.R;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import static android.content.Context.SENSOR_SERVICE;


public class HeightFragment extends Fragment {

    View root;

    SensorManager mSensorManager;
    MySensorEventListener mMySensorEventListener;

    Handler mCameraHandler;
    HandlerThread mCameraThread;

    ImageButton mBtn_height;
    ImageButton mBtn_calculate;
    ImageButton mBtn_capture;

    double x_height;
    double t_height;
    double new_height;

    float compass;
    float f_angle = 0;
    float t_angle = 0;
    float xy_angle = 0;
    float x_angle = 0;
    float y_angle = 0;

    MainActivity ma = null;

    public HeightFragment(MainActivity ma) {
        this.ma = ma;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_height, container, false);


        mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        mMySensorEventListener = new MySensorEventListener(mSensorManager);

        mBtn_height = (ImageButton) root.findViewById(R.id.Btn_height);
        mBtn_calculate = (ImageButton) root.findViewById(R.id.Btn_calculate);
        mBtn_capture = (ImageButton) root.findViewById(R.id.Btn_capture);

        mBtn_height.setOnClickListener(measureHeight);
        mBtn_calculate.setOnClickListener(getCalculateHeight);
        mBtn_capture.setOnClickListener(takeCapture);
        return root;
    }


    // Toast
    public void showToast(String data) {
        Toast.makeText(root.getContext(), data, Toast.LENGTH_SHORT).show();
    }


    // 이렇게 하게 되면 "theta_vec"에 저장되는 값은
    // [0] 처음 측정했을 때의  f_theta
    // [1] xy_theta - x_theta를 한 y_theta 값
    // [2] y2_theta
    // ....
    // [N] angleYn == n번 측정했을 때의 y_theta (n_theta)

    //Button
    final ImageButton.OnClickListener measureHeight = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View height) {

            mMySensorEventListener.updateOrientationAngles();
            if (!ma.mInputHeight.getText().toString().isEmpty()) {
                if (ma.angle_vec.isEmpty()) {
                    f_angle = Math.abs(mMySensorEventListener.getRoll());
                    ma.angle_vec.add(f_angle);
                    showToast(Integer.toString(ma.angle_vec.size()));
                    x_angle = 90 - ma.angle_vec.elementAt(0);
                } else {
                    t_angle = Math.abs(mMySensorEventListener.getRoll());
                    xy_angle = t_angle - ma.angle_vec.elementAt(0);
                    y_angle = Math.abs(xy_angle - x_angle);
                    ma.angle_vec.add(y_angle);
                }
            }
        }
    };

    /* theta_vec : 구간별 theta 벡터, dist_vec : 구간별 수고 벡터 */

    /**
     * 두번째 고도값 가져오기
     * if(calculate.getId() == R.id.Btn_calculate) {
     * float altitude2 = Math.abs(mMySensorEventListener.getAltitude());
     * ...
     * for(...) { //Up slope
     * h = altitude - altitude2;
     * d = h/ Math.tan(slope);
     * t_height = (Math.tan(angle + slope) * distance) - h;
     * }
     * for(...) { //down slope
     * h = altitude2;
     * d = h/Math.tan(slope);
     * t_height = (Math.tan(angle - slope) * distance) + h;
     * }
     * }
     */



    /*캡쳐*/
    final ImageButton.OnClickListener takeCapture = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View capture) {
            String mPath;

            try{
                Calendar c = Calendar.getInstance();
                String fileName = String.format("%02d%02d_%02d%02d%02d", c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));

                // 내장 메모리 /Pictures에 저장됨. (갤러리에서 확인은 안됨. 폴더에서 확인은 가능) 경우에 따라선 /ScreenShots 에 넣을 수 도 있음.
                mPath = Environment.getExternalStorageDirectory().toString()+  "/Pictures" + "/" + fileName + ".jpg";
                // create bitmap screen capture
                // 화면 이미지 만들기
                ArFragment af = ma.arFragment;


                ArSceneView view = af.getArSceneView();
                final Bitmap mybitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
                final HandlerThread handlerThread = new HandlerThread("PixelCopier");
                handlerThread.start();

                PixelCopy.request(view, mybitmap, (copyResult) -> {
                    if (copyResult == PixelCopy.SUCCESS) {
                        try {
                            saveBitmapToDisk(mybitmap, mPath);
                        } catch (IOException e) {
                            return;
                        }
                    }
                    handlerThread.quitSafely();
                }, new Handler(handlerThread.getLooper()));
                Toast.makeText(ma, mPath, Toast.LENGTH_LONG).show();
            } catch(Throwable e){
                // Several error may come out with file handling or OOM
                e.printStackTrace();
            }



        }
    };


    public void saveBitmapToDisk(Bitmap bitmap, String path) throws IOException {

        //  String path = Environment.getExternalStorageDirectory().toString() +  "/Pictures/Screenshots/";

        Bitmap rotatedImage = bitmap;


        if(ma.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.postRotate(90);
            rotatedImage = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotationMatrix, true);
        }

        File mediaFile = new File(path);

        FileOutputStream fileOutputStream = new FileOutputStream(mediaFile);
        rotatedImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);


        fileOutputStream.flush();
        fileOutputStream.close();


    }




    final ImageButton.OnClickListener getCalculateHeight = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View calculate) {


            if (calculate.getId() == R.id.Btn_calculate) {
                float phoneHeight = Float.valueOf(ma.mInputHeight.getText().toString()) /100f;
                float distance = (float) (Math.tan(x_angle) * phoneHeight);
                compass = Math.abs(mMySensorEventListener.getYaw());
                for (int i = 1; i < ma.angle_vec.size(); i++) {
                    if (ma.height_vec.isEmpty()) {
                        x_height = distance * Math.tan(ma.angle_vec.elementAt(i));
                        ma.height_vec.add(x_height);
                        t_height += x_height;
                    } else {
                        double tmp_height = distance * Math.tan(ma.angle_vec.elementAt(i));
                        new_height = tmp_height - t_height;
                        ma.height_vec.add(new_height);
                        t_height += new_height;
                    }
                }
                t_height += phoneHeight;
                String totalHeightValue = String.format("%.1f", t_height);
                ma.mHeight_val=t_height;
                ma.mHeight_tv.setText("수        고 :" + totalHeightValue + "m");
                ma.mCompass_tv.setText("방        위 :"+compass+"°"+mMySensorEventListener.matchDirection(compass));
            }
        }
    };
}


