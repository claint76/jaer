/**
 * TestPose.java
 * Test class for basic pose estimation using Optical Gyro and VOR Sensor data
 * 
 * Created on November 14, 2012, 2:24 PM
 *
 * @author Haza
 */

package ch.unizh.ini.jaer.projects.poseestimation;

import com.phidgets.PhidgetException;
import com.phidgets.SpatialPhidget;
import com.phidgets.event.*;
import com.sun.opengl.util.GLUT;
import java.awt.geom.Point2D;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import net.sf.jaer.Description;
import net.sf.jaer.DevelopmentStatus;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.BasicEvent;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.event.OutputEventIterator;
import net.sf.jaer.eventprocessing.EventFilter2D;
import net.sf.jaer.graphics.FrameAnnotater;

@Description("Pose Estimation using OptigalGyro and VOR Sensor information")
@DevelopmentStatus(DevelopmentStatus.Status.Experimental)
public class TestPose extends EventFilter2D implements Observer, FrameAnnotater {
   
    // Controls
    protected int varControl = getPrefs().getInt("TestPose.varControl", 1);
    {
        setPropertyTooltip("varControl", "varControl description");
    }

    // VOR Handlers
    SpatialPhidget spatial = null;
    SpatialDataEvent spatialData = null;
    private int samplingDataRateVOR = 400; // Can't just be any number .. Not sure why
    
    // VOR Outputs
    private int ts; // Timestamp
    private double[] acceleration, gyro, compass = new double[3];

    // Drawing Points
    Point2D.Float ptVar = new Point2D.Float(); 

    /**
     * Constructor
     * @param chip Called with AEChip properties
     */
    public TestPose(AEChip chip) {
        super(chip);
        chip.addObserver(this);
        initFilter();
    
        // Create sensor device variable
        try {
            spatial = new SpatialPhidget();
        } catch (PhidgetException e) {
            log.log(Level.WARNING, "{0}: gyro will not be available", e.toString());
        }

        // Creates listener for when device is plugged in 
        spatial.addAttachListener(new AttachListener() {

            // Log device info and set device sampling rate
            @Override
            public void attached(AttachEvent ae) {
                log.log(Level.INFO, "attachment of {0}", ae);
                try {
                    ((SpatialPhidget) ae.getSource()).setDataRate(samplingDataRateVOR); 
                    StringBuilder sb = new StringBuilder();
                    sb.append("Serial: ").append(spatial.getSerialNumber()).append("\n");
                    sb.append("Accel Axes: ").append(spatial.getAccelerationAxisCount()).append("\n");
                    sb.append("Gyro Axes: ").append(spatial.getGyroAxisCount()).append("\n");
                    sb.append("Compass Axes: ").append(spatial.getCompassAxisCount()).append("\n");
                    sb.append("Data Rate: ").append(spatial.getDataRate()).append("\n");
                    log.info(sb.toString());
                } catch (PhidgetException pe) {
                    log.log(Level.WARNING, "Problem setting data rate: {0}", pe.toString());
                }
            }
        });
        
        // Creates listener for when device is unplugged
        spatial.addDetachListener(new DetachListener() {

            // Log detachment and reset filter
            @Override
            public void detached(DetachEvent ae) {
                log.log(Level.INFO, "detachment of {0}", ae);
                // do not close since then we will not get attachment events anymore
                resetFilter();
            }
        });
        
        // Creates listener for device errors
        spatial.addErrorListener(new ErrorListener() {

            // Log error
            @Override
            public void error(ErrorEvent ee) {
                log.warning(ee.toString());
            }
        });
        
        // Creates listener for incoming data 
        spatial.addSpatialDataListener(new SpatialDataListener() {

            // Write incoming data to variables
            @Override
            public void data(SpatialDataEvent sde) {
                if (sde.getData().length == 0) {
                    log.warning("empty data");
                    return;
                }
                acceleration = sde.getData()[0].getAcceleration();
                gyro = sde.getData()[0].getAngularRate();
                compass = sde.getData()[0].getMagneticField();
                ts = sde.getData()[0].getTimeMicroSeconds();
            }
        });

        // Open device anytime
        try {
            spatial.openAny(); // Starts thread to open any device that is plugged in (now or later)
        } catch (PhidgetException ex) {
            log.warning(ex.toString());
        }

    }
    
    /**
     * Called on creation
     */    
    @Override
    public void initFilter() {

    }

    /**
     * Called on filter reset
     */    
    @Override
    synchronized public void resetFilter() {
    
    }
    
    /**
     * Called on changes in the chip
     * @param o 
     * @param arg 
     */    
    @Override
    public void update(Observable o, Object arg) {
//        initFilter();
    }

