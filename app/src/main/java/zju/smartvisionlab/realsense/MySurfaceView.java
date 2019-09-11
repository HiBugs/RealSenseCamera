package zju.smartvisionlab.realsense;


import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class MySurfaceView implements Runnable, SurfaceHolder.Callback{
    private SurfaceHolder mHolder; // 用于控制SurfaceView
    private DrawThread thread; // 声明一条线程
    private volatile boolean flagThread; // 线程运行的标识，用于控制线程
    private Matrix matrix;
    private Canvas mCanvas; // 声明一张画布
    private Paint mpaint; // 声明一支画笔
    private Bitmap mBitmap;
    private Activity activity;

    public MySurfaceView(Activity activity) {
        this.activity = activity;
        SurfaceView surfaceView = activity.findViewById(R.id.screen_view);
        this.mHolder = surfaceView.getHolder(); // 获得SurfaceHolder对象
        this.mHolder.addCallback(this); // 为SurfaceView添加状态监听
        this.mCanvas = new Canvas();
        this.mpaint = new Paint(); // 创建一个画笔对象
        this.matrix = new Matrix();

    }
    public void setAspectRatio(float previewW, float previewH){
        Resources resources = this.activity.getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
//        float density = dm.density;
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        float ratioW = width/previewW;
        float ratioH = height/previewH;
        float ratio = ratioH<ratioW?ratioH:ratioW;
        matrix.setScale(ratio,ratio);

    }
    /**
     * 当SurfaceView创建的时候，调用此函数
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d("RealSense","created");
        thread = new DrawThread();
        thread.start();
    }

    /**
     * 当SurfaceView的视图发生改变的时候，调用此函数
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    /**
     * 当SurfaceView销毁的时候，调用此函数
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        flagThread = false;
        mHolder.removeCallback(this);
    }

    class DrawThread extends Thread {
        public void run() {
            while (flagThread) {
                try {
                    synchronized (mHolder) {
                        Thread.sleep(10);
                        draw();

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setBitmap(Bitmap bitmap){
        this.mBitmap = bitmap;
    }

    public void run(){
        flagThread = true;
    }

    public void draw(){
        mCanvas = mHolder.lockCanvas(); // 获得画布对象，开始对画布画画
        if (mCanvas != null) {
            Log.d("RealSense","draw");
            mCanvas.drawBitmap(mBitmap,matrix,mpaint);
            mHolder.unlockCanvasAndPost(mCanvas); // 完成画画，把画布显示在屏幕上
        }
    }
}

