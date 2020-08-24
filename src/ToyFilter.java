import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import net.sf.jaer.chip.AEChip;
import net.sf.jaer.event.BasicEvent;
import net.sf.jaer.event.EventPacket;
import net.sf.jaer.eventprocessing.EventFilter2D;
import net.sf.jaer.graphics.FrameAnnotater;
import org.opencv.aruco.Aruco;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static org.bytedeco.javacpp.opencv_core.CV_8UC1;

// TODO: Implement buffering of event frame data
// TODO: Parse event data into ArUco functions and see what the heck happens
// TODO: Simple marker annotation at centroid

// Buffer "memory" will be implemented through pixel decay

public class ToyFilter extends EventFilter2D implements FrameAnnotater {
    // TODO: How do you obtain these dynamically for different devices?
    // Currently set to the DAVIS-346 dimensions
    private int CHIP_WIDTH = 346;
    private int CHIP_HEIGHT = 260;
    private Aruco usage_class = new Aruco();

    private float bufferCounter = 0;
    private int bufferCycleLength = 20; // for specifying whole frame retention
    private float bufferTimeLength = getFloat("bufferTimeLength",1000f); // in microseconds
    private float maxBufferTimeLength = 1000000f; // one second
    private float minBufferTimeLength = 10f; // What should this be?

    private int kDim = 4;
    private int ptSize = 2;

    final private Mat buffer = new Mat(CHIP_WIDTH, CHIP_HEIGHT, CV_8UC1, new Scalar(0));

    public int getBufferCycleLength() {
        return bufferCycleLength;
    }
    public void setBufferCycleLength(int bufferCycleLength) {
        this.bufferCycleLength = bufferCycleLength;
    }
    public float getMinBufferTimeLength() {
        return minBufferTimeLength;
    }
    public float getMaxBufferTimeLength() {
        return maxBufferTimeLength;
    }
    public float getBufferTimeLength() {
        return bufferTimeLength;
    }
    public void setBufferTimeLength(float bufferTimeLength) {
        this.bufferTimeLength = bufferTimeLength;
    }
    public int getPtSize() {
        return ptSize;
    }
    public void setPtSize(int ptSize) {
        this.ptSize = ptSize;
    }
    public int getkDim() {
        return kDim;
    }
    public void setkDim(int kDim) {
        this.kDim = kDim;
    }

        /**
     * Subclasses should call this super initializer
     *
     * @param chip
     */
    public ToyFilter(AEChip chip) {
        super(chip);
    }

    @Override
    public EventPacket<?> filterPacket(EventPacket<?> in) {
        buffer.setTo(new Scalar(0));

        if (false && bufferCounter > bufferCycleLength) {
            buffer.setTo(new Scalar(0));
            bufferCounter = 0;
        }

        for (BasicEvent ev: in) {
            buffer.put(ev.getX(), ev.getY(), new byte[]{(byte) 255});
        }

        // bufferCounter++;

        return in;
    }

    @Override
    public void resetFilter() {

    }

    @Override
    public void initFilter() {
    }

    @Override
    public void annotate(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glPushMatrix();

        gl.glPointSize(ptSize);
        gl.glBegin(GL2.GL_POINTS);

        // Mat out = buffer.clone();
        Mat out = Mat.zeros(buffer.size(), CV_8UC1);
        Imgproc.blur(buffer, out, new Size(kDim,kDim));

        for (int i = 0; i < CHIP_WIDTH; i++) {
            for (int j = 0; j < CHIP_HEIGHT; j++) {
                byte[] temp = new byte[1];
                out.get(i, j, temp);

                if (temp[0] > 0) {
                    gl.glColor3f(0, 1f - temp[0] / 255f,0);
                    gl.glVertex2d(i, j);
                }
            }
        }


        gl.glEnd();

        gl.glPopMatrix();

    }
}
