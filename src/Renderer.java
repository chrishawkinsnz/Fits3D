import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL2.*;

import com.jogamp.opengl.GL2;

public class Renderer {
	
	private PointCloud pointCloud;
	private GL2 gl;
	
	private int vertexVboid;
	private int colorVboid;
	private boolean spinning = false;
	
	private int shaderProgram;
	private int alphaFudgeUniformLocation;
	
	public float alphaFudge = 0.02f;

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
		
		this.shaderProgram = ShaderHelper.programWithShaders2(gl, "src/shaders/shader.vert", "src/shaders/shader.frag");
		this.alphaFudgeUniformLocation = gl.glGetUniformLocation(this.shaderProgram, "alphaFudge");
	}	
	
	private float theta = 0f;
	private int sumFrames;
	public void display() {
		gl.glUseProgram(this.shaderProgram);
		gl.glUniform1f(this.alphaFudgeUniformLocation, this.alphaFudge);

		gl.glLoadIdentity();
		if (this.spinning) {
			theta += 1f;
		}
		
		gl.glRotatef(theta, 0f, 1f, 0f);
//		gl.glRotatef(22.5f, 1f, 0f, 0f);
		
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		gl.glEnableClientState(GL_VERTEX_ARRAY);
		gl.glBindBuffer(GL_ARRAY_BUFFER, this.vertexVboid);
		gl.glVertexPointer(3,GL_FLOAT, 0, 0);
		
	    gl.glEnableClientState(GL_COLOR_ARRAY);
	    gl.glBindBuffer(GL_ARRAY_BUFFER, this.colorVboid);
	    gl.glColorPointer(4, GL_FLOAT, 0, 0);
		
	    gl.glDrawArrays(GL_POINTS, 0, this.pointCloud.validPts);
		gl.glUseProgram(0);
		
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
