package com.example.forestinventorysurverytools.ui.diameter;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

//import com.example.forestinventorysurverytools.CameraAPI;
import com.example.forestinventorysurverytools.FirstScreen;
import com.example.forestinventorysurverytools.Info;
import com.example.forestinventorysurverytools.MainActivity;
import com.example.forestinventorysurverytools.MySensorEventListener;
import com.example.forestinventorysurverytools.R;
//import com.example.forestinventorysurverytools.ui.distance.DistanceFragment;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.TransformableNode;

import org.apache.poi.ss.formula.functions.T;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.SENSOR_SERVICE;

public class DiameterFragment extends Fragment implements Scene.OnUpdateListener, LocationListener{


    //View
    View root;


    //Sensor
    SensorManager mSensorManager;
    LocationManager mLocationManager;
    MySensorEventListener mMySensorEventListener;


    //Data
    ArrayList<Info> ai;
    double longitude;
    double latitude;
    double altitude;
    float compass;
    public int id;
    float diameterValue;
    String diameter;


    //Activity
    MainActivity ma = null;
    public DiameterFragment(MainActivity ma) {this.ma = ma; ai=ma.infoArray;}


    //SeekBar
    public SeekBar radiusbar;
    public SeekBar lr_rot;
    public SeekBar fb_rot;


    //ImageButton
    public ImageButton mTop;
    public ImageButton mBottom;
    public ImageButton mRight;
    public ImageButton mLeft;


