///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// COMPANY: Boca Systems Inc.
// PROJECT: Android Tablet Printer Tester Sample Code
// RELEASE DATES:
// Version 1.0 - June 2016
// Version 1.1 - August 2016 Fixed portrait/landscape bug and added help screen
// Version 2.0 - January 2017 Implemented the use of our SDK and added an assortment of new functions
//               included download/print logos.  Also eliminated the need for PlugPDF addon.
// Version 3.0 - April 2017 Added USB & WIFI communication to the SDK.  Added auto reconnect functions for BT and USB.
//               Also enhanced the Android Tester Users Guide included as an HTML file.
// Version 4.0 - May 2018 Updated Android Studio to version 3.1.2.  Eliminated deprecated functions.
//               Moved several method out of the main UI thread to background threads for better execution.
//               Added BocaConnect to handle opening USB, BT and WIFI ports, with user message to wait.
//
// Author: Michael Hall - michael@bocasystems.com
//
// This SDK can be used to support a main program as demonstrated in the MainActivity.java included in
// this project.  Also pieces of the SDK can be copied into another project and used as is.
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package bocasystems.com.sdk;

import android.app.ActivityManager;
import android.app.PendingIntent;
// import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
//4.0 import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.content.BroadcastReceiver;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.graphics.pdf.PdfRenderer.Page;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.annotation.MainThread;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import java.net.*;
import java.io.*;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
//4.0 import android.widget.ArrayAdapter;
//4.0 import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
//4.0 import java.io.BufferedInputStream;
//4.0 import java.io.FileInputStream;
//4.0 import java.io.IOException;
//4.0 import java.io.FileNotFoundException;
//4.0 import java.io.InputStream;
//4.0 import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import android.graphics.Matrix;
import android.os.ParcelFileDescriptor;
//4.0 import java.io.File;
import android.content.Context;
import java.lang.ref.WeakReference;             //4.0

//3.0 Added USB Support
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;

// import BocaLibrary.app.src.main.java.com.bocasystems.com.sample.MainActivity;


public abstract class BocaSystemsSDK {

    //4.0 public BluetoothAdapter myBluetoothAdapter;
    public TextView text;
    //4.0 public ListView myListView;
    //4.0 public ArrayAdapter<String> BTArrayAdapter;
    //4.0 public CharSequence Device;
    //4.0 public BluetoothServerSocket mServerSocket;
    //4.0 public BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    //4.0 private BluetoothDevice BTdevice;         //4.0 made it local

    //Handler/Callback function and constants
    //Used for threads to communicate with each other
    static private Handler mHandler;            //4.0 made it static
    static final int CONNECTION_FAILED = 0;
    static final int CONNECTION_SUCCESSFUL = 1;
    static final int DISCONNECTED = 2;
    static final int MESSAGE_READ = 3;
    static final int CONNECTING = 4;
    static final int WRITE_FAILED = 5;
    static final int WRITE_FAILED_ABORT = 6;
    static final int STATUS_REPORT = 9;
    static boolean connected = false;           //4.0 made it static
    static boolean reading = false;             //4.0 made it static
    //private final int DATA = 1;               //4.0 made it local
    //private final int ZEROS = 0;              //4.0 made it local

    static private BluetoothSocket btsocket;            //4.0 made it static
    private UsbInterface interfaceOut;
    private UsbInterface interfaceIn;
    private int portNumber=9100;            //4.0 Boca Systems Ethernet printer port number
    private Socket client;                  //4.0
    private OutputStreamWriter printwriter;
    private InputStreamReader printreader;
    private BocaSystemsSDK.ConnectedThreadBT mConnectedThreadBT;
    private BocaSystemsSDK.ConnectedThreadUSB mConnectedThreadUSB;
    private BocaSystemsSDK.ConnectedThreadWIFI mConnectedThreadWIFI;

    //byte[] BitmapData;

    static byte[] readBuf = new byte[128];         //4.0
    //4.0 int count = 0;

    //The application Paint uses a 62 bit header
    //The BMP header used by Boca Systems printers is 54 bytes, ignoring the last two integers
    private int mDataWidth;
    private int nRead;              //byte count of BMP file
    //4.0 byte[] mDataArray;
    //4.0 byte[] mRawBitmapData;
    private int[] mDataArray;
    private int[] mRawBitmapData;
    private int mDataArrayLength;
    private int mWidth;
    private int mHeight;

    private byte[] BMPHeader;
    /*
        class BMPHeader {
            short bfType;
            int bfSize;
            short bfReserved1;
            short bfReserved2;
            int bfOffBits;
        }

        class BITMAPINFOHEADER {
            int biSize;
            int  biWidth;
            int  biHeight;
            short  biPlanes;
            short  biBitCount;
            int biCompression;
            int biSizeImage;
            int  biXPelsPerMeter;
            int  biYPelsPerMeter;
            int biClrUsed;
            int biClrImportant;
            int biReserved1;
            int biReserved2;
        }
    */

    class FileHeader {
        char bfType0;
        char bfType1;
        int bfSize;
        short bfR1;
        short bfR2;
        int bfOffBits;
    }

    class InfoHeader {
        int biSize;
        int biWidth;
        int biHeight;
        short biPlanes;
        short biBitCount;
        int biCompression;
        int biSizeImage;
        int biXPPMeter;
        int biYPPMeter;
        int biClrUsed;
        int biClrImportant;
        int biBlackBitMask;
        int biWhiteBitMask;
    }

    public class LineParser {
        public int start;                  //starting positions for block of zeros/data
        private int count;                  //number of zeros/data in block
        private int block_type;             //0 for zeros and 1 for data
    }

    //;

    //public class ArgbColor_Original {
    //Byte Alpha;
    //Byte Red;
    //Byte Green;
    //Byte Blue;
    //


    //since unsigned byte unavailable integers used
    public class ArgbColor {
        int Alpha;
        int Red;
        int Green;
        int Blue;

        private ArgbColor() {
        }
    }

    //Monochrome Colors
    private final Byte BLACK = 0;
    private final Byte WHITE = 1;
    private final int DITHERTHRESHOLD = 128;
    private int readThreadCount = 0;

// --Commented out by Inspection START (5/18/18, 3:16 PM):
//Stock Sizes
//    final int CONCERT = 0;
//    final int CINEMA = 1;
//    final int CREDITCARD = 2;
//    final int RECEIPT = 3;
//    final int SKI = 4;
//    final int FOURBY = 5;
//    final int W1 = 6;
//    final int W2 = 7;
//    final int LETTER = 8;
// --Commented out by Inspection STOP (5/18/18, 3:16 PM)

    //Global State Variables and default values
    //Setting printer configuration default values based upon most common printer and stock
    private boolean connectionStatus = false;
    private int PrinterResolution = 300;                           //Default to 300 DPI
    private String PrinterPath = "<P1>";                    //Default to path 1
    private String PrinterOrientation = "<LM>";                    //Default to Landscape mode
    private double StockHeight = 2.0;                            //Default to Concert Stock height
    private double StockWidth = 5.5;                             //Default to Concert Stock width
    private boolean ImageScaled = false;
    private boolean ImageDithered = true;

    private int portrait_dots = 0;
    private int portrait_dot_array[][] = new int[9][3];
    private int size_indicator = 0;
    private int originalx = 0;              //Row position for image
    private int originaly = 0;              //Column position for image

    private String selectedFileName = null; /* File Name Only, i.e file.txt */
    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final OutputStream mmOutStream = null;
    private String FileExtension = "";
    private String LogoNumber = "";
    static String StatusReturned = "";              //return string from printer    //4.0 made it static
    static String Mode = "";                        //3.0 Added USB //4.0 made it static

    //3.0 Added USB
    //4.0 Spinner spEndPoint;
    private ArrayList<UsbEndpoint> listUsbEndpoint;
    //4.0 ArrayAdapter<String> adapterEndpoint;


    //3.0 Added USB Support
    private UsbManager mUsbManager;
    private static final String ACTION_USB_PERMISSION = "bocasystems.com.sdk.USB_PERMISSION";
    //private static final String ACTION_USB_DEVICE_ATTACHED = "bocasystems.com.sdk.USB_CONNECT";
    //private static final String ACTION_USB_DEVICE_DETACHED = "bocasystems.com.sdk.USB_DISCONNECT";
    private PendingIntent USBPermission;
    //private PendingIntent USBConnect;
    //private PendingIntent USBDisconnect;

    //4.0 PendingIntent BTPermission;
    //4.0 private PendingIntent BTConnect;
    //4.0 private PendingIntent BTDisconnect;

    //4.0 PendingIntent WIFIPermission;
    //4.0 private PendingIntent WIFIConnect;
    //4.0 private PendingIntent WIFIDisconnect;
    //4.0 private PendingIntent WIFIOther;
    //4.0 private PendingIntent WIFIReason;

    private UsbDevice USBdevice;
    static boolean VerifyConnection=false;          //4.0 made it static
    //4.0 private String CurrentBTDevice = "";      //4.0 made this a local

    //4.0 private byte[] bytes;
    //4.0 private byte[] bytesin;
    //4.0 private static int TIMEOUT = 0;       //4.0 made this a local

    private boolean WifiProcessing = false;             //used to control WIFI connection timing for success/failure

    public abstract void StatusReportCallback(String statusReport);
    public abstract long getMemorySizeInBytes();

    //4.0 static inner class doesn't hold an implicit reference to the outer class
    private static class mHandler extends Handler {

        //Using a weak reference means you won't prevent garbage collection
        private final WeakReference<BocaSystemsSDK> myClassWeakReference;

        private mHandler(BocaSystemsSDK myClassInstance) {
            myClassWeakReference = new WeakReference<BocaSystemsSDK>(myClassInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            BocaSystemsSDK myClass = myClassWeakReference.get();
            if (myClass != null) {

                super.handleMessage(msg);

                switch (msg.what) {

                    case CONNECTING:
                        //AppendStatus("SDK - Attempting Connection.");
                        break;

                    case CONNECTION_SUCCESSFUL:

                        //Toast.makeText(getApplicationContext(), "Connection Successful", Toast.LENGTH_SHORT).show();

                        connected = true;        //set connection status
                        break;

                    case CONNECTION_FAILED:

                        //Toast.makeText(getApplicationContext(),"Connection Failed" , Toast.LENGTH_SHORT).show();

                        connected = false;        //reset connection status
                        break;

                    case DISCONNECTED:

                        //Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();

                        connected = false;        //reset connection status
                        btsocket = null;        //clear pointer
                        reading = false;        //Stop while loop in read thread
                        VerifyConnection = false;

                        break;

                    case WRITE_FAILED:
                        //AppendStatus("Write Failed. Trying again.");
                        break;

                    case WRITE_FAILED_ABORT:
                        //AppendStatus("Write Failed. Aborting...");
                        break;

                    case MESSAGE_READ:

                        if(Mode.equals("WIFI"))
                            readBuf = CharsToBytes((char[])msg.obj);
                        else
                            readBuf = (byte[]) msg.obj;
                        EstablishStatus(msg.arg1);

                        break;

                    case STATUS_REPORT:
                        AppendStatus(String.valueOf(msg.arg2));
                        break;
                }
            }
        }

        //4.0 This routine distinquishes between known status responses and string responses
        private void EstablishStatus(int count)
        {
            int i;
            byte x;
            String statusResponse;
            String stringMessage = "";

            //loop for each character in readBuf array
            for(i = 0; i < count; i++)
            {
                //look at each character
                x = readBuf[i];

                //Check the byte for a known status value
                switch (x) {
                    case 6:
                        statusResponse = "Ticket ACK";
                        break;
                    case 16:
                        statusResponse = "Out of Tickets";
                        break;
                    case 17:
                        statusResponse = "X-On";
                        break;
                    case 18:
                        statusResponse = "Power On";
                        break;
                    case 19:
                        statusResponse = "X-Off";
                        break;
                    case 21:
                        statusResponse = "Ticket NAK";
                        break;
                    case 24:
                        statusResponse = "Ticket Jam";
                        break;
                    case 25:
                        statusResponse = "Illegal Data";
                        break;
                    case 26:
                        statusResponse = "Power Up Problem";
                        break;
                    case 27:
                        statusResponse = "Ticket NAK";
                        break;
                    case 28:
                        statusResponse = "Downloading Error";
                        break;
                    case 29:
                        statusResponse = "Cutter Jam";
                        break;
                    default:
                        statusResponse = "";    //else null
                        break;
                }

                //If not a known status value, check to see if byte is a printable character
                //                if ((x >= 32) && (x < 128))
                if (x >= 32)
                {
                    stringMessage += (char)x;
                }
                else
                {
                    //If not, is there a status response or string message established, then display it
                    if(stringMessage.length() > 0)
                    {
                        AppendStatus(stringMessage);
                        stringMessage = "";
                    }
                    if(statusResponse.length() > 0)
                        AppendStatus(statusResponse);
                }
            }
            //Display any residual string message that may have been built and not yet displayed
            if(stringMessage.length() > 0)
                AppendStatus(stringMessage);

        }

        //append text message to Printer Status area
        private void AppendStatus (String msg) {
            StatusReturned = StatusReturned + msg;
        }

    }

