package zju.smartvisionlab.realsense;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.DepthFrame;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.Extension;
import com.intel.realsense.librealsense.Frame;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.VideoFrame;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;

import static zju.smartvisionlab.realsense.ImageUtils.rgb2Bitmap;
import static zju.smartvisionlab.realsense.ImageUtils.saveBitmap;

public class MainActivity extends AppCompatActivity {
    private static final Size RealSenseSize = new Size(960,540);
    private static final int PERMISSIONS_REQUEST = 0;
    private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    private static final String PERMISSION_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private RsContext mRsContext;
    private Bitmap realsenseBM;
    private MySurfaceView mySurfaceView;
    private String TAG = "RealSense";

    @Override
    public void onRequestPermissionsResult(
            final int requestCode, final String[] permissions, final int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            } else {
                requestPermission();
            }
        }
    }

    private boolean hasPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(PERMISSION_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA) ||
                    shouldShowRequestPermissionRationale(PERMISSION_STORAGE)) {
                Toast.makeText(this,
                        "Camera AND storage permission are required for this demo", Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[]{PERMISSION_CAMERA, PERMISSION_STORAGE}, PERMISSIONS_REQUEST);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!mStreamingThread.isAlive())
            mStreamingThread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mStreamingThread.interrupt();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mySurfaceView = new MySurfaceView(this);
        mySurfaceView.setAspectRatio(RealSenseSize.getWidth(),RealSenseSize.getHeight());
        realsenseBM = BitmapFactory.decodeResource(this.getResources(), R.drawable.test).copy(Bitmap.Config.RGB_565, true);
//        realsenseBM = Bitmap.createBitmap(RealSenseSize.getWidth(),RealSenseSize.getHeight(),Bitmap.Config.ARGB_8888);
        mySurfaceView.setBitmap(realsenseBM);

        if (!hasPermission()) {
            requestPermission();
        }

        mySurfaceView.run();
        //RsContext.init must be called once in the application's lifetime before any interaction with physical RealSense devices.
        //For multi activities applications use the application context instead of the activity context
        RsContext.init(getApplicationContext());

        //Register to notifications regarding RealSense devices attach/detach events via the DeviceListener.
        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(new DeviceListener() {
            @Override
            public void onDeviceAttach() {
                mStreamingThread.start();
            }

            @Override
            public void onDeviceDetach() {
                mStreamingThread.interrupt();
            }
        });

    }

    private Thread mStreamingThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                stream();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    //Start streaming and print the distance of the center pixel in the depth frame.
    private void stream() throws Exception {
        // 图像传感器设置，需要用到的数据大小，格式，以及rate
        // 具体使用方式查看config里的函数，里面详细描述了使用方法
        Config config  = new Config();
//        config.enableStream(StreamType.DEPTH, RealSenseSize.getWidth(),RealSenseSize.getHeight());
        config.enableStream(StreamType.COLOR, RealSenseSize.getWidth(),RealSenseSize.getHeight());

        // 启动pipe，并用config信息初始化
        Pipeline pipe = new Pipeline();
        pipe.start(config);

        final DecimalFormat df = new DecimalFormat("#.##");

        // 当线程还在active的时候，进行收集
        while (!mStreamingThread.isInterrupted())
        {
            // 对于video frame来说，帧率设置的相同，那么一个frameset里面会包含所有stream的frame
            // 像这里就有color和depth，下面是对两个frame的操作举例
            // 所有可以使用的函数操作可以看Frame类和VideoFrame类

            try (FrameSet frames = pipe.waitForFrames()) {
                // color操作
                try (Frame f = frames.first(StreamType.COLOR)){
                    VideoFrame color = f.as(Extension.VIDEO_FRAME);

                    // 对videoframe的一些函数操作
                    int c_size= color.getDataSize();
                    int c_height = color.getHeight();
                    int c_width = color.getWidth();

                    Log.d(TAG, "color frame width size: "+ String.valueOf(c_width)+" height size : "+String.valueOf(c_height)+"\n");
                    Log.d(TAG,"data size (byte) : "+String.valueOf(c_size)+"\n");

                    // 尝试将realsense的byte数组转化为bitmap
                    byte[] c_data = new  byte[c_size];
                    color.getData(c_data);
                    final int len = c_data.length;
                    if(c_data.length !=0) {

                        realsenseBM = rgb2Bitmap(c_data,RealSenseSize.getWidth(),RealSenseSize.getHeight());
//                        saveBitmap(realsenseBM,"realsense.png");
                        mySurfaceView.setBitmap(realsenseBM);

                        Log.d(TAG, "onCaptureData: " + c_data.length);
                        Log.d(TAG,"transform byte to bitmap successfully\n");

                    }else{
                        Log.d(TAG,"fail to load color data\n");
                    }

                }

//                //  深度图操作
//                try (Frame f = frames.first(StreamType.DEPTH))
//                {
//                    DepthFrame depth = f.as(Extension.DEPTH_FRAME);
//                    final float deptValue = depth.getDistance(depth.getWidth()/2, depth.getHeight()/2);
//
//                }
            }
        }

        // 关闭realsense
        pipe.stop();
    }
}
