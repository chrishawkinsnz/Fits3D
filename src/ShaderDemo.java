import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL2;


public class ShaderDemo {

    public static int programWithShaders2(GL2 gl, String vertexShaderPath, String fragmentShaderPath){
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
//		gl.glShaderSource(vertexShader,vertexShaderSource);
		gl.glCompileShader(vertexShader);

		
		
        gl.glShaderSource(fragmentShader, 1, new String[] { fragmentShaderSource.toString() }, (int[]) null, 0);
		gl.glCompileShader(fragmentShader);
		
		ByteBuffer bb = ByteBuffer.allocate(1000);
		checkLogInfo(gl, shaderProgram);
		
		
		gl.glAttachShader(shaderProgram, vertexShader);
		gl.glAttachShader(shaderProgram, fragmentShader);
		
		gl.glLinkProgram(shaderProgram);
		gl.glValidateProgram(shaderProgram);
		
		return shaderProgram;
    }
    
//    private static int programWithShaders(GL2 gl, String vertexShaderPath, String fragmentShaderPath) {
//    	int shaderProgram = gl.glCreateProgram();
//		int vertexShader = gl.glCreateShader(gl.GL_VERTEX_SHADER);
//		int fragmentShader = gl.glCreateShader(gl.GL_FRAGMENT_SHADER);
//		
//		ShaderDemo.loadShader(vertexShader, vertexShaderPath);
//		ShaderDemo.loadShader(fragmentShader, fragmentShaderPath);
//		
//		gl.glAttachShader(shaderProgram, vertexShader);
//		gl.glAttachShader(shaderProgram, fragmentShader);
//		
//		gl.glLinkProgram(shaderProgram);
//		gl.glValidateProgram(shaderProgram);
//		
//		return 0;
//    }
//    
//    private static void loadShader(GL2 gl, int shader, String fileName) {
//		//--read in the string
//    	StringBuilder shaderSource = new StringBuilder();
//    	try {
//			BufferedReader reader = new BufferedReader(new FileReader(fileName));
//			String line;
//			while((line = reader.readLine()) != null) {
//				shaderSource.append(line).append("\n");
//			}
//			reader.close();
//		} catch (IOException e) {
//			System.err.println("Somehting went wrong reading file at  '" + fileName + "'");
//			quit();
//		}
//    	
//    	//--compile shader source
//        gl.glShaderSource(shader, 1, new String[] { shaderSource.toString() }, (int[]) null, 0);
//		gl.glCompileShader(shader);		
//    }
    
    
    private static void quit() {
		System.exit(1);	
    }
    
    private static void checkLogInfo(GL2 gl, int programObject) {
        IntBuffer intValue = IntBuffer.allocate(1);
        gl.glGetObjectParameterivARB(programObject, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB, intValue);

        int lengthWithNull = intValue.get();

        if (lengthWithNull <= 1) {
            return;
        }

        ByteBuffer infoLog = ByteBuffer.allocate(lengthWithNull);

        intValue.flip();
        gl.glGetInfoLogARB(programObject, lengthWithNull, intValue, infoLog);

        int actualLength = intValue.get();

        byte[] infoBytes = new byte[actualLength];
        infoLog.get(infoBytes);
        System.out.println("GLSL Validation >> " + new String(infoBytes));
    }
}