    //4.0
    private Handler getHandler() {
        return new mHandler(this);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //HIGH LEVEL SUPPORT FUNCTIONS SECTION
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //This routine will load image files into a UIImage structure, then take that image and convert it to
    //a monochrome BMP file and send that to the printer.  Also raw text files can be sent directly to the
    //printer through this routine
    //Currently supported file types include:
    // JPG, PNG and BMP - image files
    // PDF - Document files, first page
    // TXT - Raw text files containing FGL commands and text to be printed
    //

    public boolean SendFile(String filename, int row, int column) {

        boolean status;
        LogoNumber = "";
        originalx = row;
        originaly = column;

        if (filename.endsWith("TXT") || filename.endsWith("txt")) {
            FileExtension = "txt";
            status = RWTextFile(filename);
        } else if (filename.endsWith("PDF") || filename.endsWith("pdf")) {
            FileExtension = "pdf";
            status = PdfToBmp(filename);
        } else if (filename.endsWith("PNG") || filename.endsWith("png")) {
            FileExtension = "png";
            status = ImageToBmp(filename);
        } else if (filename.endsWith("JPG") || filename.endsWith("jpg") || filename.endsWith("JPEG") || filename.endsWith("jpeg")) {
            FileExtension = "jpg";
            status = ImageToBmp(filename);
        } else if (filename.endsWith("BMP") || filename.endsWith("bmp")) {
            FileExtension = "bmp";
            status = ImageToBmp(filename);
        } else {
            FileExtension = "";
            status = false;
        }

        return (status);
    }

    public boolean DownloadLogo(String filename, int idnum) {
        boolean status;

        LogoNumber = String.valueOf(idnum);

        if (filename.endsWith("PDF") || filename.endsWith("pdf")) {
            FileExtension = "pdf";
            status = PdfToBmp(filename);
        } else if (filename.endsWith("PNG") || filename.endsWith("png")) {
            FileExtension = "png";
            status = ImageToBmp(filename);
        } else if (filename.endsWith("JPG") || filename.endsWith("jpg") || filename.endsWith("JPEG") || filename.endsWith("jpeg")) {
            FileExtension = "jpg";
            status = ImageToBmp(filename);
        } else if (filename.endsWith("BMP") || filename.endsWith("bmp")) {
            FileExtension = "bmp";
            status = ImageToBmp(filename);
        } else {
            FileExtension = "";
            status = false;
        }

        return (status);
    }

    public boolean PrintLogo(int idnum, int row, int column) {
        boolean status = true;
        try {
            String outgoing = "<SP";
            outgoing = outgoing + String.valueOf(row);
            outgoing = outgoing + ",";
            outgoing = outgoing + String.valueOf(column);
            outgoing = outgoing + "><LD";
            outgoing = outgoing + String.valueOf(idnum);
            outgoing = outgoing + ">";
            SendString(outgoing);
        } catch (Exception e) {
            status = false;
            e.printStackTrace();
        }


        return (status);
    }

    public void ChangeConfiguration(String path, int resolution, boolean scaled, boolean dithered, int stocksizeindex, String orientation) {
        PrinterPath = path;                             //Printer Path 1, 2, 3 or 4
        PrinterResolution = resolution;                 //DPI 200, 300 or 600
        ImageScaled = scaled;                           //Scale image to ticket size false or true
        ImageDithered = dithered;                       //Dither image/barcode etc. false or true
        PrinterOrientation = orientation;               //Landscape or Portrait
        switch (stocksizeindex)                         //Ticket size index from 0 to 8
        {
            case 0:
                //if landscape mode
                if (PrinterOrientation.equals("<LM>")) {
                    //_sizeOut.text = @"Concert (2 x 5.5)";
                    StockHeight = 2.0;
                    StockWidth = 5.5;
                } else //portrait mode
                {
                    //_sizeOut.text = @"Concert (5.5 x 2)";
                    StockHeight = 5.5;
                    StockWidth = 2.0;
                }
                break;

            case 1:

                //if landscape mode
                if (PrinterOrientation.equals("<LM>")) {
                    //_sizeOut.text = @"Cinema (3.25 x 2)";
                    StockHeight = 3.25;
                    StockWidth = 2.0;
                } else //portrait mode
                {
                    //_sizeOut.text = @"Cinema (2 x 3.25)";
                    StockHeight = 2.0;
                    StockWidth = 3.25;
                }
                break;

            case 2:

                //if landscape mode
                if (PrinterOrientation.equals("<LM>")) {
                    //_sizeOut.text = @"Credit Card (2.13 x 3.37)";
                    StockHeight = 2.13;
                    StockWidth = 3.37;
                } else //portrait mode
                {
                    //_sizeOut.text = @"Credit Card (3.37 x 2.13)";
                    StockHeight = 3.37;
                    StockWidth = 2.13;
                }
                break;

            case 3:

                //if landscape mode
                if (PrinterOrientation.equals("<LM>")) {
                    //_sizeOut.text = @"Receipt (3.25 x 8)";
                    StockHeight = 3.25;
                    StockWidth = 8.0;
                } else //portrait mode
                {
                    //_sizeOut.text = @"Receipt (8 x 3.25)";
                    StockHeight = 8.0;
                    StockWidth = 3.25;
                }
                break;

            case 4:

                //if landscape mode
                if (PrinterOrientation.equals("<LM>")) {
                    //_sizeOut.text = @"Ski (3.25 x 6)";
                    StockHeight = 3.25;
                    StockWidth = 6.0;
                } else //portrait mode
                {
                    //_sizeOut.text = @"Ski (6 x 3.25)";
                    StockHeight = 6.0;
                    StockWidth = 3.25;
                }
                break;

            case 5:

                //if landscape mode
                if (PrinterOrientation.equals("<LM>")) {
                    //_sizeOut.text = @"4 x 6";
                    StockHeight = 4.0;
                    StockWidth = 6.0;
                } else //portrait mode
                {
                    //_sizeOut.text = @"6 x 4";
                    StockHeight = 6.0;
                    StockWidth = 4.0;
                }
                break;

            case 6:

                //if landscape mode
                if (PrinterOrientation.equals("<LM>")) {
                    //_sizeOut.text = @"Wristband 1 (11 x 1)";
                    StockHeight = 1.0;
                    StockWidth = 11.0;
                } else //portrait mode
                {
                    //_sizeOut.text = @"Wristband 1 (1 x 11)";
                    StockHeight = 11.0;
                    StockWidth = 1.0;

                }
                break;

            case 7:

                //if landscape mode
                if (PrinterOrientation.equals("<LM>")) {
                    //_sizeOut.text = @"Wristband 2 (11 x 1.328)";
                    StockHeight = 1.328;
                    StockWidth = 11.0;
                } else //portrait mode
                {
                    //_sizeOut.text = @"Wristband 2 (1.328 x 11)";
                    StockHeight = 11.0;
                    StockWidth = 1.328;
                }
                break;

            case 8:                                 //used for unformatted ticket size.  8.5 x 11 does not really exist

                //if landscape mode
                if (PrinterOrientation.equals("<LM>")) {
                    //_sizeOut.text = @"Letter (8.5 x 11)";
                    StockHeight = 8.5;
                    StockWidth = 11.0;
                } else //portrait mode
                {
                    //_sizeOut.text = @"Letter (11 x 8.5)";
                    StockHeight = 11.0;
                    StockWidth = 8.5;
                }
                break;

            default:
                stocksizeindex = 0;
                break;
        }
        size_indicator = stocksizeindex;                //set index value for the portrait_dot_array
        set_portrait_dots();

    }

    public void SendString(final String string)
    {
        //3.0 Added USB
        //3.0 Added WIFI
        //4.0 Switch
        switch (Mode) {
            case "BT":
                Write_BT(string);
                break;
            case "USB":
                Write_USB(string);
                break;
            case "WIFI":
                Write_WIFI(string);
                break;
            default:
                StatusReportCallback("Invalid Mode");
                break;
        }
    }

    public void SendData(final byte[] buf, boolean FF)
    {
        //3.0 Added USB
        //3.0 Added WIFI
        //4.0 Switch
        //6.1 Added FF boolean for WIFI
        switch (Mode) {
            case "BT":
                Write_BT_Data(buf);
                break;
            case "USB":
                Write_USB_Data(buf);
                break;
            case "WIFI":
                Write_WIFI_Data(buf, FF);           //4.0   //6.1 Added FF boolean for WIFI
                break;
            default:
                StatusReportCallback("Invalid Mode");
                break;
        }
    }

    public void ClearMemory() {
        byte[] CM;
        CM = new byte[2];
        CM[0] = 0x1b;           //Esc
        CM[1] = 0x63;           //c
        SendData(CM, false);           //Send Escape c    //6.1 Added FF boolean for WIFI
    }

    public void PrintCut() {
        final String CUT = "<p>";           //FGL cut command
        SendString(CUT);
    }

    public void PrintNoCut()
    {
        final String EJECT = "<q>";         //FGL No cut command
        SendString(EJECT);
    }

    /*
    public void ReadPrinter() {
        String stat = "";

        switch (Mode) {
            //connect USB through the OTG adapter
            case "USB":                                                     //4.0
                stat = Read_USB();
                ClearStatus_USB();
                break;

            case "BT":                                                      //4.0
                stat = Read_BT();
                ClearStatus_BT();
                break;

            case "WIFI":                                                    //4.0
                stat = Read_WIFI();
                ClearStatus_WIFI();
                break;

            default:
                break;
        }
        StatusReportCallback(stat);
    }
    */

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //BLUETOOTH SUPPORT FUNCTIONS SECTION
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public class ConnectedThreadBT extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private ConnectedThreadBT(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //As a first timer Android Application writer, I think I have done a less than
        //stellar job on this read thread.  I did not quite understand it and ran out
        //of time before I figured it out.  I did however get this to read status from the printer
        //which was my main goal.
        public void run() {
            byte[] buffer = new byte[1024];        // buffer store for the stream
            int bytes;                            // bytes returned from read()
            int x, y = 0;                        // used just to create a delay
            reading = true;

            // Keep listening to the InputStream until an exception occurs
            while (reading)
            {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                //Slight delay to allow incoming data to be handled.  Probably should be using
                //a postDelayed command here, but I ran out of time before I figure it out.
                //This delay loop is crude but works.  Should someone have a better way to do
                //this, please email the modified code and I will update the example code on the web site for everyone.

                //mHandler.postDelayed(mRunnable,100);
                for (x = 0; x < 100000; x++) {
                    y++;
                }
                y = 0;
            }
            //AppendStatus("Exiting Read Thread");
        }

        //Call this from the main Activity to send data to the remote device
        private  void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Call this from the main Activity to shutdown the connection
        public void cancel() {
            try {
                mmSocket.close();
                mHandler.sendEmptyMessage(DISCONNECTED);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    private final BroadcastReceiver mBTReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) //&& Global.tryBluetoothReconnect)
            {
                VerifyConnection = true;
            }

            if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) //&& Global.tryBluetoothReconnect)
            {
                VerifyConnection = false;
                mHandler.sendEmptyMessage(DISCONNECTED);
            }

        }

    };

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //BLUETOOTH SDK SECTION
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean OpenSessionBT(String device, Context context)
    {
        String CurrentBTDevice = "";                //4.0
        InitDotArray();                             //used for positioning in portrait mode
        mHandler = getHandler();                    //4.0 get a message handler

        //register the BLUETOOTH broadcast receiver
        //BTConnect = PendingIntent.getBroadcast(context, 0, new Intent(BluetoothDevice.ACTION_ACL_CONNECTED),0);
        //BTDisconnect = PendingIntent.getBroadcast(context, 0, new Intent(BluetoothDevice.ACTION_ACL_DISCONNECTED),0);
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        context.registerReceiver(mBTReceiver, filter1);
        context.registerReceiver(mBTReceiver, filter2);

        Mode = "BT";                                //Set communication mode flag for Bluetooth
        connectionStatus = Open_BT(device);
        if (connectionStatus) {
            CurrentBTDevice = device;               //Store this for possible automatic reconnect in the case of a disconnect
            mContext = context;
        }
        else
            Mode = "";                              //Clear communication mode flag

        return (connectionStatus);
    }
/*

	public void OpenSessionBT(final String device, Context context) {

        Mode = "BT";
        connecting = true;
        mContext = context;
        connected = false;

        InitDotArray();
        mHandler = getHandler();

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        context.registerReceiver(mBTReceiver, filter1);
        context.registerReceiver(mBTReceiver, filter2);

        connectThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    String address;
                    BluetoothDevice BTdevice;

                    address = device.substring(11);
                    BTdevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

                    try {
                        btsocket = BTdevice.createRfcommSocketToServiceRecord(uuid);
                        btsocket.connect();

                        if (!Thread.interrupted()){

                            connected = true;
                            mConnectedThreadBT = new ConnectedThreadBT(btsocket);
                            mConnectedThreadBT.start();
                        }

                    } catch (IOException e) {
                        mHandler.sendEmptyMessage(CONNECTION_FAILED);
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                connecting = false;
                connectionStatus = connected;
                verifyConnection = connected;
				if (!verifyConnection) Mode = "";
            }
        });
        connectThread.start();
    }

*/
    public void CloseSessionBT()
    {
        Close_BT();
        Mode = "";                                  //Clear communication mode flag

    }

    public boolean VerifyConnectionBT()
    {
        return(VerifyConnection);       //4.0 simplified
    }

    //4.0 private
    private boolean Open_BT(String deviceselected) {
        String address = "";
        BluetoothDevice BTdevice;           //4.0

        boolean status = false;
        address = deviceselected.substring(11);
        BTdevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);

        //Display Connecting message
        //mHandler.sendEmptyMessage(CONNECTING);

        try {
            btsocket = BTdevice.createRfcommSocketToServiceRecord(uuid);
            btsocket.connect();
            //mHandler.sendEmptyMessage(CONNECTION_SUCCESSFUL);
            connected = true;        //set connection status
            status = true;

            // Start the thread to manage the connection and perform transmissions
            mConnectedThreadBT = new ConnectedThreadBT(btsocket);
            mConnectedThreadBT.start();


        } catch (IOException e) {
            mHandler.sendEmptyMessage(CONNECTION_FAILED);
            e.printStackTrace();
        }

        return (status);
    }