    //TextView
    public TextView radius_controller;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_diameter, null);
        id = 0;


        //Sensor
        mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        mLocationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        mMySensorEventListener = new MySensorEventListener(ma, mSensorManager);


        //ImageButton
        mTop = (ImageButton)root.findViewById(R.id.top);
        mBottom = (ImageButton)root.findViewById(R.id.bottom);
        mRight = (ImageButton)root.findViewById(R.id.right);
        mLeft = (ImageButton)root.findViewById(R.id.left);

        mTop.setOnClickListener(controll_BtnTop);
        mBottom.setOnClickListener(controll_BtnBottom);
        mRight.setOnClickListener(controll_BtnRight);
        mLeft.setOnClickListener(controll_BtnLeft);




        //SeekBar
        radius_controller = (TextView) root.findViewById(R.id.radi_controller);
        radiusbar = (SeekBar) root.findViewById(R.id.radi_controller1);
        radiusbar.setMin(30);
        radiusbar.setMax(800);
        radiusbar.setProgress(ma.radi);
        radiusbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                Frame frame = ma.arFragment.getArSceneView().getArFrame();
                Pose objectPose = ma.anchor.getPose();
                Pose cameraPose = frame.getCamera().getPose();

                //Get the Anchor Pose
                ma.dx = objectPose.tx() - cameraPose.tx();
                ma.dy = objectPose.ty() - cameraPose.ty();
                ma.dz = objectPose.tz() - cameraPose.tz();
                ma.distanceMeters = (float) Math.sqrt(ma.dx * ma.dx + ma.dy * ma.dy + ma.dz * ma.dz);
                String distance = String.format("%.1f", ma.distanceMeters);
                ma.mDistance_tv.setText("거        리 : " + distance + "m");

                ma.radi = progress;
                ma.initModel();
                ma.infoArray.get(ma.tree_id).getNode().setRenderable(ma.modelRenderable);
                ma.arFragment.getArSceneView().getScene().addOnUpdateListener(ma.arFragment);
                diameterValue = (((ma.radi*2)/10) * ((ma.distanceMeters*100)+((((ma.radi*2)/10)+2)))/(ma.distanceMeters * 100));
                diameter = String.format("%.1f", diameterValue);
                ma.mDiameter_tv.setText("흉 고 직 경 : " + diameter + "cm" );
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                ma.tree_id = (ma.infoArray.size() == 0)? 0 : id;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ma.infoArray.get(ma.tree_id).setDiameter(Float.valueOf(diameter));
                ma.infoArray.get(ma.tree_id).getNode().setRenderable(ma.modelRenderable);

                //AR TextView
                ma.RenderText(seekBar.getProgress());


                ma.arFragment.getArSceneView().getScene().addOnUpdateListener(ma.arFragment);
            }
        });

        lr_rot = (SeekBar)root.findViewById(R.id.LR_Rotation);
        lr_rot.setMin(-90); lr_rot.setMax(90); lr_rot.setProgress(0);
        lr_rot.setOnSeekBarChangeListener(LRROT);


        fb_rot = (SeekBar)root.findViewById(R.id.FB_Rotation);
        fb_rot.setMin(-90); fb_rot.setMax(90); fb_rot.setProgress(0);
        fb_rot.setOnSeekBarChangeListener(FBROT);

        //AR
        ma.initModel();
        ma.initModel2();
        ma.initModel3();
        ma.initModel4();
        ma.arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
            mMySensorEventListener.updateOrientationAngles();
            if (ma.modelRenderable == null)
                return;

            // Creating Anchor.
            Anchor anchor2 = hitResult.createAnchor();
            AnchorNode anchorNode2 = new AnchorNode(anchor2);
            anchorNode2.setParent(ma.arFragment.getArSceneView().getScene());

            ma.radi = 100;
            ma.height=0;

            ma.initModel();
            ma.initModel2();
            ma.initModel3();
            ma.initModel4();

            // Create the transformable object and add it to the anchor.
            ma.anchor = anchor2;
            ma.anchorNode = anchorNode2;
            SimpleDateFormat dateformat = new SimpleDateFormat("dd_HHmmss");
            String idstr = dateformat.format(System.currentTimeMillis());
            Info tmp = new Info(new TransformableNode(ma.arFragment.getTransformationSystem()),
                    new TransformableNode(ma.arFragment.getTransformationSystem()),
                    new TransformableNode(ma.arFragment.getTransformationSystem()),
                    new TransformableNode(ma.arFragment.getTransformationSystem()), idstr);

            tmp.setDiameter(100);
            tmp.setHeight(0);
            tmp.getNode().setRenderable(ma.modelRenderable);
            tmp.getH_Node().setRenderable(ma.modelRenderable2);
            tmp.getT_Node().setRenderable(ma.modelRenderable3);
            tmp.getM_node().setRenderable(ma.modelRenderable4);

            /***************************************************/
            // T_Node를 Parent라고 놓고 (근간이라고 놓고)
            // 그에 해당하는 Child로 Node와 H_Node를 둠.
            // => 추후 이동할때 T_Node만 움직여도 나머지 노드들은 Parent따라서 움직임.
            /***************************************************/
            tmp.getT_Node().setParent(tmp.getM_node());
            tmp.getNode().setParent(tmp.getM_node());
            tmp.getH_Node().setParent(tmp.getM_node());
            tmp.getM_node().setParent(anchorNode2);

            tmp.getNode().setOnTouchListener(touchNode);
            tmp.getT_Node().setOnTouchListener(touchNode);
            tmp.getM_node().setOnTouchListener(touchNode);

            ma.arFragment.getArSceneView().getScene().addOnUpdateListener(ma.arFragment);
            ma.arFragment.getArSceneView().getScene().addChild(anchorNode2);

            //Get the Anchor distance to User and other value(Altitude, Compass. Diameter)
            if (ma.anchorNode != null) {
                Frame frame = ma.arFragment.getArSceneView().getArFrame();

                /*06.07(일) 테스트용으로 주석처리*/
                Pose objectPose = ma.anchor.getPose();
                //Pose cameraPose = frame.getCamera().getPose();
                //Get the Anchor Pose
                //ma.dx = objectPose.tx() - cameraPose.tx();
                //ma.dy = objectPose.ty() - cameraPose.ty();
                //ma.dz = objectPose.tz() - cameraPose.tz();

                /*06.07(일) 테스트용으로 대체 추가*/
                Vector3 ov = tmp.getT_Node().getWorldPosition();
                Pose cameraPose = frame.getCamera().getPose();
                ma.dx = ov.x - cameraPose.tx();
                ma.dy = ov.y - cameraPose.ty();
                ma.dz = ov.z - cameraPose.tz();
                ma.distanceMeters = (float) Math.sqrt(ma.dx * ma.dx + ma.dy * ma.dy + ma.dz * ma.dz);
                String meter = String.format("%.1f", ma.distanceMeters);
                ma.mDistance_tv.setText("거        리 : " + meter + "m");

                tmp.setDistance(ma.distanceMeters);
                //Get the altitude
                if (ma.altitude_vec.isEmpty()) {
                    ma.altitude_vec.add(altitude);
                    ma.mAltitude_tv.setText("고        도 :" +
                            Integer.toString((int) altitude) + "m");
                }

                //Get the compass
                if (ma.compass_vec.isEmpty()) {
                    compass = Math.abs(mMySensorEventListener.getYaw());
                    compass = (float) Math.toDegrees(compass);
                    ma.mCompass_tv.setText("방        위 : " + Integer.toString((int) compass) + "°"
                            + mMySensorEventListener.matchDirection(compass));
                }
            }


            //Get the Diameter
            ma.mDiameter_tv.setText("흉 고 직 경 : " +
                    Float.toString(((float) ma.radi / 10)*2) + "cm");

            ai.add(tmp);
            id = ai.size() - 1;
            ai.get(id).getNode().select();
            ma.tree_id = id;
            radiusbar.setProgress(100,true);
        });
        return root;
    }


    // Toast
    public void showToast(String data) {
        Toast.makeText(root.getContext(), data, Toast.LENGTH_SHORT).show();
    }

    //AR
    @Override
    public void onUpdate(FrameTime frameTime) {
    }

    TransformableNode.OnTouchListener touchNode = new TransformableNode.OnTouchListener(){
        @Override
        public boolean onTouch(HitTestResult hitTestResult, MotionEvent motionEvent) {
            
            if(hitTestResult.getNode()!=null) {
                id = (ai.size() == 0) ? 0 : ai.size() - 1;
                for (int i = 0; i < ai.size(); i++) {
                    if (hitTestResult.getNode().equals(ai.get(i).getNode()) ||
                            hitTestResult.getNode().equals(ai.get(i).getT_Node()) ||
                            hitTestResult.getNode().equals(ai.get(i).getM_node())) {
                        id = i;
                        if(hitTestResult.getNode().equals(ai.get(i).getNode())) {
                            ai.get(id).getNode().select();
                        }
                        else if (ai.get(id).getM_node().select()) {
                        }
                        else {
                            ai.get(id).getT_Node().select();
                        }
                        break;
                    }
                }

                showToast(Integer.toString(id + 1) + "번째 요소 선택("+ai.get(id).getId()+")");

                ma.tree_id=id;


                String meter = String.format("%.2f", ai.get(id).getDistance());
                ma.mDistance_tv.setText("거        리 : " + meter + "m");
                ma.mDiameter_tv.setText("흉 고 직 경 : " + Float.toString((ai.get(id).getDiameter() / 10)*2) + "cm");
                ma.mHeight_tv.setText("수      고 : " + Float.toString(1.2f+ai.get(id).getHeight()/100)+"m" ); //수정필요

            }
            return false;
        }
    };


    //Sensor
    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(mMySensorEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(mMySensorEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
    }
    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mMySensorEventListener);
        mLocationManager.removeUpdates(this);
    }



    //Image Button
    //control the object
    //Top
    ImageButton.OnClickListener controll_BtnTop = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View controllTop) {
            if (controllTop == mTop) {
                ma.initModel();
                ma.initModel2();
                ma.initModel3();
                    if (ma.infoArray.get(id).getNode().isSelected() || ma.infoArray.get(id).getM_node().isSelected()) {

                        Vector3 tmpVec3 = ma.infoArray.get(id).getM_node().getWorldPosition();
                        ma.infoArray.get(id).getM_node().setWorldPosition(new Vector3(tmpVec3.x, tmpVec3.y,
                                ((tmpVec3.z * 100)-1)/100));
                        ma.arFragment.getArSceneView().getScene().addOnUpdateListener(ma.arFragment);


                        Vector3 ov = ma.infoArray.get(id).getM_node().getWorldPosition();
                        Pose cameraPose = ma.arFragment.getArSceneView().getArFrame().getCamera().getPose();

                        ma.dx = ov.x - cameraPose.tx();
                        ma.dy = ov.y - cameraPose.ty();
                        ma.dz = ov.z - cameraPose.tz();
                        ma.distanceMeters = (float) Math.sqrt(ma.dx * ma.dx + ma.dy * ma.dy + ma.dz * ma.dz);
                        String meter = String.format("%.1f", ma.distanceMeters);
                        ma.mDistance_tv.setText("거        리 : " + meter + "m");
                        ma.infoArray.get(id).setDistance(ma.distanceMeters);



                    }

            }
        }
    };

    //Bottom
    ImageButton.OnClickListener controll_BtnBottom = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View controllBottom) {
            if (controllBottom == mBottom) {
                ma.initModel();
                ma.initModel2();
                ma.initModel3();
                    if (ma.infoArray.get(id).getNode().isSelected() ||ma.infoArray.get(id).getM_node().isSelected()) {

                        Vector3 tmpVec3 = ma.infoArray.get(id).getM_node().getWorldPosition();
                        ma.infoArray.get(id).getM_node().setWorldPosition(new Vector3(tmpVec3.x, tmpVec3.y,
                                ((tmpVec3.z * 100)+1)/100));

                        ma.arFragment.getArSceneView().getScene().addOnUpdateListener(ma.arFragment);
                        Vector3 ov = ma.infoArray.get(id).getM_node().getWorldPosition();
                        Pose cameraPose = ma.arFragment.getArSceneView().getArFrame().getCamera().getPose();

                        ma.dx = ov.x - cameraPose.tx();
                        ma.dy = ov.y - cameraPose.ty();
                        ma.dz = ov.z - cameraPose.tz();
                        ma.distanceMeters = (float) Math.sqrt(ma.dx * ma.dx + ma.dy * ma.dy + ma.dz * ma.dz);
                        String meter = String.format("%.1f", ma.distanceMeters);
                        ma.mDistance_tv.setText("거        리 : " + meter + "m");
                        ma.infoArray.get(id).setDistance(ma.distanceMeters);
                    }
                }
        }
    };

    //Right
    ImageButton.OnClickListener controll_BtnRight = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View controllRight) {
            if (controllRight == mRight) {
                ma.initModel();
                ma.initModel2();
                ma.initModel3();
                    if (ma.infoArray.get(id).getNode().isSelected()||ma.infoArray.get(id).getM_node().isSelected()) {


                        Vector3 tmpVec3 = ma.infoArray.get(id).getM_node().getWorldPosition();
                        ma.infoArray.get(id).getM_node().setWorldPosition(new Vector3(((tmpVec3.x*100)+1)/100,
                                tmpVec3.y, tmpVec3.z));
                        ma.arFragment.getArSceneView().getScene().addOnUpdateListener(ma.arFragment);
                        Vector3 ov = ma.infoArray.get(id).getM_node().getWorldPosition();
                        Pose cameraPose = ma.arFragment.getArSceneView().getArFrame().getCamera().getPose();

                        ma.dx = ov.x - cameraPose.tx();
                        ma.dy = ov.y - cameraPose.ty();
                        ma.dz = ov.z - cameraPose.tz();
                        ma.distanceMeters = (float) Math.sqrt(ma.dx * ma.dx + ma.dy * ma.dy + ma.dz * ma.dz);
                        String meter = String.format("%.1f", ma.distanceMeters);
                        ma.mDistance_tv.setText("거        리 : " + meter + "m");
                        ma.infoArray.get(id).setDistance(ma.distanceMeters);

                    }
            }
        }

    };

    //Left
    ImageButton.OnClickListener controll_BtnLeft = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View controllLeft) {
            if (controllLeft == mLeft) {
                ma.initModel();
                ma.initModel2();
                ma.initModel3();
                    if (ma.infoArray.get(id).getNode().isSelected()||ma.infoArray.get(id).getM_node().isSelected()) {


                        Vector3 tmpVec3 = ma.infoArray.get(id).getM_node().getWorldPosition();
                        ma.infoArray.get(id).getM_node().setWorldPosition(new Vector3(((tmpVec3.x*100)-1)/100,
                                tmpVec3.y, tmpVec3.z));
                        ma.arFragment.getArSceneView().getScene().addOnUpdateListener(ma.arFragment);
                        Vector3 ov = ma.infoArray.get(id).getM_node().getWorldPosition();
                        Pose cameraPose = ma.arFragment.getArSceneView().getArFrame().getCamera().getPose();
                        ma.dx = ov.x - cameraPose.tx();
                        ma.dy = ov.y - cameraPose.ty();
                        ma.dz = ov.z - cameraPose.tz();
                        ma.distanceMeters = (float) Math.sqrt(ma.dx * ma.dx + ma.dy * ma.dy + ma.dz * ma.dz);
                        String meter = String.format("%.1f", ma.distanceMeters);
                        ma.mDistance_tv.setText("거        리 : " + meter + "m");
                        ma.infoArray.get(id).setDistance(ma.distanceMeters);
                    }
                }
        }
    };

    /******************  회전부   *********************/

    /*

         seekbar를 조이스틱 스위치 방식처럼 구현.


     */


    SeekBar.OnSeekBarChangeListener LRROT = new SeekBar.OnSeekBarChangeListener() {
        int cur_rot;
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int degree = (progress-cur_rot >0)? 1:-1;
            ma.initModel();
            if(ma.infoArray.get(id).getNode().isSelected()||ma.infoArray.get(id).getT_Node().isSelected()){
                Quaternion rotation1 = ma.infoArray.get(id).getNode().getLocalRotation();
                Quaternion rotation2 = Quaternion.axisAngle(new Vector3(0.0f, 0f, 1.0f), degree);

                ma.infoArray.get(id).getNode().setLocalRotation(Quaternion.multiply(rotation1, rotation2));

                ma.arFragment.getArSceneView().getScene().addOnUpdateListener(ma.arFragment);
                ma.infoArray.get(id).getNode().select();
            }
            cur_rot=progress;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            cur_rot = seekBar.getProgress();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            seekBar.setProgress(0);
        }
    };
    SeekBar.OnSeekBarChangeListener FBROT = new SeekBar.OnSeekBarChangeListener() {
        int cur_rot;
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            int degree = (progress-cur_rot >0)? 1:-1;
            ma.initModel();
            if(ma.infoArray.get(id).getNode().isSelected()||ma.infoArray.get(id).getT_Node().isSelected()){
                Quaternion rotation1 = ma.infoArray.get(id).getNode().getLocalRotation();
                Quaternion rotation2 = Quaternion.axisAngle(new Vector3(1.0f, 0f, 0.0f),degree);

                ma.infoArray.get(id).getNode().setLocalRotation(Quaternion.multiply(rotation1, rotation2));

                ma.arFragment.getArSceneView().getScene().addOnUpdateListener(ma.arFragment);
                ma.infoArray.get(id).getNode().select();
            }
            cur_rot=progress;
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            cur_rot = seekBar.getProgress();
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            seekBar.setProgress(0);
        }
    };
    @Override
    public void onLocationChanged(Location location) {
        double altitude = location.getAltitude();

        ma.mAltitude_tv.setText("고        도 :"+Integer.toString((int)altitude)+"m");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}

