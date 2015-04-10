import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.jogamp.opengl.GL2.*;


import com.jogamp.opengl.GL2;


public class Renderer {
	
	private PointCloud pointCloud;
	private GL2 gl;
	
	private int vertexVboid;
	private int colorVboid;
	private boolean spinning = false;
	

	public Renderer(PointCloud pointCloud, GL2 gl){
		this.pointCloud = pointCloud;
		this.gl = gl;
		System.out.println(gl);
		int[] bufferIds = new int[2];
		gl.glGenBuffers(2,bufferIds, 0);
		this.vertexVboid = bufferIds[0];//bufferIds[0];
		this.colorVboid = bufferIds[1];//bufferIds[1];
		this.vertexBufferData(this.vertexVboid, this.pointCloud.vertexBuffer);
		this.vertexBufferData(this.colorVboid, this.pointCloud.colorBuffer);
		
	}
	
	
	private float theta = 0f;
	public void display() {
//		gl.glLoadIdentity();
//    	gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
//    	gl.glColor3f(1.0f, 0.0f, 1.0f);
//    	gl.glBegin(GL_LINES);
//    		gl.glVertex3f(0f, 0f, 0.5f);
//    		gl.glVertex3f(1f, 1f,0.5f);
//    	gl.glEnd();
//    	gl.glFlush();
		gl.glLoadIdentity();
		if (this.spinning) {
			theta += 1f;
		}
		gl.glRotatef(theta, 0f, 1f, 0f);
		
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glEnableClientState(GL_VERTEX_ARRAY);
		gl.glBindBuffer(GL_ARRAY_BUFFER, this.vertexVboid);
		gl.glVertexPointer(3,GL_FLOAT, 0, 0);
		
	    gl.glEnableClientState(GL_COLOR_ARRAY);
	    gl.glBindBuffer(GL_ARRAY_BUFFER, this.colorVboid);
	    gl.glColorPointer(4, GL_FLOAT, 0, 0);
		
	    gl.glDrawArrays(GL_POINTS, 0, this.pointCloud.validPts);
//	    gl.glDrawArrays(GL_TRIANGLES, 0, this.pointCloud.validPts);
		gl.glFlush();
	}
	
	private void vertexBufferData(int id, FloatBuffer buffer) {
	    gl.glBindBuffer(GL_ARRAY_BUFFER, id); 
	    gl.glBufferData(GL_ARRAY_BUFFER, 4 * buffer.capacity(), buffer, GL_STATIC_DRAW);
	}



	public void toggleSpinning() {
		this.spinning = !this.spinning;
	}
}
