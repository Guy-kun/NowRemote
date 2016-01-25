package low.moe.nowremote;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Status extends AppCompatActivity {
    int indoorTemp, outdoorTemp;

    static MqttClient mqttClient;

    TextView insideTempTextView;
    TextView outsideTempTextView;
    TextView messageTextView;

    ScrollView scrollView;
    LinearLayout adjustableLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        scrollView = (ScrollView)findViewById(R.id.scrollViewA);
        adjustableLinearLayout = (LinearLayout)findViewById(R.id.adjustableHeightLinearLayout);


        insideTempTextView = (TextView)findViewById(R.id.insideTempText);
        outsideTempTextView = (TextView)findViewById(R.id.outsideTempText);
        messageTextView = (TextView)findViewById(R.id.messageText);
        indoorTemp = -99;
        outdoorTemp = 0;
        setTemps();

        Intent intent = getIntent();
        processIntent(intent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkMQTTSetup();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent){
        if (intent.getExtras()!=null) {
            int cmd = intent.getIntExtra(AccessibilityService.CommandTypeExtra,-1);
            int param = intent.getIntExtra(AccessibilityService.CommandParamExtra,-1);
            handleCommand(AccessibilityService.CommandType.GetValue(cmd), AccessibilityService.CommandParam.GetValue(param));
        }

        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                adjustableLinearLayout.setLayoutParams(new LinearLayout.LayoutParams(adjustableLinearLayout.getLayoutParams().width,
                        scrollView.getHeight()));
            }
        });
    }

    private void handleCommand(AccessibilityService.CommandType type,AccessibilityService.CommandParam param)
    {
        if (param.IsEmpty())
            return;

        if (type == AccessibilityService.CommandType.AC_POWER)
        {
            sendMQTTMessage("ac", param == AccessibilityService.CommandParam.ON ? "on" : "off");
        }
    }

    private void sendMQTTMessage(String type,String value){
        checkMQTTSetup();
        MqttMessage message = new MqttMessage((type+":"+value).getBytes());
        message.setQos(0);
        try {
            mqttClient.publish("aircon/1",message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    private void checkMQTTSetup() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            try {
                mqttClient = new MqttClient("tcp://broker.mqttdashboard.com:1883", "clientId-UwHaTRClaa", new MemoryPersistence());
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                mqttClient.connect(connOpts);
                mqttClient.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.e("MQTT connection lost", "");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        if (topic.equals("airconnode/1")) {
                            try {
                                String tempString = new String(message.getPayload());
                                String[] elems = tempString.split(":");

                                int temp = -99;

                                temp = Integer.parseInt(elems[1]);
                                indoorTemp = temp;

                            } catch (Exception ex) {
                                //
                            }
                            Status.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setTemps();
                                }
                            });
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
                mqttClient.subscribe("airconnode/1");

            } catch (MqttException e) {
                e.printStackTrace();
            }


        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_status, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setTemps(){
        insideTempTextView.setText(indoorTemp+"");
        outsideTempTextView.setText(outdoorTemp + "");

    }
}
