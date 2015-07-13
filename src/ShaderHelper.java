import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.jogamp.opengl.GL3;

import static com.jogamp.opengl.GL3.*;

public class ShaderHelper {

    public static int programWithShaders2(GL3 gl, String vertexShaderFileName, String fragmentShaderFileName){
    	int shaderProgram = gl.glCreateProgram();
		int vertexShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fragmentShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		
		//--parse in that fucking shader using string parsing apparently.
		StringBuilder vertexShaderSource = new StringBuilder();
		StringBuilder fragmentShaderSource = new StringBuilder();

		//--vert first
		try {
			InputStream is = ShaderHelper.class.getClass().getResourceAsStream("/shaders/" + vertexShaderFileName);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader reader = new BufferedReader(isr);

			String line;
			while((line = reader.readLine()) != null) {
				vertexShaderSource.append(line).append("\n");
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("Somehting went wrton with the vertex shader reading");
			e.printStackTrace();
			System.exit(1);
		}
		
		//then frag
		try {
			InputStream is = ShaderHelper.class.getClass().getResourceAsStream("/shaders/" + fragmentShaderFileName);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader reader = new BufferedReader(isr);


			String line;
			while((line = reader.readLine()) != null) {
				fragmentShaderSource.append(line).append("\n");
			}
			reader.close();
		} catch (IOException e) {
			System.err.println("Somehting went wrton with the frag shader reading");
			e.printStackTrace();
			System.exit(1);
		}
		
        gl.glShaderSource(vertexShader, 1, new String[] { vertexShaderSource.toString() }, (int[]) null, 0);
		gl.glCompileShader(vertexShader);
		checkLogInfo(gl, vertexShader, "vertex shader");
		
        gl.glShaderSource(fragmentShader, 1, new String[] { fragmentShaderSource.toString() }, (int[]) null, 0);
		gl.glCompileShader(fragmentShader);
		checkLogInfo(gl, fragmentShader, "fragment shader");
		
		gl.glAttachShader(shaderProgram, vertexShader);
		gl.glAttachShader(shaderProgram, fragmentShader);
		
		gl.glLinkProgram(shaderProgram);
		gl.glValidateProgram(shaderProgram);
		
		return shaderProgram;
    }
    
    private static void checkLogInfo(GL3 gl, int programObject, String shaderName) {
    	int[] compiled = new int[1];
        gl.glGetShaderiv(programObject, GL_COMPILE_STATUS, compiled,0);
        if(compiled[0]==0){
            int[] logLength = new int[1];
            gl.glGetShaderiv(programObject, GL_INFO_LOG_LENGTH, logLength, 0);

            byte[] log = new byte[logLength[0]];
            gl.glGetShaderInfoLog(programObject, logLength[0], (int[])null, 0, log, 0);

            System.err.println("Error compiling " + shaderName +": " + new String(log));
            System.exit(1);
        }
    }
}