    private void Close_BT() {
        try {
            btsocket.close();
            mHandler.sendEmptyMessage(DISCONNECTED);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void Write_BT(String command) {
        mConnectedThreadBT.write(command.getBytes());
    }

    private void Write_BT_Data(byte[] buf) {
        mConnectedThreadBT.write(buf);
    }

    //return the data in the Status Returned buffer to the main application.
    //The Status Returned buffer is being filled by the BT read thread above.
    //It listens to the printer for any possible data being returned from the printer
    private String Read_BT()
    {
        return (StatusReturned);
    }

    //Once data has been returned from the buffer to the main applicatio, clear the buffer
    private void ClearStatus_BT()
    {
        StatusReturned = "";
    }

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    public static String bytesToHex(byte[] bytes, int length) {
        byte[] hexChars = new byte[length * 2];
        for (int j = 0; j < length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //USB SUPPORT FUNCTIONS SECTION
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public class ConnectedThreadUSB extends Thread
    {
        private final UsbDeviceConnection connection;
        private final UsbEndpoint mmInStream;
        private final UsbEndpoint mmOutStream;

        private ConnectedThreadUSB(UsbDevice device)
        {
            // StatusReturned += "Starting ConnectedThreadUSB";

            connection = mUsbManager.openDevice(device);
            UsbEndpoint tmpIn = null;
            UsbEndpoint tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try
            {
                //Setup write channel
                interfaceOut = device.getInterface(0);
                tmpOut = interfaceOut.getEndpoint(0);

                //Setup read channel
                interfaceIn = device.getInterface(1);
                tmpIn = interfaceIn.getEndpoint(1);

                connection.claimInterface(interfaceOut, true);
                connection.claimInterface(interfaceIn, true);
            }
            catch (Exception e)
            {
                mHandler.sendEmptyMessage(CONNECTION_FAILED);
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        //As a first timer Android Application writer, I think I have done a less than
        //stellar job on this read thread.  I did not quite understand it and ran out
        //of time before I figured it out.  I did however get this to read status from the printer
        //which was my main goal.
        //As a first timer Android Application writer, I think I have done a less than
        //stellar job on this read thread.  I did not quite understand it and ran out
        //of time before I figured it out.  I did however get this to read status from the printer
        //which was my main goal.
        public void run() {
            final int SIZE_BUFFER = 512;
            final int TIMEOUT_SHORT = 50;
            final int TIMEOUT_INFINITE = 0;
            byte[] buffer = new byte[SIZE_BUFFER];
            byte[] xfer = new byte[SIZE_BUFFER];
            String statusReport = "";
            int timeout = 500; // TIMEOUT_INFINITE;
            int tries = 0;
            int bytes = 0;
            int xferLength = 0;
            int i;
            int j;
            for (i = 0; i < SIZE_BUFFER; i++)
            {
                buffer[i] = 0x00;
            }
            if (0 < readThreadCount)
            {
                return;
            }
            readThreadCount++;
            reading = true;

            // Keep listening to the InputStream until an exception occurs
            StatusReportCallback("Entering Read Thread");
            while (reading) {

                for (i = 0; i < bytes; i++)
                {
                    buffer[i] = 0x00;
                }
                try {
                    for (i = 0; i < bytes; i++)
                    {
                        buffer[i] = 0x00;
                    }
                    // Read from the InputStream
                    //bytes = mmInStream.read(buffer);
                    // TEMP: ABR
                    bytes = connection.bulkTransfer(mmInStream, buffer,0, SIZE_BUFFER, timeout);
                    if (0 < bytes)
                    {
                        for (i = 0; i < bytes; i++)
                        {
                            switch (buffer[i])
                            {
                                case 6:
                                    StatusReportCallback("Ticket ACK");
                                    timeout = TIMEOUT_INFINITE;
                                    Thread.sleep(100);
                                    break;
                                case 16:
                                    StatusReportCallback("Out of Tickets");
                                    timeout = TIMEOUT_INFINITE;
                                    break;
                                case 17:
                                    StatusReportCallback("X-On");
                                    timeout = TIMEOUT_INFINITE;
                                    break;
                                case 18:
                                    StatusReportCallback("Power On");
                                    timeout = TIMEOUT_INFINITE;
                                    break;
                                case 19:
                                    StatusReportCallback("X-Off");
                                    timeout = TIMEOUT_INFINITE;
                                    break;
                                case 21:
                                    StatusReportCallback("Ticket NAK");
                                    timeout = TIMEOUT_INFINITE;
                                    break;
                                case 24:
                                    StatusReportCallback("Ticket Jam");
                                    timeout = TIMEOUT_INFINITE;
                                    break;
                                case 25:
                                    StatusReportCallback("Illegal Data");
                                    timeout = TIMEOUT_INFINITE;
                                    break;
                                case 26:
                                    StatusReportCallback("Power Up Problem");
                                    timeout = TIMEOUT_INFINITE;
                                    break;
                                case 27:
                                    StatusReportCallback("Ticket NAK");
                                    timeout = TIMEOUT_INFINITE;
                                    break;
                                case 28:
                                    StatusReportCallback("Downloading Error");
                                    timeout = TIMEOUT_INFINITE;
                                    break;
                                case 29:
                                    StatusReportCallback("Cutter Jam");
                                    break;
                                case 0x0D:
                                case 0x0A:
                                    if (0 < statusReport.length()) {
                                        StatusReportCallback(statusReport);
                                        timeout = TIMEOUT_INFINITE;
                                        // StatusReturned = StatusReturned + statusReport;
                                    }
                                    tries = 0;
                                    statusReport = "";
                                default:
                                    statusReport += (char)buffer[i];
                                    timeout = TIMEOUT_SHORT;
                                    tries = 0;
                                    break;
                            }
                        }
                    }
                    else if (TIMEOUT_SHORT == timeout && 0 < statusReport.length()) {
                        if (0 == bytes) {
                            // This section is only for status reports
                            // that do not come with a trailing "\r"
                            if (1 < tries++) {
                                StatusReportCallback(statusReport);
                                // StatusReturned = StatusReturned + statusReport;
                                timeout = TIMEOUT_INFINITE;
                                tries = 0;
                                statusReport = "";
                            }
                        } else if (0 > bytes) {
                            // statusReport += String.format("\r\nTransmission Error : %d", bytes);
                            if (3 < tries++)
                            {
                                StatusReportCallback(statusReport);
                                // StatusReturned = StatusReturned + statusReport;
                                timeout = TIMEOUT_INFINITE;
                                tries = 0;
                                statusReport = "";
                            }
                        }
                    }
                } catch (Exception e) {
                    // reading = false;
                    break;
                }
            }
            //AppendStatus("Exiting Read Thread");
            StatusReportCallback("Exiting Read Thread");
            readThreadCount--;
        }

        //3.0 Added USB Support
        //Call this to send data out the USB port to the remote device
        private void write(final byte[] bytes) {

            int TIMEOUT = 0;                            //4.0 made this a local
            int EachTransfer = 0;                       //amount transfered each time
            int TotalTransfered = 0;                    //running total of amount of data transfered
            int TotalToBeTransfered = bytes.length;     //Grand total target
            int LeftToBeTransfered = bytes.length;      //Count down from grand total to zero
            int MaxTransfer = 16384;                     //maximum block size per USB transfer
            boolean GottaTransfer = true;               //control loop
            int FailureCount = 0;
            if (0 == bytes.length)
            {
                return;
            }

            //When sending large amounts of data through the USB port, it must be subdivided into 16384 byte
            //blocks so a loop is used to repeatedly perform bulk transfers at the max size
            //until all the data has been transmitted.
            try
            {
                //Continue as long as we have something to transfer
                while (GottaTransfer)
                {
                    //if the data left to transfer is smaller than the maximum block size allowable, use the
                    //actual amount otherwise use the maximum block size of 16384 bytes.
                    if (LeftToBeTransfered < MaxTransfer)
                        MaxTransfer = LeftToBeTransfered;

                    //Perform transfer and look at each transfer return value to determine success/failure.
                    //if the value returned is less than 0, then the transfer failed.
                    EachTransfer = connection.bulkTransfer(mmOutStream, bytes, TotalTransfered, MaxTransfer, TIMEOUT);

                    //if less than 0 - failure
                    if (EachTransfer < 0)
                    {
                        //Issue write failed message but try again, up to 10 times total
                        mHandler.obtainMessage(WRITE_FAILED).sendToTarget();
                        FailureCount++;
                        if (20 < FailureCount)
                            GottaTransfer = false;
                    }
                    else
                    //recalculate how much is left to be transferred by adding each transfer up
                    //and subtracting from the grand total in the beginning.  Also use the total
                    //transfered value as the offset index for the next bulk transfer above.
                    {
                        TotalTransfered += EachTransfer;
                        LeftToBeTransfered = TotalToBeTransfered - TotalTransfered;
                        //if finished, reset the flag to exit while loop
                        if (LeftToBeTransfered <= 0)
                            GottaTransfer = false;
                    }
                }

            }
            catch (Exception e)
            {
                //Exception fault, issue message and abort transfer
                mHandler.obtainMessage(WRITE_FAILED_ABORT).sendToTarget();
                GottaTransfer = false;
            }
        }


    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action))
            {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false))
                    {
                        if(device != null){
                            connectionStatus = Open_USB();
                            VerifyConnection = connectionStatus;
                        }
                    }
                    //else {
                    //Toast.makeText(MainActivity.this,
                    //"permission denied for device " + device,
                    //Toast.LENGTH_LONG).show();
                    //}
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))
            {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null)
                {
                    VerifyConnection = true;
                }
            }

            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action))
            {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null)
                {
                    VerifyConnection = false;
                }
            }

        }

    };


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //USB SDK SECTION                                                                           //3.0 Added USB
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public boolean OpenSessionUSB(Context context)
    {
        PendingIntent USBConnect;
        PendingIntent USBDisconnect;

        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        InitDotArray();                             //used for positioning BMP in portrait mode
        mHandler = getHandler();                    //4.0 get a message handler

        //register the broadcast receiver
        USBPermission = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
        USBConnect = PendingIntent.getBroadcast(context, 0, new Intent(UsbManager.ACTION_USB_DEVICE_ATTACHED), 0);
        USBDisconnect = PendingIntent.getBroadcast(context, 0, new Intent(UsbManager.ACTION_USB_DEVICE_DETACHED), 0);
        //ACTION_USB_DEVICE_ATTACHED
        //ACTION_USB_DEVICE_DETACHED
        IntentFilter filter1 = new IntentFilter(ACTION_USB_PERMISSION);
        IntentFilter filter2 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        IntentFilter filter3 = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(mUsbReceiver, filter1);
        context.registerReceiver(mUsbReceiver, filter2);
        context.registerReceiver(mUsbReceiver, filter3);

        Mode = "USB";                            //Set communication mode flag for USB Port
        connectionStatus = Open_USB();
        VerifyConnection = connectionStatus;

        if (connectionStatus) {
            mContext = context;
        }
        else
            Mode = "";                            //Clear communication mode flag

        return (connectionStatus);
    }

    public void CloseSessionUSB()
    {
        Close_USB();
        Mode = "";                              //Clear communication mode flag

    }

    public boolean VerifyConnectionUSB()
    {
        //4.0 boolean status = VerifyConnection;
        //4.0 return(status);
        return(VerifyConnection);           //4.0 simplified
    }

    //3.0 Added USB
    private boolean Open_USB()
    {

        boolean status = true;

        checkDeviceInfo();

        if(USBdevice == null)
            status = false;
        else {
            Boolean permitToRead = mUsbManager.hasPermission(USBdevice);
            //Display Connecting message
            //mHandler.sendEmptyMessage(CONNECTING);

            if (!permitToRead) {
                mUsbManager.requestPermission(USBdevice, USBPermission);
            } else {
                try {
                    //mHandler.sendEmptyMessage(CONNECTION_SUCCESSFUL);
                    connected = true;        //set connection status
                    status = true;
                    reading = true;
                    mConnectedThreadUSB = new ConnectedThreadUSB(USBdevice);
                    mConnectedThreadUSB.start();
                } catch (Exception e) {
                    mHandler.sendEmptyMessage(CONNECTION_FAILED);
                    e.printStackTrace();
                }
            }
        }

        return (status);
    }
    //3.0 Added USB
    private void Close_USB()
    {
        try
        {
            reading = false;
            mConnectedThreadUSB.connection.releaseInterface(interfaceIn);
            mConnectedThreadUSB.connection.releaseInterface(interfaceOut);
            mConnectedThreadUSB.connection.close();
        }
        catch(Exception ex)
        {

        }
    }

    //3.0 Added USB
    private void Write_USB(final String command)
    {
        mConnectedThreadUSB.write(command.getBytes());
    }

    //3.0 Added USB
    private void Write_USB_Data(final byte[] buf)
    {
        mConnectedThreadUSB.write(buf);
    }

    //3.0 Added USB
    private String Read_USB() {
        return (StatusReturned);
    }

    //3.0 Added USB
    private void ClearStatus_USB() {
        StatusReturned = "";
    }

    //3.0 Added USB
    private void checkDeviceInfo() {

        //3.0 Added USB
        //4.0 Spinner spDeviceName;
        ArrayList<String> listDeviceName;           //4.0
        ArrayList<UsbDevice> listUsbDevice;         //4.0
        //4.0 ArrayAdapter<String> adapterDevice;

        listDeviceName = new ArrayList<String>();
        listUsbDevice = new ArrayList<UsbDevice>();

        //3.0
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        //3.0
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            listDeviceName.add(device.getDeviceName());
            listUsbDevice.add(device);
        }

        int position = 0;
        UsbDevice device;
        if(listUsbDevice.size() > 0) {
            device = listUsbDevice.get(position);
            checkUsbDevice(device);
        }
        else
            device = null;

        USBdevice = device;

    }

    //3.0 Added USB
    private void checkUsbDevice(UsbDevice d) {
        //3.0 Added USB
        Spinner spInterface;
        ArrayList<String> listInterface;                //4.0
        ArrayList<UsbInterface> listUsbInterface;       //4.0
        //4.0 ArrayAdapter<String> adapterInterface;

        listInterface = new ArrayList<String>();
        listUsbInterface = new ArrayList<UsbInterface>();

        for (int i = 0; i < d.getInterfaceCount(); i++) {
            UsbInterface usbif = d.getInterface(i);
            listInterface.add(usbif.toString());
            listUsbInterface.add(usbif);
        }

        //adapterInterface = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item, listInterface);
        //adapterDevice.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //spInterface.setAdapter(adapterInterface);
        //spInterface.setOnItemSelectedListener(interfaceOnItemSelectedListener);
        int position = 0;
        UsbInterface selectedUsbIf = listUsbInterface.get(position);
        checkUsbInterface(selectedUsbIf);
    }

    //3.0 Added USB
    private void checkUsbInterface(UsbInterface uif) {
        ArrayList<String> listEndPoint;

        listEndPoint = new ArrayList<String>();
        listUsbEndpoint = new ArrayList<UsbEndpoint>();

        for (int i = 0; i < uif.getEndpointCount(); i++) {
            UsbEndpoint usbEndpoint = uif.getEndpoint(i);
            listEndPoint.add(usbEndpoint.toString());
            listUsbEndpoint.add(usbEndpoint);
        }

        //int position = 0;
        //UsbEndpoint selectedEndpoint = listUsbEndpoint.get(position);
    }

    //3.0 Added USB
    AdapterView.OnItemSelectedListener endpointOnItemSelectedListener =
            new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent,
                                           View view, int position, long id) {

                    UsbEndpoint selectedEndpoint = listUsbEndpoint.get(position);

                    String sEndpoint = "\n" + selectedEndpoint.toString() + "\n"
                            + translateEndpointType(selectedEndpoint.getType());

                    //3.0 textEndPoint.setText(sEndpoint);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }

            };

    //3.0 Added USB
    private String translateEndpointType(int type) {
        switch (type) {
            case UsbConstants.USB_ENDPOINT_XFER_CONTROL:
                return "USB_ENDPOINT_XFER_CONTROL (endpoint zero)";
            case UsbConstants.USB_ENDPOINT_XFER_ISOC:
                return "USB_ENDPOINT_XFER_ISOC (isochronous endpoint)";
            case UsbConstants.USB_ENDPOINT_XFER_BULK:
                return "USB_ENDPOINT_XFER_BULK (bulk endpoint)";
            case UsbConstants.USB_ENDPOINT_XFER_INT:
                return "USB_ENDPOINT_XFER_INT (interrupt endpoint)";
            default:
                return "unknown";
        }
    }

    //3.0 Added USB
    private String translateDeviceClass(int deviceClass) {
        switch (deviceClass) {
            case UsbConstants.USB_CLASS_APP_SPEC:
                return "Application specific USB class";
            case UsbConstants.USB_CLASS_AUDIO:
                return "USB class for audio devices";
            case UsbConstants.USB_CLASS_CDC_DATA:
                return "USB class for CDC devices (communications device class)";
            case UsbConstants.USB_CLASS_COMM:
                return "USB class for communication devices";
            case UsbConstants.USB_CLASS_CONTENT_SEC:
                return "USB class for content security devices";
            case UsbConstants.USB_CLASS_CSCID:
                return "USB class for content smart card devices";
            case UsbConstants.USB_CLASS_HID:
                return "USB class for human interface devices (for example, mice and keyboards)";
            case UsbConstants.USB_CLASS_HUB:
                return "USB class for USB hubs";
            case UsbConstants.USB_CLASS_MASS_STORAGE:
                return "USB class for mass storage devices";
            case UsbConstants.USB_CLASS_MISC:
                return "USB class for wireless miscellaneous devices";
            case UsbConstants.USB_CLASS_PER_INTERFACE:
                return "USB class indicating that the class is determined on a per-interface basis";
            case UsbConstants.USB_CLASS_PHYSICA:
                return "USB class for physical devices";
            case UsbConstants.USB_CLASS_PRINTER:
                return "USB class for printers";
            case UsbConstants.USB_CLASS_STILL_IMAGE:
                return "USB class for still image devices (digital cameras)";
            case UsbConstants.USB_CLASS_VENDOR_SPEC:
                return "Vendor specific USB class";
            case UsbConstants.USB_CLASS_VIDEO:
                return "USB class for video devices";
            case UsbConstants.USB_CLASS_WIRELESS_CONTROLLER:
                return "USB class for wireless controller devices";
            default:
                return "Unknown USB class!";

        }
    }

    private ParcelFileDescriptor getSeekableFileDescriptor(String filename) {
        ParcelFileDescriptor fd = null;
        try {
            fd = ParcelFileDescriptor.open(new File(filename), ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return fd;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //WIFI SUPPORT FUNCTIONS SECTION
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public class ConnectedThreadWIFI extends Thread {

        private final InputStreamReader mmInStream;
        private final OutputStreamWriter mmOutStream;

        private ConnectedThreadWIFI(Socket client) {

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                printwriter = new OutputStreamWriter(client.getOutputStream(), "ISO-8859-1");
                printreader = new InputStreamReader(client.getInputStream(), "ISO-8859-1");
            } catch (IOException e) {
                e.printStackTrace();        //4.0
            }

            mmInStream = printreader;
            mmOutStream = printwriter;
        }

        //As a first timer Android Application writer, I think I have done a less than
        //stellar job on this read thread.  I did not quite understand it and ran out
        //of time before I figured it out.  I did however get this to read status from the printer
        //which was my main goal.
        public void run() {
            char[] buffer = new char[1024];        // buffer store for the stream
            int bytes = 0;                            // bytes returned from read()
            int x = 0, y = 0;                        // used just to create a delay
            reading = true;

            // Keep listening to the InputStream until an exception occurs
            while (reading) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();        //4.0
                    break;
                }

                //Slight delay to allow incoming data to be handled.  Probably should be using
                //a postDelayed command here, but I ran out of time before I figure it out.
                //This delay loop is crude but works.  Should someone have a better way to do
                //this, please email the modified code and I will update the example code on the web site for everyone.
                //4.0 look at this
                //mHandler.postDelayed(mRunnable,100);
                for (x = 0; x < 100000; x++) {
                    y++;
                }
                y = 0;
            }
            //AppendStatus("Exiting Read Thread");
        }

        //Call this from the main Activity to send data to the remote device
        public void write(char[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();        //4.0
            }
        }

        //Call this from the main Activity to shutdown the connection
        public void cancel() {
            try {
                client.close();
                mHandler.sendEmptyMessage(DISCONNECTED);
            } catch (IOException e) {
                e.printStackTrace();        //4.0
            }
        }

    }


    private final BroadcastReceiver mWIFIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();

            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                //4.0 boolean isWifiConn = netInfo.isConnected();
                boolean isWifiConn = false;                     //4.0
                if (netInfo != null)                            //4.0
                    isWifiConn = netInfo.isConnected();         //4.0
                VerifyConnection = isWifiConn;
            }

            if (ConnectivityManager.EXTRA_NO_CONNECTIVITY.equals(action)) {
                Log.d("WifiReceiver", "EXTRA_NO_CONNECTIVITY");
            }

            if (ConnectivityManager.EXTRA_OTHER_NETWORK_INFO.equals(action)) {
                Log.d("WifiReceiver", "EXTRA_OTHER_NETWORK_INFO");
            }

            if (ConnectivityManager.EXTRA_REASON.equals(action)) {
                Log.d("WifiReceiver", "EXTRA_REASON");
            }

            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI)
                Log.d("WifiReceiver", "Have Wifi Connection");
            else
                Log.d("WifiReceiver", "Don't have Wifi Connection");
        }
    };


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //WiFi SDK SECTION
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
/*
    public void OpenSessionWIFI(final String ipAddress, Context context) {

        InitDotArray();
        mHandler = getHandler();

        IntentFilter filter1 = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        //IntentFilter filter2 = new IntentFilter(ConnectivityManager.EXTRA_NO_CONNECTIVITY);
        //IntentFilter filter3 = new IntentFilter(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);
        //IntentFilter filter4 = new IntentFilter(ConnectivityManager.EXTRA_REASON);

        context.registerReceiver(mWIFIReceiver, filter1);
        //context.registerReceiver(mWIFIReceiver, filter2);
        //context.registerReceiver(mWIFIReceiver, filter3);
        //context.registerReceiver(mWIFIReceiver, filter4);

        mContext = context;
        Mode = "WIFI";
        connecting = true;
        connectionStatus = false;

        connectThread = new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    socket = new Socket(ipAddress, portNumber);

                    if (!Thread.interrupted()){
                        mConnectedThreadWIFI = new ConnectedThreadWIFI(socket);
                        mConnectedThreadWIFI.start();
                        connectionStatus = true;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                connecting = false;
                verifyConnection = connectionStatus;
                if (!verifyConnection) Mode = "";
            }
        });
        connectThread.start();
    }
*/

    public boolean OpenSessionWIFI(String ipAddress, Context context)
    {
        InitDotArray();                             //used for positioning in portrait mode
        mHandler = getHandler();                    //4.0 get a message handler

        //register the WIFI broadcast receiver
        //WIFIConnect = PendingIntent.getBroadcast(context, 0, new Intent(ConnectivityManager.CONNECTIVITY_ACTION),0);
        //WIFIDisconnect = PendingIntent.getBroadcast(context, 0, new Intent(ConnectivityManager.EXTRA_NO_CONNECTIVITY),0);
        //WIFIOther = PendingIntent.getBroadcast(context, 0, new Intent(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO),0);
        //WIFIReason = PendingIntent.getBroadcast(context, 0, new Intent(ConnectivityManager.EXTRA_REASON),0);
        IntentFilter filter1 = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        IntentFilter filter2 = new IntentFilter(ConnectivityManager.EXTRA_NO_CONNECTIVITY);
        IntentFilter filter3 = new IntentFilter(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);
        IntentFilter filter4 = new IntentFilter(ConnectivityManager.EXTRA_REASON);

        context.registerReceiver(mWIFIReceiver, filter1);
        context.registerReceiver(mWIFIReceiver, filter2);
        context.registerReceiver(mWIFIReceiver, filter3);
        context.registerReceiver(mWIFIReceiver, filter4);

        Mode = "WIFI";                              //Set communication mode for Wi-Fi
        WifiProcessing = true;
        //establish client and open port 9100 based on IP Address
        Open_WIFI(ipAddress);

        //wait here while wifi attempting to connect. Wifi connection attempt runs in background
        //on a seperate thread
        while(WifiProcessing)
        {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        VerifyConnection = connectionStatus;

        //if connection failed clear Mode
        if(connectionStatus)
            mContext = context;
        else
            Mode = "";                              //Clear communication mode

        return (connectionStatus);
    }

    public void CloseSessionWIFI() {
        Close_WIFI();
        Mode = "";                              //Clear communication mode

    }

    public boolean VerifyConnectionWIFI()
    {
        return(VerifyConnection);       //4.0 simplified
    }

    //3.0 Added WIFI
    private void Open_WIFI(final String ipAddress) {

        connectionStatus = false;

        //Trigger Connecting message to set Wifi processing flag
        //mHandler.sendEmptyMessage(CONNECTING);
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    client = new Socket(ipAddress, portNumber);

                    if(client == null) {
                        //mHandler.sendEmptyMessage(CONNECTION_FAILED);
                        WifiProcessing = false;
                    }
                    else {
                        // Start the thread to manage the connection and perform transmissions
                        mConnectedThreadWIFI = new ConnectedThreadWIFI(client);
                        mConnectedThreadWIFI.start();
                        //mHandler.sendEmptyMessage(CONNECTION_SUCCESSFUL);
                        connectionStatus = true;
                        WifiProcessing = false;
                    }

                }

                catch (UnknownHostException e) {
                    e.printStackTrace();
                    //mHandler.sendEmptyMessage(CONNECTION_FAILED);
                    WifiProcessing = false;
                } catch (IOException e) {
                    e.printStackTrace();
                    //mHandler.sendEmptyMessage(CONNECTION_FAILED);
                    WifiProcessing = false;

                }
                WifiProcessing = false;
            }
        }).start();

        //4.0 return;
    }


    //3.0 Added WIFI
    private void Close_WIFI()
    {
        try {
            if(printwriter != null)
                printwriter.close();
            if(printreader != null)
                printreader.close();
            if(client != null)
                client.close();
            mHandler.sendEmptyMessage(DISCONNECTED);
        }

        catch (IOException e) {
            e.printStackTrace();
        }

    }

    //3.0 Added WIFI
    private void Write_WIFI(final String command)
    {
        //4.0 place TCP Client into background thread
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(printwriter != null)
                {
                    try
                    {
                        printwriter.write(command);
                        printwriter.flush();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    //3.0 Added WIFI
    //4.0 No longer pass character array.  Pass byte array and convert it here
    //4.0 See SendData above
    //4.0 private void Write_WIFI_Data(final char[] buf)
    private void Write_WIFI_Data(final byte[] buf, final boolean FF)              //4.0
    {
        //4.0 place TCP Client into background thread
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(printwriter != null)
                {
                    try {
                        //4.0 must now convert byte array to string using correct charset
                        //4.0 name and then convert string to char array
                        String text1 = new String(buf, "ISO-8859-1");  //"UTF-8");
                        if(FF)      //6.1
                        {
                            text1 = text1 + "<p>";
                        }
                        char[] chars = text1.toCharArray();
                        //4.0 printwriter.write(buf);
                        printwriter.write(chars);           //4.0
                        printwriter.flush();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    //3.0 Added WIFI
    private String Read_WIFI() {
        return (StatusReturned);
    }

    //3.0 Added WIFI
    private void ClearStatus_WIFI() {
        StatusReturned = "";
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //LOW LEVEL CONVERSION FUNCTIONS SECTION
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private boolean ImageToBmp(String url) {

        //ProcessedFF = true;          //do not let formfeed through until processing complete at the end of ParseBMP()

        //ImageView imageV = new ImageView(mContext);
        //Image image = new Image();
        Bitmap bitmap = null;
        boolean status = true;
        int BMP_WIDTH;
        int BMP_HEIGHT;
        int height = 0;
        int width = 0;

        try {
            selectedFileName = url;

            BitmapConvertor convertor;
            convertor = new BitmapConvertor(mContext);

            //Convert image file to Color Bitmap
            bitmap = BitmapFactory.decodeFile(url);
            //height = bitmap.getHeight();
            //width = bitmap.getWidth();

            //If scaling image to ticket size
            if (ImageScaled) {
                //if landscape mode, set up height and width
                if (PrinterOrientation.equals("<LM>")) {
                    BMP_WIDTH = (int) (PrinterResolution * StockWidth);
                    BMP_HEIGHT = (int) (PrinterResolution * StockHeight);
                } else       //if not landscape, then rotate to portrait mode, by reversing height and width
                {
                    BMP_WIDTH = (int) (PrinterResolution * StockHeight);
                    BMP_HEIGHT = (int) (PrinterResolution * StockWidth);
                }
                bitmap = getResizedBitmap(bitmap, BMP_WIDTH, BMP_HEIGHT);       //this scales the image

            }
            /*else        //no scaling.  Print image actual size.
            {
                //if landscape mode, set up height and width
                if (PrinterOrientation.equals("<LM>"))
                {
                    BMP_WIDTH = width;
                    BMP_HEIGHT = height;
                }
                else       //if not landscape, then rotate to portrait mode, by reversing height and width
                {
                    BMP_WIDTH = height;
                    BMP_HEIGHT = width;
                }
            }*/

            //make monochrome bitmap from color bitmap and send to printer
            //convertor.convertBitmap(bitmap);    //,BMP_WIDTH, BMP_HEIGHT);          //Use this later

            //convertor.convertImageBitmap(bitmap,BMP_HEIGHT,BMP_WIDTH);             //@mdh test with this now

            PrintBMP(bitmap,bitmap.getWidth(),bitmap.getHeight());
            //PrintBMP(bitmap, BMP_WIDTH, BMP_HEIGHT);
            //convertor.convertBitmap(bitmap);                                //Convert in background
        } catch (Exception e) {
            status = false;
            e.printStackTrace();
        }

        return (status);

    }

    private boolean PdfToBmp(final String url)
    {
        boolean status = true;

        //new Thread(new Runnable()
        //{

            //@Override
            //public void run()
            //{
                //ProcessedFF = true;          //do not let formfeed through until processing complete
                BitmapConvertor convertor;
                convertor = new BitmapConvertor(mContext);

                Bitmap bitmap = null;
                int BMP_WIDTH;
                int BMP_HEIGHT;
                //int height = 0;
                //int width = 0;

                selectedFileName = url;

                //PDF create a new renderer
                try {
                    PdfRenderer renderer = new PdfRenderer(getSeekableFileDescriptor(url));
                    //let us just render all pages
                    final int pageCount = renderer.getPageCount();
                    for (int i = 0; i < pageCount; i++) {
                        Page page = renderer.openPage(i);
                        BMP_HEIGHT = page.getHeight();
                        BMP_WIDTH = page.getWidth();

                        bitmap = Bitmap.createBitmap(BMP_WIDTH, BMP_HEIGHT, Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawColor(Color.WHITE);
                        canvas.drawBitmap(bitmap, 0, 0, null);

                        if (ImageScaled) {
                            //if landscape mode, set up height and width
                            if (PrinterOrientation.equals("<LM>")) {
                                BMP_WIDTH = (int) (PrinterResolution * StockWidth);
                                BMP_HEIGHT = (int) (PrinterResolution * StockHeight);
                            } else       //if not landscape, then rotate to portrait mode, by reversing height and width
                            {
                                BMP_WIDTH = (int) (PrinterResolution * StockHeight);
                                BMP_HEIGHT = (int) (PrinterResolution * StockWidth);
                            }

                        } else {
                            if (PrinterOrientation.equals("<LM>")) {
                                BMP_HEIGHT = (page.getHeight() / 72) * PrinterResolution;
                                BMP_WIDTH = (page.getWidth() / 72) * PrinterResolution;
                            } else        //else if Portrait mode
                            {
                                BMP_HEIGHT = (page.getWidth() / 72) * PrinterResolution;
                                BMP_WIDTH = (page.getHeight() / 72) * PrinterResolution;
                            }

                        }

                        bitmap = getResizedBitmap(bitmap, BMP_WIDTH, BMP_HEIGHT);       //this scales the image

                        //say we render for showing on the screen
                        page.render(bitmap, null, null, Page.RENDER_MODE_FOR_PRINT);
                        //page.render(bitmap, null, null, Page.RENDER_MODE_FOR_DISPLAY);
                        //do stuff with the bitmap
                        PrintBMP(bitmap,bitmap.getWidth(),bitmap.getHeight());

                        //PrintBMP(bitmap, BMP_WIDTH, BMP_HEIGHT);
                        //convertor.convertBitmap(bitmap);                                //Convert in background

                        // close the page
                        page.close();
                    }

                    //close the renderer
                    renderer.close();
                }
                catch (IOException e)
                {
                    //status = false;
                    e.printStackTrace();
                }

            //}
        //}).start();

        return (status);
    }

    //This routine will read a monochrome BMP file, parse it and convert it to Boca FGL graphics commands.
    //This routine will also remove as much blank spaces as possible so that there is less to tranmit via usb/wifi/bluetooth to the printer.
    private void ParseBMP(byte[] buffer) {

        String command = "";

        int i = 0, i1 = 0, i2 = 0, i3 = 0, i4 = 0;
        int j = 0, len1 = 0, len2 = 0;
        int m = 0, n = 0, x = 0, y = 0;
        int line_total = 0, start_data = 0, seg_count = 0;
        int k, rc_position = 0, start_command = 0;

        int charx = 0;
        int charynew = 0;
        int charyorg = 0;
        int chartmp = 0, chartmp1 = 0, chartmp2 = 0, chartmp3 = 0;

        int bitmaskx[] = new int[8];
        bitmaskx[0] = 1;
        bitmaskx[1] = 2;
        bitmaskx[2] = 4;
        bitmaskx[3] = 8;
        bitmaskx[4] = 16;
        bitmaskx[5] = 32;
        bitmaskx[6] = 64;
        bitmaskx[7] = 128;

        int LINEMAPMAX = 2500;
        int DATA = 1;
        int ZEROS = 0;

        int bitplacex = 0;
        int bitcounter = 0;

        int IMAGE_MAX_WIDTH = 0x07ffffff;                  //2^27-1 to allow for 15 channel data
        int IMAGE_MAX_HEIGHT = 0x7fffffff;                 //2^31-1

        double DataSize = 0;
        int RowSize = 0;
        int CompRowSize = 0;
        int PixelArraySize = 0;
        int NibblesToIgnore = 0;
        int BytesToIgnore = 0;
        int arraypointer = 0;
        int rx, ry, rg;
        boolean success;
        String strx = "";
        String stry = "";
        String strg = "";

        LineParser[] linemap = new LineParser[LINEMAPMAX];
        FileHeader bmfile = new FileHeader();
        InfoHeader bminfo = new InfoHeader();

        //move bitmap file to array
        //const uint8_t *buffer = (const uint8_t*)[myData bytes];

        //There are three major steps to this process.
        // 1. Read the Bit map file header to gather information about the file and the image contained within that file
        // 2. The first pass through the raster data allows for the conversion of the bmp raster data into Boca Systems FGL
        //    text commands and all Boca Graphics.
        // 3. The second pass will be performed to remove large blocks of blank data and modify the row/column commands
        //    generated in the first pass.

        //Step 1 - parse bitmap file header
        bmfile.bfType0 = (char) buffer[0];                         //B
        bmfile.bfType1 = (char) buffer[1];                         //M

        bmfile.bfSize = (int) (UnsignedInt(buffer[2]));                        //Next four Bytes represent the file size
        bmfile.bfSize |= UnsignedInt(buffer[3]) << 8;
        bmfile.bfSize |= UnsignedInt(buffer[4]) << 16;
        bmfile.bfSize |= UnsignedInt(buffer[5]) << 24;

        bmfile.bfR1 = (short) (UnsignedInt(buffer[6]));                            //Next two Bytes represent Reserved #1
        bmfile.bfR1 |= UnsignedInt(buffer[7]) << 8;

        bmfile.bfR2 = (short) (UnsignedInt(buffer[8]));                            //Next two Bytes represent Reserved #2
        bmfile.bfR2 |= UnsignedInt(buffer[9]) << 8;

        bmfile.bfOffBits = (int) (UnsignedInt(buffer[10]));                      //Next four Bytes represent the file byte offset
        bmfile.bfOffBits |= UnsignedInt(buffer[11]) << 8;
        bmfile.bfOffBits |= UnsignedInt(buffer[12]) << 16;
        bmfile.bfOffBits |= UnsignedInt(buffer[13]) << 24;

        //parse bitmap image header
        bminfo.biSize = (int) (UnsignedInt(buffer[14]));                         //Next four Bytes represent the DIB Header size
        bminfo.biSize |= UnsignedInt(buffer[15]) << 8;
        bminfo.biSize |= UnsignedInt(buffer[16]) << 16;
        bminfo.biSize |= UnsignedInt(buffer[17]) << 24;

        bminfo.biWidth = (int) (UnsignedInt(buffer[18]));                        //Next four Bytes represent the image width
        bminfo.biWidth |= UnsignedInt(buffer[19]) << 8;
        bminfo.biWidth |= UnsignedInt(buffer[20]) << 16;
        bminfo.biWidth |= UnsignedInt(buffer[21]) << 24;

        bminfo.biHeight = (int) (UnsignedInt(buffer[22]));                       //Next four Bytes represent the image height
        bminfo.biHeight |= UnsignedInt(buffer[23]) << 8;
        bminfo.biHeight |= UnsignedInt(buffer[24]) << 16;
        bminfo.biHeight |= UnsignedInt(buffer[25]) << 24;

        bminfo.biPlanes = (short) (UnsignedInt(buffer[26]));                       //Next two Bytes represent the # of planes
        bminfo.biPlanes |= UnsignedInt(buffer[27]) << 8;

        bminfo.biBitCount = (short) (UnsignedInt(buffer[28]));                     //Next two Bytes represent the # of bits per pixel
        bminfo.biBitCount |= UnsignedInt(buffer[29]) << 8;

        bminfo.biCompression = (int) (UnsignedInt(buffer[30]));                  //Next four Bytes represent the compression style
        bminfo.biCompression |= UnsignedInt(buffer[31]) << 8;
        bminfo.biCompression |= UnsignedInt(buffer[32]) << 16;
        bminfo.biCompression |= UnsignedInt(buffer[33]) << 24;

        bminfo.biSizeImage = (int) (UnsignedInt(buffer[34]));                    //Next four Bytes represent the image size
        bminfo.biSizeImage |= UnsignedInt(buffer[35]) << 8;
        bminfo.biSizeImage |= UnsignedInt(buffer[36]) << 16;
        bminfo.biSizeImage |= UnsignedInt(buffer[37]) << 24;

        bminfo.biXPPMeter = (int) (UnsignedInt(buffer[38]));                     //Next four Bytes represent the X pixels/meter
        bminfo.biXPPMeter |= UnsignedInt(buffer[39]) << 8;
        bminfo.biXPPMeter |= UnsignedInt(buffer[40]) << 16;
        bminfo.biXPPMeter |= UnsignedInt(buffer[41]) << 24;

        bminfo.biYPPMeter = (int) (UnsignedInt(buffer[42]));                     //Next four Bytes represent the Y pixels/meter
        bminfo.biYPPMeter |= UnsignedInt(buffer[43]) << 8;
        bminfo.biYPPMeter |= UnsignedInt(buffer[44]) << 16;
        bminfo.biYPPMeter |= UnsignedInt(buffer[45]) << 24;

        bminfo.biClrUsed = (int) (UnsignedInt(buffer[46]));                      //Next four Bytes represent the colors in color table
        bminfo.biClrUsed |= UnsignedInt(buffer[47]) << 8;
        bminfo.biClrUsed |= UnsignedInt(buffer[48]) << 16;
        bminfo.biClrUsed |= UnsignedInt(buffer[49]) << 24;

        bminfo.biClrImportant = (int) (UnsignedInt(buffer[50]));                 //Next four Bytes represent the important color count
        bminfo.biClrImportant |= UnsignedInt(buffer[51]) << 8;
        bminfo.biClrImportant |= UnsignedInt(buffer[52]) << 16;
        bminfo.biClrImportant |= UnsignedInt(buffer[53]) << 24;

        bminfo.biBlackBitMask = (int) (UnsignedInt(buffer[54]));                 //Next four Bytes represent the black channel bit mask
        bminfo.biBlackBitMask |= UnsignedInt(buffer[55]) << 8;
        bminfo.biBlackBitMask |= UnsignedInt(buffer[56]) << 16;
        bminfo.biBlackBitMask |= UnsignedInt(buffer[57]) << 24;

        bminfo.biWhiteBitMask = (int) (UnsignedInt(buffer[58]));                 //Next four Bytes represent the white channel bit mask
        bminfo.biWhiteBitMask |= UnsignedInt(buffer[59]) << 8;
        bminfo.biWhiteBitMask |= UnsignedInt(buffer[60]) << 16;
        bminfo.biWhiteBitMask |= UnsignedInt(buffer[61]) << 24;


        long Height = UnsignedInt(bminfo.biHeight);
        long Width = UnsignedInt(bminfo.biWidth);
        long BitCount = UnsignedShort(bminfo.biBitCount);
        //error check before proceeding.  Is image width, height and depth within range
        if (((Width > 0) && (Width <= IMAGE_MAX_WIDTH)) &&
                ((Height > 0) && (Height <= IMAGE_MAX_HEIGHT)) &&
                (BitCount == 1)) {
            //check color used.  If color used zero or one, set to two
            if ((bminfo.biClrUsed == 0) || (bminfo.biClrUsed == 1)) {
                bminfo.biClrUsed = 2;
            }
            //monochrome file will have two colors black and white
            if (bminfo.biClrUsed == 2) {
                //check resolution. if pixel per inch read as zero, set to 128
                if ((bminfo.biXPPMeter == 0) || (bminfo.biYPPMeter == 0)) {
                    bminfo.biXPPMeter = 128;
                    bminfo.biYPPMeter = 128;
                }

                //DataSize = ((bminfo.biBitCount * bminfo.biWidth) / 32.0) * 4.0;
                //RowSize = (((bminfo.biBitCount * bminfo.biWidth)  + 31) / 32) * 4;
                //PixelArraySize = bminfo.biWidth * bminfo.biHeight;
                DataSize = (int) (((BitCount * Width) / 32.0) * 4.0);
                RowSize = (int) ((((BitCount * Width) + 31) / 32) * 4);
                PixelArraySize = (int) (Width * Height);
                NibblesToIgnore = (int) ((RowSize - DataSize) / 0.5);                   //this will range from 0 to 7
                BytesToIgnore = NibblesToIgnore / 2;

                //define an array to contain raster data now that the amount of data has been established
                byte bit_image[] = new byte[PixelArraySize];
                for (i = 0; i < PixelArraySize; i++)                                    //zero out BIT IMAGE ARRAY
                    bit_image[i] = 0;

                CompRowSize = RowSize * 8;
                char CompBuffer[] = new char[CompRowSize];
                for (i = 0; i < CompRowSize; i++)                                       //zero out composite buffer
                    CompBuffer[i] = 0;

                //Step 2 - First pass to convert raster data to Boca graphics
                //long bmpcnt = buffer.length - 1;                               //start at the end and read left to right per row
                int bmpcnt = buffer.length - 1;                               //start at the end and read left to right per row
                arraypointer = CompRowSize - 1;

                //if logo number does not exist then print the image else download graphic as a logo
                if (LogoNumber.equals("")) {
                    //complete header command to include path, orientation, no repeat and original row column position
                    command = PrinterPath + PrinterOrientation + "<RE0><DI><RC" + originalx + "," + originaly + ">";
                } else        //download graphic as logo
                {
                    //When saving a logo row,column should be 0,0
                    originalx = 0;
                    originaly = 0;
                    byte esc[] = new byte[1];
                    esc[0] = 0x1b;

                    //Send ID number and an escape to trigger download.  Logo number should be between 1 and 1000
                    //include ID number and escape control character
                    command = "<RE0><DI><RC0,0><ID" + LogoNumber + ">";       // + 0x001b;
                    SendString(command);
                    SendData(esc, false);   //6.1 Added FF boolean for WIFI
                    command = "";
                }

                j = 0;                                                                  //initialize bit image index counter
                rx = originalx;     //0;                                                                 //original x coordinate test
                ry = originaly;     //0;                                                                 //original y coordinate test

                //put command string into bit image character array
                for (i = 0; i < command.length(); i++)
                    bit_image[j++] = (byte) (command.charAt(i) & 0xff);                  //unsigned byte

                //Loop through raster data row by row
                //                for (y = 0; y < bminfo.biHeight; y++)

                for (y = 0; y < Height; y++) {

                    for (x = 0; x < RowSize; x++) {
                        charx = (~buffer[bmpcnt--]) & 0xff;                     //get the char and invert it

                        if (x < BytesToIgnore) {
                            charx = 0;
                            NibblesToIgnore -= 2;
                        } else {
                            if (NibblesToIgnore > 0) {
                                charx = (charx & 0xf0);
                                NibblesToIgnore = 0;
                            }
                        }

                        //we start bit shifting: the highest bit first, which we will send to the bit zero,
                        //then we will put it in the right position to be place in the newchar
                        bitplacex = 0;
                        while (bitplacex <= 7) {
                            chartmp = charx;                                            // bit operations are destructive, so make sure you get the char

                            // make sure we only have the bit we want by masking the other ones so we only have one bit value left. In the case of the highest bit
                            chartmp1 = (chartmp & bitmaskx[bitplacex]);

                            // (bit 7) the bitmask will be 128, the lowest bit will have bitmask 1 etc.
                            // put the requested bit on the same place as is requested by the line: first we shift it to bitplace zero: if we are on first col, shift bit 7 to position 0,
                            // on the second col shift bit 6 to position 0 etc. As bitplace x starts counting from seven to zero, we can use that one.
                            chartmp2 = (chartmp1 >> (bitplacex));

                            // now shift the bit to the place we want it in the new char: we can derive that from the bitcounter. Line 0 is bitcounter 0, which is the highest bit in the char:
                            // So we shift for line zero 7, for line 1 6 positions, until line 7, where we shift 0 positions: that works out as (7-bitcounter).
                            // as the other bits are allready 0, and the new bit are too, we now have effectively created a bitmask.
                            chartmp3 = (chartmp2 << (7 - bitcounter));

                            charyorg = CompBuffer[arraypointer];                  // get the original char

                            // overlay the new byte with the original and the bitmask created by the shifted char: this is done by an bit OR:
                            //the original bits stay intact
                            charynew = (charyorg | chartmp3);

                            //if ((charynew == '\x03') || (charynew == '\x07') || (charynew == '\xff'))
                            //if (charynew == '\xff')
                            //{
                            //NSLog(@"Test");
                            //}
                            //CompBuffer[arraypointer] = [NSString stringWithUTF8String:charynew];
                            CompBuffer[arraypointer] = (char) charynew;

                            // increment bitplace with 1, and decrement the arraypointer
                            bitplacex++;
                            arraypointer--;
                        }

                    }
                    // add to the bitcounter
                    bitcounter++;

                    // every line we set the counter for the output buffer
                    arraypointer = CompRowSize - 1;

                    // if it is end of the line, we start printing
                    if (((y + 1) % 8) == 0 && y > 0) {

                        bitcounter = 0;

                        //build output array with <RC#,#> and <G#> graphics data
                        command = "<G" + CompRowSize + ">";

                        for (i = 0; i < command.length(); i++)                              //zero out BIT IMAGE ARRAY
                        {
                            bit_image[j++] = (byte) (command.charAt(i) & 0xff);              //unsigned byte
                        }

                        for (k = 0; k < CompRowSize; k++) {
                            bit_image[j++] = (byte) (CompBuffer[k] & 0xff);                  //unsigned byte
                        }

                        if (PrinterOrientation.equals("<LM>")) {
                            rx = originalx + y + 1;
                            ry = originaly;      //0;
                        } else {
                            ry = originaly + y + 1;
                            rx = portrait_dots - originalx;     //0;
                        }
                        command = "<RC" + rx + "," + ry + ">";

                        rc_position = j;
                        for (k = 0; k < command.length(); k++) {
                            bit_image[j++] = (byte) (command.charAt(k) & 0xff);                  //unsigned byte
                        }

                        // clean the buffer
                        // memset(CompBuffer, 0, BytesPerLine * 8);
                        for (k = 0; k < CompRowSize; k++)         //zero out composite buffer
                            CompBuffer[k] = 0;

                    }
                }
                //The last RC command written is not needed so back up insert form feed and zero fill the rest
                for (k = rc_position; k < j; k++)
                    bit_image[k] = 0;
                j = rc_position;

                //Step 3 - Second pass through what is now Boca commands and graphics.  This is to improve effiecency by removing large blocks of blank data to make
                //         for smaller amounts of data to transmit via Wi-Fi or bluetooth.
                byte bit_image_refined[] = new byte[j];
                for (k = 0; k < j; k++)                            //zero out BIT IMAGE ARRAY
                    bit_image_refined[k] = 0;

                m = 0;

                //complete header command to include path, orientation and no repeat
                //if logo number does not exist then print the image else download graphic as a logo
                if (LogoNumber.equals("")) {
                    //complete header command to include path, orientation, no repeat and original row column position
                    command = PrinterPath + PrinterOrientation + "<RE0><DI>";
                } else        //download graphic as logo
                {
                    //When saving a logo row,column should be 0,0
                    originalx = 0;
                    originaly = 0;

                    //Send ID number and an escape to trigger download.  Logo number should be between 1 and 1000
                    //command = @"<RE0><DI><RC0,0><";
                    //command = [NSString stringWithFormat:@"@%@",command,LogoNumber];       //include ID number
                    //command = [NSString stringWithFormat:@"@%@",command,@">"];];
                    //command = [NSString stringWithFormat:@"@%C",command,0x001b];           //include escape control character
                    //SendString(command);
                    //command = @"";
                }

                //put command string into refined bit image character array
                for (k = 0; k < command.length(); k++)
                    bit_image_refined[m++] = (byte) (command.charAt(k) & 0xff);                  //unsigned byte

                for (k = 0; k < j; k++) {
                    success = false;
                    //find <RC#,#>
                    if ((bit_image[k] == '<') && (bit_image[k + 1] == 'R') && (bit_image[k + 2] == 'C')) {
                        start_command = k;              //save this position, if needed below
                        i2 = 0;
                        i3 = k + 3;                     //start right after <RC and look for >
                        while (bit_image[i3] != '>') {
                            if (bit_image[i3] == ',')    //if a comma is found along the way...
                                i2 = i3;
                            i3++;
                        }

                        len1 = i2 - (k + 3);
                        //copy len1 # of characters from bit image array and build an NSString
                        //string x value
                        for (i = 0; i < len1; i++) {
                            command = String.valueOf(Character.toString((char) bit_image[k + 3 + i]));
                            //command = String.valueOf(bit_image[k + 3 + i]);
                            //command = bit_image[k + 3 + i];
                            if (i == 0)
                                strx = command;
                            else
                                strx = strx + command;

                        }

                        len1 = i3 - i2 - 1;
                        //copy len1 # of characters from bit image array and build an NSString
                        //string y value
                        for (i = 0; i < len1; i++) {
                            command = String.valueOf(Character.toString((char) bit_image[i2 + 1 + i]));
                            if (i == 0)
                                stry = command;
                            else
                                stry = stry + command;

                        }

                        success = true;

                    }

                    //if RC command above was found this time thru the loop
                    if (success) {
                        //find <G#>
                        i1 = i3 + 1;
                        i2 = i3 + 2;
                        i3 = i2 + 1;
                        if ((bit_image[i1] == '<') && (bit_image[i2] == 'G')) {
                            while (bit_image[i3] != '>') {
                                i3++;
                            }
                            start_data = i3 + 1;                                            //mark starting position of graphic data
                            len1 = (i3 - i2) - 1;
                            //copy len1 # of characters from bit image array and build an NSString
                            //string G value
                            for (i = 0; i < len1; i++) {
                                command = String.valueOf(Character.toString((char) bit_image[i2 + 1 + i]));
                                if (i == 0)
                                    strg = command;
                                else
                                    strg = strg + command;

                            }
                        }

                        //Count leading, middle and trailing zeros
                        i1 = i3 + 1;                                                        //position index pointer to start of data
                        i2 = Integer.valueOf(strg);                                         //set counter to data count G#, so we can count down to zero
                        len2 = i2;                                                          //save total count
                        i3 = 0;                                                             //set counter to 0
                        i4 = 0;                                                             //set data counter to 0
                        seg_count = 0;                                                      //init segment counter to zero

                        for (i = 0; i < LINEMAPMAX; i++) {
                            linemap[i] = new LineParser();
                            linemap[i].start = 0;                                           //zero out all positions
                            linemap[i].count = 0;                                           //zero out all counters
                            linemap[i].block_type = -1;
                        }

                        while (i2 > 0)                                                      //search to end of line
                        {
                            if (seg_count == (LINEMAPMAX - 2))
                                i = 0;
                            if (i1 == (PixelArraySize - 2))
                                i = 0;

                            if (bit_image[i1] == 0)                                         //leading or middle zero found
                            {
                                if (i4 > 0)                                                 //found zeros but some data was already found
                                {
                                    linemap[seg_count].count = i4;                          //save data counter for that segment
                                    seg_count++;                                            //increment segment counter
                                    i4 = 0;                                                 //reset data counter
                                }
                                if (i3 == 0)                                                //if first zero found in this segment
                                {
                                    linemap[seg_count].start = i1;                          //mark where zeros start
                                    linemap[seg_count].block_type = ZEROS;                  //set type to zeros
                                }
                                i3++;                                                       //increment segment zero counter
                            } else                                                            //graphic data found, so mark position
                            {
                                if (i3 > 0)                                                 //found data but some zeros were already found
                                {
                                    linemap[seg_count].count = i3;                          //save zero counter for that segment
                                    seg_count++;                                            //increment segment counter
                                    i3 = 0;                                                 //reset zero counter
                                }
                                if (i4 == 0) {
                                    linemap[seg_count].start = i1;                          //mark where data starts
                                    linemap[seg_count].block_type = DATA;                   //set type to data
                                }
                                i4++;                                                       //increment segment data counter
                            }
                            i1++;                                                           //increment index pointer to next byte of data
                            i2--;                                                           //decrement G# counter

                        }
                        if (i3 > 0)                                                         //reached end of line but some trailing zeros were found
                        {
                            linemap[seg_count].count = i3;                                  //save zero counter for that segment
                            seg_count++;                                                    //increment segment counter
                            i3 = 0;                                                         //reset zero counter
                        }
                        if (i4 > 0)                                                         //reached end of line but some trailing data was found
                        {
                            linemap[seg_count].count = i4;                                  //save data counter for that segment
                            seg_count++;                                                    //increment segment counter
                            i4 = 0;                                                         //reset data counter
                        }


                        //check line results
                        //If seg1 > 1 then multiple segments so zeros and data were both found
                        if (seg_count > 1) {

                            line_total = 0;
                            i = 0;
                            rg = 0;
                            boolean command_ready = false;
                            //look at each segment of zeros to determine if more than 19 bytes of zeros were found
                            //which is the break even point to make it worth breaking one command into two or more
                            while (i < seg_count) {
                                if (linemap[i].block_type == DATA)        //looking at data block
                                {
                                    //if new command, then start it by setting row/column
                                    if (rg == 0) {
                                        command_ready = false;
                                        i1 = linemap[i].start;              //mark starting position
                                        //calc new row/column and graphics command
                                        if (PrinterOrientation.equals("<LM>")) {
                                            if (rx == Integer.valueOf(strx)) {
                                                //ry = i3 + line_total;
                                                ry = i3 + line_total + originaly;    //@mdh test

                                            } else {
                                                ry = i3;
                                                //ry = i3 + originaly;    //@mdh test
                                                line_total = 0;
                                            }
                                            rx = Integer.valueOf(strx);
                                            if (ry == 0)                //@mdh test
                                                ry = originaly;
                                        } else {
                                            if (ry == Integer.valueOf(stry)) {
                                                //portrait dots value is used here to correct positioning when rotating into portrait mode
                                                //rx = portrait_dots - (i3 + line_total);
                                                rx = (portrait_dots) - (i3 + line_total + originalx);     //@mdh test
                                            } else {
                                                //portrait dots value is used here to correct positioning when rotating into portrait mode
                                                //rx = portrait_dots - i3;
                                                rx = portrait_dots - (i3 + originalx);     //@mdh test
                                                line_total = 0;
                                            }
                                            ry = Integer.valueOf(stry);
                                            if (rx == portrait_dots)                //@mdh test
                                                rx = portrait_dots - originalx;
                                        }

                                    }
                                    rg += linemap[i].count;
                                } else                                                            //looking at zero block
                                {
                                    if (linemap[i].count < 19)                                  //if # of zeros in block < 19 include in current command
                                    {
                                        //if new command, then start it by setting row/column
                                        if (rg == 0) {
                                            command_ready = false;
                                            i1 = linemap[i].start;              //mark starting position
                                            //calc new row/column and graphics command
                                            if (PrinterOrientation.equals("<LM>")) {
                                                if (rx == Integer.valueOf(strx)) {
                                                    //ry = i3 + line_total;
                                                    ry = i3 + line_total + originaly;    //@mdh test
                                                } else {
                                                    ry = i3;
                                                    //ry = i3 + originaly;    //@mdh test

                                                    line_total = 0;
                                                }
                                                rx = Integer.valueOf(strx);
                                                if (ry == 0)                //@mdh test
                                                    ry = originaly;
                                            } else {
                                                if (ry == Integer.valueOf(stry)) {
                                                    //portrait dots value is used here to correct positioning when rotating into portrait mode
                                                    //rx = portrait_dots - (i3 + line_total);
                                                    rx = (portrait_dots) - (i3 + line_total + originalx);     //@mdh test
                                                } else {
                                                    //portrait dots value is used here to correct positioning when rotating into portrait mode
                                                    //rx = portrait_dots - i3;
                                                    rx = portrait_dots - (i3 + originalx);     //@mdh test

                                                    line_total = 0;
                                                }
                                                ry = Integer.valueOf(stry);
                                                if (rx == portrait_dots)                //@mdh test
                                                    rx = portrait_dots - originalx;
                                            }
                                        }
                                        if (i + 1 < seg_count)                                  //if not trailing spaces
                                            rg += linemap[i].count;
                                    } else {
                                        if (rg > 0)
                                            command_ready = true;
                                        if (i + 1 < seg_count)                                     //if not trailing spaces
                                            i3 += linemap[i].count;                             //increment past the spaces
                                    }
                                }
                                //While looping thru the line segments of one line, output command if ready
                                if (command_ready) {

                                    //build command string and then insert command into refined array
                                    command = "<RC" + rx + "," + ry + "><G" + rg + ">";

                                    for (n = 0; n < command.length(); n++)
                                        bit_image_refined[m++] = (byte) (command.charAt(n) & 0xff);                  //unsigned byte

                                    for (n = 0; n < rg; n++)
                                        bit_image_refined[m++] = bit_image[i1 + n];             //move graphic bytes

                                    command_ready = false;
                                    line_total += rg;
                                    rg = 0;
                                }
                                i++;
                            }

                            if (rg > 0)          //we have reached the end of line and a command needs to be completed
                            {

                                //build command string and then insert command into refined array
                                command = "<RC" + rx + "," + ry + "><G" + rg + ">";

                                for (n = 0; n < command.length(); n++)
                                    bit_image_refined[m++] = (byte) (command.charAt(n) & 0xff);                  //unsigned byte

                                for (n = 0; n < rg; n++)
                                    bit_image_refined[m++] = bit_image[i1 + n];                 //move graphic bytes
                            }
                        } else        //just one segment
                        {
                            //if (linemap[seg_count].block_type == DATA)                          //no zeros found, all data found so write line as is
                            //V4.0 test for segcount - 1
                            if (linemap[seg_count - 1].block_type == DATA)                          //no zeros found, all data found so write line as is
                            {
                                rg = Integer.valueOf(strg);
                                int total_command_length = ((start_data - start_command) + rg) - 1;
                                //start_command
                                for (n = 0; n <= total_command_length; n++)
                                    bit_image_refined[m++] = bit_image[start_command + n];      //move graphic bytes

                            }
                        }
                    }
                }

                SendData(bit_image_refined, true);  //6.1 Added FF boolean for WIFI
                //if logo number is not blank, send an escape to end download
                if (LogoNumber != "") {
                    byte esc[] = new byte[1];
                    esc[0] = 0x1b;
                    SendData(esc, false);   //6.1 Added FF boolean for WIFI
                    LogoNumber = "";
                }


            } else {
                //file must be monochrome
                //UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Alert" message:@"Error - BMP file must be monochrome." delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
                //[alert show];

            }
        } else {
            //invalid size
            //UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"Alert" message:@"Error - Invalid Image width/height/depth." delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
            //[alert show];

        }
    }

    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    private void PrintBMP(Bitmap image, int width, int height) {
        if (ImageDithered)
            CreateTransformedImage(image, width, height);
        else
            ConvertToMonochrome(image, width, height);

        CreateRawMonochromeData();
        populate_bmpheader();

        //In an attempt to reduce the ammount of data transmitted to the printer via Wi-Fi or Bluetooth, which will speed things up, I wrote another routine
        //named ParseBMP which will convert the data to FGL graphics and then parse the data for large blocks of "WHITE" space.  Remember the Boca Systems printers
        //only print black. When we refer to monochrome B&W BMP images, we really mean just black.  By removing the blocks of white graphical data from the stream,
        //less erroneous data is transmitted across relatively slow ports such as Wi-Fi or Bluetooth which speeds up the process, proportionally.
        byte[] Test;
        Test = new byte[nRead + 62];
        //System.arraycopy();
        for (int i = 0; i < 62; i++) {
            Test[i] = BMPHeader[i];                             //move the BMP header information to the byte array
        }
        for (int i = 0; i < nRead; i++) {
            Test[i + 62] = ToByte(mRawBitmapData[i]);           //move the monochrome BMP graphical data to the same byte array
        }
        ParseBMP(Test);                                         //remove some white space and send to printer

    }

    private void CreateTransformedImage(Bitmap image, int width, int height) {
        int row, col;
        int index;              //@mdh maybe a long???
        ArgbColor transformed;
        ArgbColor[] OriginalData;

        ArgbColor current;      // = new ArgbColor();
        // ArgbColor RGBA = new ArgbColor();

        //int testH = image.getHeight();
        // int testW = image.getWidth();
        // int[] RawData = new int[height * width];
        //image.getPixels(RawData,0,testW,0,0,width,height);

        OriginalData = new ArgbColor[height * width];

        //TransformToRGBA(image, OriginalData, width, height);
        //Transform integer pixel array of data to RGBA data
        for (row = 0; row < height; row++) {
            for (col = 0; col < width; col++) {

                //get RGBA pixel data based on width and height
                int colour = image.getPixel(col, row) & 0x0000ffff;

                index = (row * width) + col;

                OriginalData[index] = new ArgbColor();

                OriginalData[index].Red = Color.red(colour);
                OriginalData[index].Blue = Color.blue(colour);
                OriginalData[index].Green = Color.green(colour);
                OriginalData[index].Alpha = Color.alpha(colour);
/*
                if(RawData[index] != -1)
                    RGBA.Alpha = Color.alpha(RawData[index]);

                RGBA.Alpha = Color.alpha(RawData[index]);
                RGBA.Red = Color.red(RawData[index]);
                RGBA.Blue = Color.blue(RawData[index]);
                RGBA.Green = Color.green(RawData[index]);
                OriginalData[index] = RGBA;
*/
            }
        }

        for (row = 0; row < height; row++) {

            for (col = 0; col < width; col++) {
                index = (row * width) + col;
                //Transform the pixel to monochrome
                current = OriginalData[index];
                transformed = TransformPixel(current);
                OriginalData[index] = transformed;

                //Apply Floyd Steinberg dithering alogorithm
                DitherFloydSteinberg(OriginalData, current, transformed, col, row, width, height);
            }
        }

        //Return data to bitmap format
        mWidth = width;
        mHeight = height;
        mDataWidth = ((mWidth + 31) / 32) * 4 * 8;
        mDataArrayLength = mDataWidth * mHeight;
        //mDataArray = new byte[mDataArrayLength];
        //mRawBitmapData = new byte[(mDataWidth * mHeight)/8];
        mDataArray = new int[mDataArrayLength];
        mRawBitmapData = new int[(mDataWidth * mHeight) / 8];
        //Put Monochrome data into mDataArray
        MakeItMonochrome(OriginalData, width, height);
    }

    //Convert color data to RGBA.  Do not dither. Then convert to monochrome based on Dither Threshold
    private void ConvertToMonochrome(Bitmap image, int width, int height) {
        int Monochrome, k;
        mWidth = width;
        mHeight = height;
        k = 0;

        //int[] pixels;
        //pixels = new int[mHeight * mWidth];     //@mdh needs to be unsigned int
        // 2.
        mDataWidth = ((mWidth + 31) / 32) * 4 * 8;
        mDataArrayLength = mDataWidth * mHeight;
        mDataArray = new int[mDataArrayLength];
        mRawBitmapData = new int[(mDataWidth * mHeight) / 8];
        //mDataArray = new byte[mDataArrayLength];
        //mRawBitmapData = new byte[(mDataWidth * mHeight)/8];

        //int currentPixel = pixels;      //@mdh needs to be unsigned int
        for (int y = mHeight - 1; y >= 0; y--) {

            for (int x = 0; x < mWidth; x++) {
                // 3.
                //process each row of data starting with the last row
                //int color = *(currentPixel + (j*mWidth) + i);         //@mdh needs to be unsigned int
                //int color = pixels[(x*mWidth) + y];                     //@mdh needs to be unsigned int

                //get RGBA pixel data based on width and height
                int colour = image.getPixel(x, y);

                int red = Color.red(colour);
                int blue = Color.blue(colour);
                int green = Color.green(colour);
                int alpha = Color.alpha(colour);

                //Monochrome = (int)((R(color)*0.299) + (G(color)*0.587) + (B(color)*0.114));     //@mdh cast as int, used to be double
                Monochrome = (int) (red * 0.299 + green * 0.587 + blue * 0.114);
                // set new pixel color to output bitmap
                if (Monochrome < DITHERTHRESHOLD) {
                    mDataArray[k++] = BLACK;
                } else {
                    mDataArray[k++] = WHITE;
                }
            }

            if (mDataWidth > mWidth) {
                for (int p = mWidth; p < mDataWidth; p++) {
                    mDataArray[k++] = WHITE;
                }
            }
        }

    }

    //Color data already converted to RGBA and Dithered. Now convert to monochrome based on Dither Threshold
    private void MakeItMonochrome(ArgbColor[] originalData, int width, int height) {
        ArgbColor RGBA;
        int row, col;            //@mdh unsigned
        int index;              //@mdh unsigned
        //Byte Monochrome;
        int Monochrome;
        int k = 0;
        int x = 0;

        //Transform RGBA pixel data to monochrome data
        for (row = height; row > 0; row--) {
            //if(row == 1)
            //x++;

            for (col = 0; col < width; col++) {
                index = ((row - 1) * width) + col;
                //if (index == 87149)
                    //x++;

                RGBA = originalData[index];

                Monochrome = (int) ((RGBA.Red * 0.299) + (RGBA.Green * 0.587) + (RGBA.Blue * 0.114));
                if (Monochrome < DITHERTHRESHOLD) {
                    mDataArray[k++] = BLACK;        //Black = 0
                } else {
                    mDataArray[k++] = WHITE;        //White = 1
                }
            }
            if (mDataWidth > mWidth) {
                for (int p = mWidth; p < mDataWidth; p++) {
                    mDataArray[k++] = WHITE;        //White = 1
                }
            }

        }
    }

    private void DitherFloydSteinberg(ArgbColor[] original, ArgbColor originalPixel, ArgbColor transformedPixel, int x, int y, int width, int height) {
        ArgbColor offsetPixel;

        int redError;
        int blueError;
        int greenError;
        int offsetIndex;            //@mdh unsigned
        int index;                  //@mdh unsigned

        index = y * width + x;
        redError = originalPixel.Red - transformedPixel.Red;
        blueError = originalPixel.Green - transformedPixel.Green;
        greenError = originalPixel.Blue - transformedPixel.Blue;

        if (x + 1 < width) {
            // right
            offsetIndex = index + 1;
            offsetPixel = original[offsetIndex];

            offsetPixel.Red = TooByte((offsetPixel.Red + ((redError * 7) >> 4)));
            offsetPixel.Green = TooByte((offsetPixel.Green + ((greenError * 7) >> 4)));
            offsetPixel.Blue = TooByte((offsetPixel.Blue + ((blueError * 7) >> 4)));
            original[offsetIndex] = offsetPixel;
        }

        if (y + 1 < height) {
            if (x - 1 > 0) {
                // left and down
                offsetIndex = index + width - 1;
                offsetPixel = original[offsetIndex];

                offsetPixel.Red = TooByte((offsetPixel.Red + ((redError * 3) >> 4)));
                offsetPixel.Green = TooByte((offsetPixel.Green + ((greenError * 3) >> 4)));
                offsetPixel.Blue = TooByte((offsetPixel.Blue + ((blueError * 3) >> 4)));
                original[offsetIndex] = offsetPixel;
            }

            // down
            offsetIndex = index + width;
            offsetPixel = original[offsetIndex];

            offsetPixel.Red = TooByte((offsetPixel.Red + ((redError * 5) >> 4)));
            offsetPixel.Green = TooByte((offsetPixel.Green + ((greenError * 5) >> 4)));
            offsetPixel.Blue = TooByte((offsetPixel.Blue + ((blueError * 5) >> 4)));
            original[offsetIndex] = offsetPixel;

            if (x + 1 < width) {
                // right and down
                offsetIndex = index + width + 1;
                offsetPixel = original[offsetIndex];

                offsetPixel.Red = TooByte((offsetPixel.Red + ((redError * 1) >> 4)));
                offsetPixel.Green = TooByte((offsetPixel.Green + ((greenError * 1) >> 4)));
                offsetPixel.Blue = TooByte((offsetPixel.Blue + ((blueError * 1) >> 4)));
                original[offsetIndex] = offsetPixel;
            }
        }
    }

    //Limit all values to the range 0 to 255
    private byte ToByte(int value) {
        if (value < 0)
            value = 0;
        else if (value > 255)
            value = 255;
        value &= 0xff;
        return (byte) (value);
    }

    private int TooByte(int value) {
        if (value < 0)
            value = 0;
        else if (value > 255)
            value = 255;
        return (value & 0xff);
    }

    //Reverse integer and load into byte array
    private long UnsignedInt(int value) {

        long ReturnValue = 0;

        if (value < 0)
            ReturnValue = value + 256;
        else
            ReturnValue = value;

        return (ReturnValue);

    }

    private long UnsignedShort(short value) {
        long ReturnValue = 0;

        if (value < 0)
            ReturnValue = value + 256;
        else
            ReturnValue = value;

        return (ReturnValue);

    }

    private void populate_bmpheader() {
        BMPHeader = new byte[62];                   //byte array for BMP header, compatible to Paint & FGL printer firmware
        BMPHeader[0] = (byte) 0x42;                 //'B'
        BMPHeader[1] = (byte) 0x4d;                 //'M'
        BMPHeader_Int((nRead + 0x3e), 2);           //Size of BMP file in bytes
        BMPHeader[6] = (byte) 0x00;                 //Reserved
        BMPHeader[7] = (byte) 0x00;
        BMPHeader[8] = (byte) 0x00;
        BMPHeader[9] = (byte) 0x00;
        BMPHeader[10] = (byte) 0x3e;                //Offset to data
        BMPHeader[11] = (byte) 0x00;
        BMPHeader[12] = (byte) 0x00;
        BMPHeader[13] = (byte) 0x00;
        BMPHeader[14] = (byte) 0x28;                //Size of BITMAPINFOHEADER structure, must be 40
        BMPHeader[15] = (byte) 0x00;
        BMPHeader[16] = (byte) 0x00;
        BMPHeader[17] = (byte) 0x00;
        BMPHeader_Int(mWidth, 18);                   //image width
        BMPHeader_Int(mHeight, 22);                  //image height
        BMPHeader[26] = (byte) 0x01;                //number of planes in image monochrome = 1
        BMPHeader[27] = (byte) 0x00;
        BMPHeader[28] = (byte) 0x01;                //number of bits per pixel monochrome = 1
        BMPHeader[29] = (byte) 0x00;
        BMPHeader[30] = (byte) 0x00;                //compression type none = 0
        BMPHeader[31] = (byte) 0x00;
        BMPHeader[32] = (byte) 0x00;
        BMPHeader[33] = (byte) 0x00;
        BMPHeader_Int(nRead, 34);                    //image size including padding
        BMPHeader[38] = (byte) 0x00;                //horizontal resolution in pixels per meter
        BMPHeader[39] = (byte) 0x00;
        BMPHeader[40] = (byte) 0x00;
        BMPHeader[41] = (byte) 0x00;
        BMPHeader[42] = (byte) 0x00;                //vertical resolution in pixels per meter
        BMPHeader[43] = (byte) 0x00;
        BMPHeader[44] = (byte) 0x00;
        BMPHeader[45] = (byte) 0x00;
        BMPHeader[46] = (byte) 0x00;                //number of colors in image, or zero
        BMPHeader[47] = (byte) 0x00;
        BMPHeader[48] = (byte) 0x00;
        BMPHeader[49] = (byte) 0x00;
        BMPHeader[50] = (byte) 0x00;                //number of important colors, or zero
        BMPHeader[51] = (byte) 0x00;
        BMPHeader[52] = (byte) 0x00;
        BMPHeader[53] = (byte) 0x00;
        BMPHeader[54] = (byte) 0x00;                //Padded for paint offset 0x3E
        BMPHeader[55] = (byte) 0x00;
        BMPHeader[56] = (byte) 0x00;
        BMPHeader[57] = (byte) 0x00;
        BMPHeader[58] = (byte) 0xff;                //Padded for paint offset 0x3E
        BMPHeader[59] = (byte) 0xff;
        BMPHeader[60] = (byte) 0xff;
        BMPHeader[61] = (byte) 0x00;

    }

    //Reverse integer and load into byte array
    private void BMPHeader_Int(int intToReverse, int index) {
        byte[] intAsBytes = new byte[]
                {
                        (byte) (intToReverse & 0xFF),
                        (byte) ((intToReverse >> 8) & 0xFF),
                        (byte) ((intToReverse >> 16) & 0xFF),
                        (byte) ((intToReverse >> 24) & 0xFF),
                };

        BMPHeader[index] = intAsBytes[0];
        BMPHeader[index + 1] = intAsBytes[1];
        BMPHeader[index + 2] = intAsBytes[2];
        BMPHeader[index + 3] = intAsBytes[3];

    }

    private boolean test = false;

    private void CreateRawMonochromeData() {
        //byte first;
        int first;
        //byte second;
        int second;

        int length = 0;
        for (int i = 0; i < mDataArray.length; i = i + 8) {
            first = mDataArray[i] & 0xff;
            if (first == 0)
                test = true;
            for (int j = 0; j < 7; j++) {
                second = (((first << 1) & 0xff) | mDataArray[i + j]) & 0xff;
                first = second;
            }
            mRawBitmapData[length] = first;
            if (first < 255)
                test = true;
            length++;
        }
        nRead = length;
    }


    //Grayscale ARGB
    private ArgbColor TransformPixel(ArgbColor pixel) {
        //Byte Monochrome;
        int Monochrome;
        ArgbColor NewColor = new ArgbColor();

        //Monochrome = (byte)((pixel.Red*0.299) + (pixel.Green*0.587) + (pixel.Blue*0.114));
        Monochrome = TooByte((int) ((pixel.Red * 0.299) + (pixel.Green * 0.587) + (pixel.Blue * 0.114)));
        //Monochrome = (int)((pixel.Red*0.299) + (pixel.Green*0.587) + (pixel.Blue*0.114));

        if (Monochrome < DITHERTHRESHOLD) {
            //Black
            NewColor.Alpha = pixel.Alpha;
            NewColor.Red = 0;
            NewColor.Green = 0;
            NewColor.Blue = 0;
        } else {
            //White
            NewColor.Alpha = pixel.Alpha;
            NewColor.Red = 255;         //(byte)255;
            NewColor.Green = 255;       //(byte)255;
            NewColor.Blue = 255;        //(byte)255;
        }
        return (NewColor);

    }

    private static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    //Read one of the text files included in Assets and send it to the printer
    private boolean RWFile(String file_name) {
        //InputStream inStream = null;
        BufferedInputStream bis = null;
        String msg = "";
        boolean status = false;

        try {
            //AssetManager am = getApplicationContext().getAssets();
            AssetManager am = mContext.getAssets();
            //(mContext);      //used to be this instead of mContext
            InputStream inStream = am.open(file_name);

            // input stream is converted to buffered input stream
            bis = new BufferedInputStream(inStream);

            // read until a single byte is available
            while (bis.available() > 0) {
                // read next available character
                char ch = (char) bis.read();

                //Debug print the read character.
                //System.out.println("The character read = " + ch );
                msg = msg + ch;
            }
            SendString(msg);
            status = true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return (status);
    }

    //Read text file from specified directory
    private boolean RWTextFile(final String url) {
    // public boolean RWTextFile(final String url) {
        String testFile;
        String str = "";

        boolean status = false;

        try {
            if (0 == url.length())
            {
                StatusReportCallback("url is empty");
                return status;
            }
            // String msg = String.format("Memory: %ld", getMemorySizeInBytes());
            // StatusReportCallback(msg);

            File file = new File(url);
            if (null == file)
            {
                StatusReportCallback("new File is NULL");
                return status;
            }
            FileInputStream fin = new FileInputStream(file);
            if (null == fin)
            {
                StatusReportCallback("FileInputStream is NULL");
                return status;
            }
            str = convertStreamToString(fin);
            //Make sure you close all streams.
            fin.close();
            //if file read
            if (0 == str.length()) {
                StatusReportCallback("FileInputStream is NULL");
                return status;
            }
            //output file to printer as one long string
            SendString(str);
            status = true;
        } catch (Exception e) {
            str = e.toString();
            StatusReportCallback(str);
            // e.printStackTrace();
        }
        str = "";
        return (status);
    }

    private Context mContext;

    public class BitmapConvertor {

        private int mDataWidth;
        //private byte[] mDataArray;
        //private int[] mDataArrayI;
        private static final String TAG = "BitmapConvertor";
        // private ProgressDialog mPd;
        //private Context mContext;
        private int mWidth, mHeight;
        private String mStatus;
        private String mFileName;


        public BitmapConvertor(Context context) {
            // TODO Auto-generated constructor stub
            //if (context == null)
            //context = getActivity().getApplicationContext();
            mContext = context;
        }

        //Converts image to monochrome bitmap
        public String convertBitmap(Bitmap inputBitmap)     //,int BMP_WIDTH, int BMP_HEIGHT)
        {

            mWidth = inputBitmap.getWidth();
            mHeight = inputBitmap.getHeight();
            mDataWidth = ((mWidth + 31) / 32) * 4 * 8;
            mDataArray = new int[(mDataWidth * mHeight)];
            mRawBitmapData = new int[(mDataWidth * mHeight) / 8];

            ConvertInBackground convert = new ConvertInBackground();
            convert.execute(inputBitmap);

            return mStatus;

        }

        public String convertImageBitmap(Bitmap inputBitmap, int height, int width) {
            PrintBMP(inputBitmap, width, height);
            return mStatus;
        }

        //Since converting the color data to monochrome takes a little time,
        //it is best to process it in a seperate thread in the background
        class ConvertInBackground extends AsyncTask<Bitmap, String, Void> {

            @Override
            protected Void doInBackground(Bitmap... params) {

                Bitmap bitmap;
                int BMP_WIDTH;
                int BMP_HEIGHT;

                if (PrinterOrientation.equals("<LM>")) {
                    BMP_WIDTH = mWidth;
                    BMP_HEIGHT = mHeight;
                }
                else
                {
                    BMP_WIDTH = mHeight;
                    BMP_HEIGHT = mWidth;

                }
                bitmap = getResizedBitmap(params[0], BMP_WIDTH, BMP_HEIGHT);       //this scales the image
                PrintBMP(bitmap, BMP_WIDTH, BMP_HEIGHT);
                return null;
            }
/*
            //Once processing is complete inform the user, the monochrome image is being printed.
            @Override
            protected void onPostExecute(Void result) {
                //String file_name = "/storage/emulated/0/Documents/concert_stock_landscape_new.bmp"; //Test

                mPd.dismiss();
                Toast.makeText(mContext, "Monochrome bitmap image is being sent to the printer.", Toast.LENGTH_LONG).show();

            }

            //While processing image, display icon informing the user the conversion is taking place
            @Override
            protected void onPreExecute() {

                mPd = ProgressDialog.show(mContext, "Converting Image", "Please Wait", true, false, null);
            }

*/
        }
    }


    //When rotating into portrait mode, there is a little positioning error with default setting
    //The dot array below is used to reduce the positioning error.  Some of the numbers are
    //approximates and can use some fine tuning if used.
    private void InitDotArray() {
        //concert stock 2"
        portrait_dot_array[0][0] = 384;     //200 DPI
        portrait_dot_array[0][1] = 576;     //300 DPI
        portrait_dot_array[0][2] = 1184;    //600 DPI
        //cinema stock 3.25"
        portrait_dot_array[1][0] = 640;     //200 DPI
        portrait_dot_array[1][1] = 960;     //300 DPI
        portrait_dot_array[1][2] = 1920;    //600 DPI approximate
        //credit card stock 2.125"
        portrait_dot_array[2][0] = 416;     //200 DPI
        portrait_dot_array[2][1] = 608;     //300 DPI
        portrait_dot_array[2][2] = 1232;    //600 DPI
        //receipt stock 3.25"
        portrait_dot_array[3][0] = 640;     //200 DPI
        portrait_dot_array[3][1] = 960;     //300 DPI
        portrait_dot_array[3][2] = 1920;    //600 DPI approximate
        //ski stock 3.25"
        portrait_dot_array[4][0] = 640;     //200 DPI
        portrait_dot_array[4][1] = 960;     //300 DPI
        portrait_dot_array[4][2] = 1920;    //600 DPI approximate
        //4x6 stock
        portrait_dot_array[5][0] = 816;     //200 DPI
        portrait_dot_array[5][1] = 1216;    //300 DPI
        portrait_dot_array[5][2] = 2360;    //600 DPI approximate
        //Wristband 1 stock 1"
        portrait_dot_array[6][0] = 190;     //200 DPI approximate
        portrait_dot_array[6][1] = 290;     //300 DPI approximate
        portrait_dot_array[6][2] = 590;     //600 DPI approximate
        //Wristband 2 stock 1.328"
        portrait_dot_array[7][0] = 250;     //200 DPI approximate
        portrait_dot_array[7][1] = 380;     //300 DPI approximate
        portrait_dot_array[7][2] = 780;     //600 DPI approximate
        //letter/custom stock
        portrait_dot_array[8][0] = 1700;    //200 DPI approximate
        portrait_dot_array[8][1] = 2580;    //300 DPI approximate
        portrait_dot_array[8][2] = 5000;    //600 DPI approximate

        set_portrait_dots();

    }

    //read portrait dots width from array
    private void set_portrait_dots() {
        if (PrinterOrientation.equals("<PM>")) {
            //retrieve dots per width (lptix) in portrait mode
            switch (PrinterResolution) {
                case 200:
                    portrait_dots = portrait_dot_array[size_indicator][0];
                    break;
                case 300:
                    portrait_dots = portrait_dot_array[size_indicator][1];
                    break;
                case 600:
                    portrait_dots = portrait_dot_array[size_indicator][2];
                    break;

            }
        } else {
            portrait_dots = 0;
        }
    }


    private static byte[] CharsToBytes(char[] buffer)
    {

        byte[] b = new byte[buffer.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) buffer[i];
        }
        return b;

    }

    private static char[] BytesToChars(byte[] buffer)
    {

        char[] b = new char[buffer.length];
        for (int i = 0; i < b.length; i++) {
            b[i] = (char) buffer[i];
        }
        return b;

    }


}