    /**
     * Receives Packets of information and passes it onto processing
     * @param in Input events can be null or empty.
     * @return The filtered events to be rendered
     */
    @Override
    synchronized public EventPacket filterPacket(EventPacket in) {
        // Check for empty packet
        if(in.getSize() == 0) 
            return in;
        // Check that filtering is in fact enabled
        if(!filterEnabled) 
            return in;
        // If necessary, pre filter input packet 
        if(enclosedFilter!=null) 
            in=enclosedFilter.filterPacket(in);
        // Checks that output package has correct data type
        checkOutputPacketEventType(in);
        
        OutputEventIterator outItr = out.outputIterator();
        for (Object e : in) {
            BasicEvent i = (BasicEvent) e;
            BasicEvent o = (BasicEvent) outItr.nextOutput();
            o.copyFrom(i);
        }
        // Process events
//        process(in);
        return out;
    }

    /** 
     * Processes packet information
     * @param in Input event packet
     * @return Filtered event packet 
     */
    synchronized private void process(EventPacket ae) {
        // Event Iterator
//        for (Object e : ae) {
//            BasicEvent i = (BasicEvent) e; 
//        }
    }    
    
    /** 
     * Annotation or drawing method
     * @param drawable OpenGL Rendering Object
     */
    @Override
    synchronized public void annotate(GLAutoDrawable drawable) {
        if (!isAnnotationEnabled()) 
            return;
        GL gl = drawable.getGL();
        if (gl == null) 
            return;

        // Draw sensor information text
        final int font = GLUT.BITMAP_HELVETICA_18;
        gl.glColor3f(1, 1, 1);
        gl.glRasterPos3f(108, 123, 0);
        // Accelerometer x, y, z info
        chip.getCanvas().getGlut().glutBitmapString(font, String.format("a_x=%+.2f", acceleration[0]));
        gl.glRasterPos3f(108, 118, 0);
        chip.getCanvas().getGlut().glutBitmapString(font, String.format("a_y=%+.2f", acceleration[1]));
        gl.glRasterPos3f(108, 113, 0);
        chip.getCanvas().getGlut().glutBitmapString(font, String.format("a_z=%+.2f", acceleration[2]));
        // Gyroscope 
        gl.glRasterPos3f(108, 103, 0);
        chip.getCanvas().getGlut().glutBitmapString(font, String.format("g_x=%+.2f", gyro[0]));
        gl.glRasterPos3f(108, 98, 0);
        chip.getCanvas().getGlut().glutBitmapString(font, String.format("g_y=%+.2f", gyro[1]));
        gl.glRasterPos3f(108, 93, 0);
        chip.getCanvas().getGlut().glutBitmapString(font, String.format("g_z=%+.2f", gyro[2]));
        // Magnet
        gl.glRasterPos3f(108, 83, 0);
        chip.getCanvas().getGlut().glutBitmapString(font, String.format("c_x=%+.2f", compass[0]));
        gl.glRasterPos3f(108, 78, 0);
        chip.getCanvas().getGlut().glutBitmapString(font, String.format("c_y=%+.2f", compass[1]));
        gl.glRasterPos3f(108, 73, 0);
        chip.getCanvas().getGlut().glutBitmapString(font, String.format("c_z=%+.2f", compass[2]));

//        // Draw point 
//        gl.glPushMatrix();
//        gl.glPointSize(6f);
//        gl.glColor3f(1f,1f,1f);
//        gl.glBegin(GL.GL_POINTS);
//        gl.glVertex2f(56f, 56f);
//        gl.glEnd();
//        gl.glPopMatrix();
        
        // Draw line 
//        gl.glPushMatrix();
//        gl.glLineWidth(6f);
//        gl.glBegin(GL.GL_LINES);
//        gl.glColor3f(1f,1f,1f);
//        gl.glVertex2f(ptVar.x, ptVar.y);
//        gl.glVertex2f(ptVar.x * 2, ptVar.y * 2);
//        gl.glEnd();
//        gl.glPopMatrix();
        

    }

    /** 
     * Sample Method
     * @param var var
     * @return var
     */
    protected float sampleMethod(float var) {
        return var;
    }
    
    /**
     * Getter for varControl
     * @return varControl 
     */
    public int getVarControl() {
        return this.varControl;
    }

    /**
     * Setter for integration time window
     * @see #getVarControl
     * @param varControl varControl
     */
    public void setVarControl(final int varControl) {
        getPrefs().putInt("TestPose.varControl", varControl);
        getSupport().firePropertyChange("varControl", this.varControl, varControl);
        this.varControl = varControl;
    }

    public int getMinVarControl() {
        return 1;
    }

    public int getMaxVarControl() {
        return 10;
    }

}