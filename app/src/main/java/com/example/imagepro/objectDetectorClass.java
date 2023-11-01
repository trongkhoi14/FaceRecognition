package com.example.imagepro;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.checkerframework.checker.units.qual.A;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.LocalDate;

public class objectDetectorClass {
    // should start from small letter

    // this is used to load model and predict
    private Interpreter interpreter;
    // store all label in array
    private List<String> labelList;
    private int INPUT_SIZE;
    private int PIXEL_SIZE=3; // for RGB
    private int IMAGE_MEAN=0;
    private  float IMAGE_STD=255.0f;
    // use to initialize gpu in app
    private GpuDelegate gpuDelegate;
    private int height=0;
    private  int width=0;

    private Context context;

    // Constructor nhận một đối tượng Context
    public objectDetectorClass(Context context) {
        this.context = context;
    }
    objectDetectorClass(AssetManager assetManager,String modelPath, String labelPath,int inputSize) throws IOException{
        INPUT_SIZE=inputSize;
        // use to define gpu or cpu // no. of threads
        Interpreter.Options options=new Interpreter.Options();
        gpuDelegate=new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4); // set it according to your phone
        // loading model
        interpreter=new Interpreter(loadModelFile(assetManager,modelPath),options);
        // load labelmap
        labelList=loadLabelList(assetManager,labelPath);


    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        // to store label
        List<String> labelList=new ArrayList<>();
        // create a new reader
        BufferedReader reader=new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        // loop through each line and store it to labelList
        while ((line=reader.readLine())!=null){
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private ByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        // use to get description of file
        AssetFileDescriptor fileDescriptor=assetManager.openFd(modelPath);
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startOffset =fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public Mat recognizeImage(Mat mat_image, Context context) {
        // Rotate original image by 90 degrees to get portrait frame
        Mat rotated_mat_image = new Mat();
        Mat a = mat_image.t();
        Core.flip(a, rotated_mat_image, 1);
        // Release mat
        a.release();

        // Load and use the classifier
        CascadeClassifier faceClassifier = new CascadeClassifier();


        File cascadeFile = new File(context.getDir("cascade", Context.MODE_PRIVATE), "haarcascade_frontalface_default.xml");

        if (!cascadeFile.exists()) {
            // Tệp XML không tồn tại, tải nó từ res/raw và sao chép vào thư mục gốc
            try {
                InputStream inputStream = context.getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                FileOutputStream outputStream = new FileOutputStream(cascadeFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Detect faces
        MatOfRect faces = new MatOfRect();
        faceClassifier.load(cascadeFile.getAbsolutePath());
        faceClassifier.detectMultiScale(rotated_mat_image, faces);
        for (org.opencv.core.Rect rect : faces.toArray()) {

            // Convert it to bitmap
            Bitmap bitmap = null;
            // Cắt khuôn mặt từ ma trận ban đầu
            Mat croppedFace = new Mat(rotated_mat_image, new Rect(rect.x, rect.y, rect.width,  rect.height));

            bitmap = Bitmap.createBitmap(croppedFace.cols(), croppedFace.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(croppedFace, bitmap);

            // Define height and width
            int height = bitmap.getHeight();
            int width = bitmap.getWidth();

            // Scale the bitmap to the input size of the model
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

            // Convert bitmap to ByteBuffer as model input should be in it
            ByteBuffer byteBuffer = convertBitmapToByteBuffer(scaledBitmap);

            // Define output
            float[][] output = new float[1][3]; // Assuming your model has 3 output classes

            // Run inference
            interpreter.run(byteBuffer, output);
            for (int i = 0; i < output[0].length; i++) {
                System.out.println("Output[" + i + "] = " + output[0][i]);
            }
            int maxIndex = 0;
            float maxValue = output[0][0];
            for (int i = 1; i < 3; i++) {
                if (output[0][i] > maxValue) {
                    maxValue = output[0][i];
                    maxIndex = i;
                }
            }

            int labelX = rect.x; // X-coordinate of label
            int labelY = rect.y; // Y-coordinate of label (adjust spacing)

            // Create a Scalar color for the text (e.g., white)
            Scalar textColor = new Scalar(255, 255, 255);

            Imgproc.rectangle(rotated_mat_image,new Point(rect.x, rect.y),new Point(rect.x+rect.width,rect.y+ rect.height),new Scalar(0, 255, 0, 255),2);
            // Draw the label on the image

            Imgproc.putText(rotated_mat_image,labelList.get(maxIndex),new Point(rect.x, rect.y),3,0.6,new Scalar(255, 0, 0, 255),2);
            croppedFace.release();

            MyDatabaseHelper myDB = new MyDatabaseHelper(context);

            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dateStr = currentDate.format(dateFormatter);

            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String timeAsString = currentDateTime.format(formatter);
            System.out.println("11111");
            myDB.add(labelList.get(maxIndex), dateStr, timeAsString, timeAsString);
            System.out.println("22222");
        }


        // Before returning, rotate back by -90 degrees
        Mat b = rotated_mat_image.t();
        Core.flip(b, mat_image, 0);
        b.release();

        return mat_image;
    }




private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
    ByteBuffer byteBuffer;
    int numChannels = 1; // Chỉ có một kênh cho ảnh xám

    // Tính toán kích thước cần thiết cho ByteBuffer
    int bufferSize = INPUT_SIZE * INPUT_SIZE * numChannels * 4; // 4 bytes cho Float32

    byteBuffer = ByteBuffer.allocateDirect(bufferSize);
    byteBuffer.order(ByteOrder.nativeOrder());

    int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    int pixel = 0;

    for (int i = 0; i < INPUT_SIZE; ++i) {
        for (int j = 0; j < INPUT_SIZE; ++j) {
            final int val = intValues[pixel++];
            // Lấy giá trị pixel xám
            float pixelValue = (val & 0xFF) / 255.0f;
            byteBuffer.putFloat(pixelValue);
        }
    }
    return byteBuffer;
}

}
// Next video is about drawing box and labeling it
// If you have any problem please inform me