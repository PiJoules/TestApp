package com.test;

// these are all the classes from other packages necessary for plotting the
// accelerometer data
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYStepMode;
import java.util.ArrayList;

// the TimeSeriesActivity is an Activity that uses the SensorEventListener for
// getting accelerometer values
public class TimeSeriesActivity extends Activity implements SensorEventListener{

    private XYPlot plot; // the plot on which the graphs will be placed
    private Button done, start_pause, reset, show_hide; // done, start, pause, reset, show, and hide buttons
    private TextView count; // the view on the screen that displays the threshold counts for each vector
    
    // the initial arraylists holding the values for each vector
    // and one for the constant threshold value
    private ArrayList<Number> XAccel, YAccel, ZAccel, 
                              XAccelUnfiltered, YAccelUnfiltered, ZAccelUnfiltered,
                              threshold;
    
    // the series that will be plotted on the plot
    private SimpleXYSeries XAccelSeries, YAccelSeries, ZAccelSeries,
            XAccelUnfilteredSeries, YAccelUnfilteredSeries, ZAccelUnfilteredSeries,
            thresholdSeries;
            
    private final int ARRAYSIZE = 100; // number of data points per series that will be displayed on the plot
    private int sensorCount = 1; // the number of data points retrieved form accelerometer
    private Accelerometer accel; // the accelerometer object from which we received accelerometer values
    private boolean paused = false; // whether the plot is paused or not
    private final ArrayList<Number> xRange = new ArrayList<Number>(); // range of x vals to plot against
    
    // formatters for each series that decorate the lines
    // hide is the transparent decoration for hiding the threshold if necessary
    private LineAndPointFormatter XAccelFormat, YAccelFormat, ZAccelFormat,
                                  XAccelUnfilteredFormat, YAccelUnfilteredFormat, ZAccelUnfilteredFormat,
                                  thresholdFormat, hide;
    
    // accelerometer file manager for handling the text file that stores alpha and sma values
    private AccelerometerFileManager afm;
    private boolean showThreshold = true; // whether or not the threshold graph is displayed
    private int[] counts = new int[3]; // threshold count for each vector
    private boolean[] highFlags = {false, false, false}; // flags for each vector to determine if they are above the threshold or not

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        
        // hide the title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // hide the notification bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // lock the view to a vertical portrait orientation
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        
        // set the view the layout of the time_series_example.xml file
        setContentView(R.layout.time_series_example);
        
        // initialize the accelerometer
        accel = new Accelerometer(this,this, false, false);
        
        // initiliaze the accelerometer file manager
        afm = new AccelerometerFileManager(this, "");
        
        // initialize the dynamic arraylists
        XAccel = new ArrayList<Number>();
        YAccel = new ArrayList<Number>();
        ZAccel = new ArrayList<Number>();
        XAccelUnfiltered = new ArrayList<Number>();
        YAccelUnfiltered = new ArrayList<Number>();
        ZAccelUnfiltered = new ArrayList<Number>();

        threshold = new ArrayList<Number>();
        
        // fill up the arraylists with initial values
        for (int i = 0; i < ARRAYSIZE; i++){
            XAccel.add(0);
            YAccel.add(0);
            ZAccel.add(0);
            XAccelUnfiltered.add(0);
            YAccelUnfiltered.add(0);
            ZAccelUnfiltered.add(0);
            
            // the fourth element in the accelerometer file indicates
            // the threshold
            threshold.add(afm.getAccelData()[3]/9.81f);
            xRange.add(sensorCount++);
        }
        
