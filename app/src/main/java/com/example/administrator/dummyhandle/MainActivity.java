package com.example.administrator.dummyhandle;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;



@SuppressLint("ValidFragment")
class ViewDialogFragment extends DialogFragment {

    public interface Callback {
        void onClick(String userName, String password);
    }

    private Callback callback;

    public void show(FragmentManager fragmentManager) {
        show(fragmentManager, "ViewDialogFragment");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.login, null);

        builder.setView(view)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (callback != null) {
                            EditText et_userName = view.findViewById(R.id.username);
                            EditText et_password = view.findViewById(R.id.password);
                            callback.onClick(et_userName.getText().toString(), et_password.getText().toString());
                        }
                    }
                });
        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Callback) {
            callback = (Callback) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement Callback");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        callback = null;
    }
}

public class MainActivity extends AppCompatActivity  implements ViewDialogFragment.Callback{
    private WebSocketClient webSocketClient;
    private String string="ws://192.168.11.103:6341/"; //IP and port
    private TextView message;
    private int state;
    private int Index;
    private Double Destination;
    private ImageButton go;
    private ImageButton back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//竖屏
        state=1;
        Index=0;
        Destination=0.0;
        message = findViewById(R.id.textView);
        go = findViewById(R.id.imageButton2);
        back = findViewById(R.id.imageButton);

        go.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    webSocketClient.send(Move(Index,Destination+1).toString());
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    webSocketClient.send(Stop().toString());
                }
                return false;
            }
        });
        back.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    webSocketClient.send(Move(Index,Destination-1).toString());
                }else if(event.getAction() == MotionEvent.ACTION_UP){
                    webSocketClient.send(Stop().toString());
                }
                return false;
            }
        });

        RadioGroup radioGroup = findViewById(R.id.sex_rg);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.X:
                        Index = 0;
                        break;
                    case R.id.Y:
                        Index = 1;
                        break;
                    case R.id.Z:
                        Index = 2;
                        break;
                    case R.id.R1:
                        Index = 3;
                        break;
                    case R.id.R3:
                        Index = 4;
                        break;
                }
            }
        });
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == 1){
                    wbStart();
                    state=0;
                }
            }
        });

        Button button2 = findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showViewDialogFragment(v);
            }
        });
        Button button3 = findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webSocketClient != null) {
                    webSocketClient.close();
                    message.setText("");
                    state = 1;
                }
            }
        });
    }


    public void showViewDialogFragment(View view) {
        ViewDialogFragment viewDialogFragment = new ViewDialogFragment();
        viewDialogFragment.show(getFragmentManager());
    }
    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {

        @SuppressLint("SetTextI18n")
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            parseJson((String) msg.obj);
        }
    };

    @SuppressLint("SetTextI18n")
    private void parseJson(String strResult){
        try{
            SharedPreferences settings = getSharedPreferences("fanrunqi", 0);
            String isAmazing = settings.getString("config","ws://,192.168.11.103,:,6341,/");
            String[]  strs=isAmazing.split(",");   //利用正则表达来提取字符
            message.setText("message:"+"\n"+"IP:"+ strs[1] +"\n"+"Port:"+ strs[3]);
            message.setText(message.getText()+"\n"+"Index="+Index);
            String person = new JSONObject(strResult).getString("Master ID");
            message.setText(message.getText()+"\n"+"Master ID:" + person);
            JSONArray jsonArray = new JSONObject(strResult).getJSONArray("Doubles");
            Destination = (Double) jsonArray.get(Index);
            message.setText(message.getText()+"\n"+"Destination" + Destination);
            message.setText(message.getText()+"\n"+"X="+jsonArray.get(0));
            message.setText(message.getText()+"\n"+"Y="+jsonArray.get(1));
            message.setText(message.getText()+"\n"+"Z="+jsonArray.get(2));
            message.setText(message.getText()+"\n"+"R1="+jsonArray.get(3));
            message.setText(message.getText()+"\n"+"R3="+jsonArray.get(4));
            JSONArray jsonArray2 = new JSONObject(strResult).getJSONArray("Booleans");
            message.setText(message.getText()+"\n"+" X:  MOVE =" +jsonArray2.get(0)+ "  Limit- ="+jsonArray2.get(5)+"   Limit+ ="+jsonArray2.get(6));
            message.setText(message.getText()+"\n"+" Y:  MOVE =" +jsonArray2.get(1)+ "  Limit- ="+jsonArray2.get(7)+"   Limit+ ="+jsonArray2.get(8));
            message.setText(message.getText()+"\n"+" Z:  MOVE =" +jsonArray2.get(2)+ "  Limit- ="+jsonArray2.get(9)+"   Limit+ ="+jsonArray2.get(10));
            message.setText(message.getText()+"\n"+"R1:  MOVE =" +jsonArray2.get(3)+ "  Limit- ="+jsonArray2.get(11)+"  Limit+ ="+jsonArray2.get(12));
            message.setText(message.getText()+"\n"+"R3:  MOVE =" +jsonArray2.get(4)+ "  Limit- ="+jsonArray2.get(13)+"  Limit+ ="+jsonArray2.get(14));
        }catch (JSONException e) {
            message.setText("Json pares error");
            e.printStackTrace();
        }
    }

    public void wbStart() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    webSocketClient = new WebSocketClient(new URI(string), new Draft_10()) {
                        @Override
                        public void onOpen(ServerHandshake handshakedata) {
                            Log.d("picher_log", "打开通道" + handshakedata.getHttpStatus());//设置日志标志和信息
                        }

                        @Override
                        public void onMessage(String message) {
                            Log.d("picher_log", "接收消息" + message);
                            handler.obtainMessage(0, message).sendToTarget();
                        }

                        @Override
                        public void onClose(int code, String reason, boolean remote) {
                            Log.d("picher_log", "通道关闭");
                        }
                        public void onError(Exception ex) {
                            Log.d("picher_log", "链接错误");
                        }
                    };
                    webSocketClient.connect();// connect webSocket
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }
    public JSONObject Move (int Index,Double Destination){
        try {
            // 首先最外层是{}，是创建一个对象
            JSONObject person = new JSONObject();
            person.put("command", "MOVE");
            // 键data的值是对象，所以又要创建一个对象
            JSONObject data = new JSONObject();
            data.put("Index", Index);
            data.put("Destination", Destination);
            person.put("data", data.toString());//data value is String
            return person;
        } catch (JSONException ex) {
            // 键为null或使用json不支持的数字格式(NaN, infinities)
            throw new RuntimeException(ex);
        }
    }
    public JSONObject Stop (){
        try {
            // 首先最外层是{}，是创建一个对象
            JSONObject person = new JSONObject();
            person.put("command", "EMG STOP");
            // 键data的值是对象，所以又要创建一个对象
            person.put("data", "");//data value is String
            return person;
        } catch (JSONException ex) {
            // 键为null或使用json不支持的数字格式(NaN, infinities)
            throw new RuntimeException(ex);
        }
    }
    @SuppressLint("ApplySharedPref")
    public void onClick(String IP, String Port) {
        Toast.makeText(MainActivity.this, "IP: " + IP + " Port: " + Port, Toast.LENGTH_SHORT).show();
        String wb ="ws://,"+ IP +",:,"+Port+",/";
        SharedPreferences settings = getSharedPreferences("fanrunqi", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("config",wb);
        editor.commit();// 提交本次编辑
    }
}
