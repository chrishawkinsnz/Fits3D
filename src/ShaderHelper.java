import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;


public class ShaderHelper {

    public static int programWithShaders2(GL3 gl, String vertexShaderPath, String fragmentShaderPath){
    	int shaderProgram = gl.glCreateProgram();
		int vertexShader = gl.glCreateShader(gl.GL_VERTEX_SHADER);
		int fragmentShader = gl.glCreateShader(gl.GL_FRAGMENT_SHADER);
		
		//--parse in that fucking shader using string parsing apparently.
		StringBuilder vertexShaderSource = new StringBuilder();
		StringBuilder fragmentShaderSource = new StringBuilder();
		
		//--vert first
		try {
			BufferedReader reader = new BufferedReader(new FileReader(vertexShaderPath));
			String line;
			while((line = reader.readLine()) != null) {
				vertexShaderSource.append(line).append("\n");
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("Somehting went wrton with the vertex shader reading");
			System.exit(1);
		}
		
	
		//then frag
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fragmentShaderPath));
			String line;
			while((line = reader.readLine()) != null) {
				fragmentShaderSource.append(line).append("\n");
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("Somehting went wrton with the frag shader reading");
			System.exit(1);
		}
		
        gl.glShaderSource(vertexShader, 1, new String[] { vertexShaderSource.toString() }, (int[]) null, 0);
		gl.glCompileShader(vertexShader);
		checkLogInfo(gl, vertexShader, "vertex shader");
		
        gl.glShaderSource(fragmentShader, 1, new String[] { fragmentShaderSource.toString() }, (int[]) null, 0);
		gl.glCompileShader(fragmentShader);
		checkLogInfo(gl, fragmentShader, "fragment shader");
		
		ByteBuffer bb = ByteBuffer.allocate(1000);
		
		
		gl.glAttachShader(shaderProgram, vertexShader);
		gl.glAttachShader(shaderProgram, fragmentShader);
		
		gl.glLinkProgram(shaderProgram);
		gl.glValidateProgram(shaderProgram);
		
		return shaderProgram;
    }
    
    private static void quit() {
		System.exit(1);	
    }
    
    private static void checkLogInfo(GL3 gl, int programObject, String shaderName) {
    	int[] compiled = new int[1];
        gl.glGetShaderiv(programObject, gl.GL_COMPILE_STATUS, compiled,0);
        if(compiled[0]==0){
            int[] logLength = new int[1];
            gl.glGetShaderiv(programObject, gl.GL_INFO_LOG_LENGTH, logLength, 0);

            byte[] log = new byte[logLength[0]];
            gl.glGetShaderInfoLog(programObject, logLength[0], (int[])null, 0, log, 0);

            System.err.println("Error compiling " + shaderName +": " + new String(log));
            System.exit(1);
        }
    }
}