        // initialize the done button
        // once clicked, it will close the plot and return to the previous screen
        done = (Button) findViewById(R.id.done);
        done.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        
        // initilaize the start_pause button
        // once clicked, it either pauses the plot or starts it
        start_pause = (Button) findViewById(R.id.start_pause);
        start_pause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (paused){
                    paused = false;
                    start_pause.setText("Pause");
                }
                else {
                    paused = true;
                    start_pause.setText("Resume");
                }
            }
        });
        
        // initilaize the reset button
        // once clicked, it resets the threshold counts for each of the vectors
        reset = (Button) findViewById(R.id.reset);
        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                counts[0] = 0;
                counts[1] = 0;
                counts[2] = 0;
            }
        });
        
        // initialize the show_hide button
        // once clicked, it either shows or hides the threshild line on the plot
        show_hide = (Button) findViewById(R.id.show_hide);
        show_hide.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (showThreshold){
                    show_hide.setText("Show");
                    showThreshold = false;
                }
                else {
                    show_hide.setText("Hide");
                    showThreshold = true;
                }
            }
        });
        
        // initilaize the textview that shows the counts on the graph
        count = (TextView) findViewById(R.id.count);
        
        // setup and decorate the plot
        plot = (XYPlot) findViewById(R.id.plot1);
        plot.getGraphWidget().setSize(new SizeMetrics(0.9f, SizeLayoutType.RELATIVE, 1f, SizeLayoutType.RELATIVE)); // asjust size of plot
        plot.getGraphWidget().setAnchor(AnchorPosition.RIGHT_MIDDLE); // center the plot vertically
        plot.setRangeUpperBoundary(3, BoundaryMode.FIXED); // set the upper limit for the y axiz
        plot.setRangeLowerBoundary(-3, BoundaryMode.FIXED); // set the lower limit for the y axis
        plot.setRangeStep(XYStepMode.INCREMENT_BY_VAL, .25); // set the range between each tick on the y axis
        plot.getLegendWidget().setSize(new SizeMetrics(30, SizeLayoutType.ABSOLUTE, 450, SizeLayoutType.ABSOLUTE)); // resize the legend
        plot.getLegendWidget().position(10, XLayoutStyle.ABSOLUTE_FROM_RIGHT, 50, YLayoutStyle.ABSOLUTE_FROM_BOTTOM); // add margins to the legend
        plot.getLegendWidget().setAnchor(AnchorPosition.RIGHT_BOTTOM); // align the legend to the bottom right corner of the screen
        plot.setRangeLabel("Acceleration (g)"); // set the y axis label
        plot.setDomainLabel("Data Point Number"); // set the x axis label
        
        // check what type of filter should be used
        if (afm.getFilterType() == AccelerometerFileManager.NONE){
            // Plot the raw data from the accelerometer if no filter
            plot.setTitle("Raw Accelerometer Data");
            accel.setFilter(AccelerometerFileManager.NONE);
        }
        else if (afm.getFilterType() == AccelerometerFileManager.EMA){
            // Plot the filtered data form the accelerometer using the alpha filter
            plot.setTitle("Alpha Filter Ouput with Alpha Value (" + afm.getAccelData()[1] + ")");
            accel.setFilter(AccelerometerFileManager.EMA);
            accel.setAlpha(afm.getAccelData()[1]);
        }
        else if (afm.getFilterType() == AccelerometerFileManager.SMA){
            // Plot the fltered data from the accelerometer using the sma filter
            plot.setTitle("SMA Filter Ouput with Period Count of (" + afm.getAccelData()[2] + ")");
            accel.setFilter(AccelerometerFileManager.SMA);
            accel.setPeriods((int) afm.getAccelData()[2]);
        }
        
        // create XYSeries from xRange and the individual vectors and assign their titles
        XAccelSeries = new SimpleXYSeries(xRange, XAccel, "Filtered X");
        YAccelSeries = new SimpleXYSeries(xRange, YAccel, "Filtered Y");
        ZAccelSeries = new SimpleXYSeries(xRange, ZAccel, "Filtered Z");
        XAccelUnfilteredSeries = new SimpleXYSeries(xRange, XAccelUnfiltered, "Raw X");
        YAccelUnfilteredSeries = new SimpleXYSeries(xRange, YAccelUnfiltered, "Raw Y");
        ZAccelUnfilteredSeries = new SimpleXYSeries(xRange, ZAccelUnfiltered, "Raw Z");     
        thresholdSeries = new SimpleXYSeries(xRange, threshold, "Threshold");

        // color the XAccelSeries red
        XAccelFormat = new LineAndPointFormatter(
                Color.RED,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                null
        );
        
        // color the YAccelSeries green
        YAccelFormat = new LineAndPointFormatter(
                Color.GREEN,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                null
        );
        
        // color the XAccelSeries blue
        ZAccelFormat = new LineAndPointFormatter(
                Color.BLUE,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                null
        );
        // color the Raw XAccelSeries
        XAccelUnfilteredFormat = new LineAndPointFormatter(
                Color.RED,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                null
        );
        Paint redDashPaint = new Paint();
        redDashPaint.setColor(Color.RED);
        redDashPaint.setStyle(Paint.Style.STROKE);
        redDashPaint.setStrokeWidth(3);
        redDashPaint.setPathEffect(new DashPathEffect(new float[] {10,5}, 0));
        XAccelUnfilteredFormat.setLinePaint(redDashPaint);
        
        // color the Raw YAccelSeries green
        YAccelUnfilteredFormat = new LineAndPointFormatter(
                Color.GREEN,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                null
        );
        Paint greenDashPaint = new Paint();
        greenDashPaint.setColor(Color.GREEN);
        greenDashPaint.setStyle(Paint.Style.STROKE);
        greenDashPaint.setStrokeWidth(3);
        greenDashPaint.setPathEffect(new DashPathEffect(new float[] {10,5}, 0));
        YAccelUnfilteredFormat.setLinePaint(greenDashPaint);
        
        // color the Raw ZAccelSeries blue
        ZAccelUnfilteredFormat = new LineAndPointFormatter(
                Color.BLUE,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                null
        );
        Paint blueDashPaint = new Paint();
        blueDashPaint.setColor(Color.BLUE);
        blueDashPaint.setStyle(Paint.Style.STROKE);
        blueDashPaint.setStrokeWidth(3);
        blueDashPaint.setPathEffect(new DashPathEffect(new float[] {10,5}, 0));
        ZAccelUnfilteredFormat.setLinePaint(blueDashPaint);
        
        // color the thresholdSeries Cyan
        thresholdFormat = new LineAndPointFormatter(
                Color.CYAN,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                null
        );
        
        // make the threshold series thransparent if the show_hide button is pressed
        hide = new LineAndPointFormatter(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                null
        );
        
    }
    
    // method for graphing the series on the xyplot
    private void graph(){
        plot.clear(); // clear the plot

        // add a new series' to the xyplot using the appropriate formats
        plot.addSeries(XAccelSeries, XAccelFormat);
        plot.addSeries(YAccelSeries, YAccelFormat);
        plot.addSeries(ZAccelSeries, ZAccelFormat);
        plot.addSeries(XAccelUnfilteredSeries, XAccelUnfilteredFormat);
        plot.addSeries(YAccelUnfilteredSeries, YAccelUnfilteredFormat);
        plot.addSeries(ZAccelUnfilteredSeries, ZAccelUnfilteredFormat);
        
        // show the threshold series only if showThreshold is true
        if (showThreshold) plot.addSeries(thresholdSeries, thresholdFormat);
        else plot.addSeries(thresholdSeries, hide);

        plot.redraw(); // redraw the new data
    }

    // method called once the accelerometer values change
    public void onSensorChanged(SensorEvent event) {
        accel.updateAccelVals(event); // update the accelerometer vals in the accel object
        
        // only perform the following if not paused
        if (!paused){
            
            // add the next filtered or unfiltered accelrometer readings
            // to the end of each of the respective series and remove the 
            // first values of each series to simulate a rolling plot
            XAccelSeries.addLast(sensorCount, accel.getAccel()[0]/9.81f);
            YAccelSeries.addLast(sensorCount, accel.getAccel()[1]/9.81f);
            ZAccelSeries.addLast(sensorCount, accel.getAccel()[2]/9.81f);
            XAccelUnfilteredSeries.addLast(sensorCount, accel.getAccelUnfiltered()[0]/9.81f);
            YAccelUnfilteredSeries.addLast(sensorCount, accel.getAccelUnfiltered()[1]/9.81f);
            ZAccelUnfilteredSeries.addLast(sensorCount, accel.getAccelUnfiltered()[2]/9.81f);
            thresholdSeries.addLast(sensorCount, afm.getAccelData()[3]/9.81f);
            XAccelSeries.removeFirst();
            YAccelSeries.removeFirst();
            ZAccelSeries.removeFirst();
            XAccelUnfilteredSeries.removeFirst();
            YAccelUnfilteredSeries.removeFirst();
            ZAccelUnfilteredSeries.removeFirst();
            thresholdSeries.removeFirst();
            
            // only check if each vector crossed the threshold if showthreshold is true
            if (showThreshold){
                if (accel.getAccel()[0] > afm.getAccelData()[3] && !highFlags[0]){
                    counts[0]++;
                    highFlags[0] = true;
                }
                else if (accel.getAccel()[0] <= afm.getAccelData()[3] && highFlags[0]){
                    highFlags[0] = false;
                }
                if (accel.getAccel()[1] > afm.getAccelData()[3] && !highFlags[1]){
                    counts[1]++;
                    highFlags[1] = true;
                }
                else if (accel.getAccel()[1] <= afm.getAccelData()[3] && highFlags[1]){
                    highFlags[1] = false;
                }
                if (accel.getAccel()[2] > afm.getAccelData()[3] && !highFlags[2]){
                    counts[2]++;
                    highFlags[2] = true;
                }
                else if (accel.getAccel()[2] <= afm.getAccelData()[3] && highFlags[2]){
                    highFlags[2] = false;
                }
            }
            
            // displayt he values of each of the threshold counts
            count.setText("x:" + counts[0] + " y:" + counts[1] + " z:" + counts[2]);
            
            // get another data point
            sensorCount++;
            graph(); // graph the new data set
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        
    }
}