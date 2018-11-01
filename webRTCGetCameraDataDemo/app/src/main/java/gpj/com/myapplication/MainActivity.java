package gpj.com.myapplication;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CapturerObserver;
import org.webrtc.EglBase;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int CAMERA_OK = 10;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_OK:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //已经获取到摄像头权限开始获取相机数据
                    getCameraData();

                }else {
                    //用户拒绝给APP摄像头权限
                    Toast.makeText(MainActivity.this,"请手动打开相机权限",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
            //先判断有没有权限 ，没有就在这里进行权限的申请
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.CAMERA},CAMERA_OK);

        }else {
            //已经获取到摄像头权限开始获取相机数据
            getCameraData();
        }


    }

    private void getCameraData(){
        // 初始化PeerConnectionFactory的全局变量
        PeerConnectionFactory
                .initialize(PeerConnectionFactory
                        .InitializationOptions
                        .builder(this)
                        .setEnableInternalTracer(true)
                        .createInitializationOptions());


        // 创建一个PeerConnectionFactory实例
        //PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        PeerConnectionFactory peerConnectionFactory = PeerConnectionFactory
                .builder()
                .createPeerConnectionFactory();


        // 创建一个VideoCapturer实例，VideoCapturer 封装了 Android 的 Camera 的使用方法
        // 可以回调一些相机的回调方法。
        VideoCapturer videoCapturerAndroid = createVideoCapturer();

        //创建 MediaConstraints - 用来约束特定音频和视屏.
        MediaConstraints constraints = new MediaConstraints();

        //创建一个视频源（VideoSource）实例
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid.isScreencast());
        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        //创建一个音频源（AudioSource）实例
        AudioSource audioSource = peerConnectionFactory.createAudioSource(constraints);
        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);


        //创建SurfaceViewRenderer,并初始化
        SurfaceViewRenderer videoView = findViewById(R.id.surface_rendeer);
        videoView.setMirror(true);

        EglBase rootEglBase = EglBase.create();
        videoView.init(rootEglBase.getEglBaseContext(), null);


        SurfaceTextureHelper helper = SurfaceTextureHelper.create("gpj",rootEglBase.getEglBaseContext());
        videoCapturerAndroid.initialize(helper, this, new CapturerObserver() {
            @Override
            public void onCapturerStarted(boolean b) {
                Log.d(TAG,"onCapturerStarted:"+b);
            }

            @Override
            public void onCapturerStopped() {
                Log.d(TAG,"onCapturerStopped");
            }

            @Override
            public void onFrameCaptured(VideoFrame videoFrame) {
                videoView.onFrame(videoFrame);
            }
        });
        //开始从相机捕捉帧，参数分别为宽 ，高，fps
        videoCapturerAndroid.startCapture(1000, 1000, 30);

    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        Logging.d(TAG, "Creating capturer using camera1 API.");
        videoCapturer = createCameraCapturer(new Camera1Enumerator(false));

        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }
}