package low.moe.nowremote;

import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by anton.prydatko on 1/22/16.
 */
public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
    public static final String CommandTypeExtra = "CommandType";
    private long lastCommandTime = 0L;

    public enum CommandType
    {
        UNKNOWN(-1),
        AC_POWER(0);
        int id;
        private CommandType(int i){id = i;}

        public int GetID(){return id;}
        public boolean IsEmpty(){return this.equals(CommandType.UNKNOWN);}
        public boolean Compare(int i){return id == i;}
        public static CommandType GetValue(int _id)
        {
            CommandType[] As = CommandType.values();
            for(int i = 0; i < As.length; i++)
            {
                if(As[i].Compare(_id))
                    return As[i];
            }
            return CommandType.UNKNOWN;
        }
    }
    public enum CommandParam
    {
        UNKNOWN(-1),
        ON(0),
        OFF(1);

        int id;
        private CommandParam(int i){id = i;}

        public int GetID(){return id;}
        public boolean IsEmpty(){return this.equals(CommandParam.UNKNOWN);}
        public boolean Compare(int i){return id == i;}
        public static CommandParam GetValue(int _id)
        {
            CommandParam[] As = CommandParam.values();
            for(int i = 0; i < As.length; i++)
            {
                if(As[i].Compare(_id))
                    return As[i];
            }
            return CommandParam.UNKNOWN;
        }
    }


    public static final String CommandParamExtra = "CommandParam";


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event!=null && event.getSource()!=null && (
                event.getSource().getClassName().toString().equals("com.google.android.search.searchplate.SimpleSearchText") ||
                        event.getSource().getClassName().toString().equals("com.google.android.apps.gsa.searchplate.SimpleSearchText") ||
                        event.getSource().getClassName().toString().equals("com.google.android.apps.gsa.searchplate.StreamingTextView")))
        {
           if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && event.getSource().getText()!=null)
           {
               this.interpretCommand(event.getSource().getText().toString(),event.getEventTime());
           }
        }
    }

    private void interpretCommand(String command,long eventTime){
        Log.d("remote command",command);

        ArrayList<String> commandWords = new ArrayList<String>(Arrays.asList(command.split(" ")));

        ArrayList<String> commandParams;

        commandParams = paramsForMatchingInput(commandWords,"Turn the AC".split(" "));
        commandParams = commandParams==null?paramsForMatchingInput(commandWords,"Switch the AC".split(" ")):commandParams;
        if (commandParams!=null && commandParams.size()>0){
            String param = commandParams.get(0);
            this.beginActivityWithCommand(CommandType.AC_POWER,param.equals("on")?CommandParam.ON:CommandParam.OFF,eventTime);
        }
    }

    private static ArrayList<String> paramsForMatchingInput(ArrayList<String> input, String[] pattern){
        int i;
        for (i=0;i <pattern.length;i++)
        {
            if (i>=input.size())
            {
                return null;
            }
            if (!pattern[i].equalsIgnoreCase(input.get(i)))
            {
                return null;
            }
        }
        if (input.size()>i) {
            return new ArrayList<String>(input.subList(i, input.size()));
        }
        return null;
    }

    private void beginActivityWithCommand(CommandType type,CommandParam param,long eventTime){
        if (this.lastCommandTime + 1000L > eventTime)
        {
            return;
        }
        lastCommandTime = eventTime;

        Intent myIntent = new Intent(this, Status.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        myIntent.putExtra(CommandTypeExtra, type.id);
        myIntent.putExtra(CommandParamExtra, param.id);
        startActivity(myIntent);
    }


    @Override
    public void onInterrupt() {

    }
}
