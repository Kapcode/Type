package com.gmail.kapcode.type;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;
import org.jnativehook.mouse.NativeMouseMotionListener;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Type implements NativeKeyListener, NativeMouseListener, NativeMouseMotionListener {
    volatile int duration = 0, startDelay = 0, afterLoopDelay = 0, valueOfTag_every = 0, targetLoopCount = 1;
    //test comment
    boolean safetyEnabled = true;
    ArrayList<Integer> safetyMouseButtons;
    ArrayList<String> safetyKeys;
    ArrayList<Point> safetyLocations;
    static String defaultSafetyString="0,0,Meta,mouse3";//implemented, but not using string in code
    ArrayList<String[]> tags = new ArrayList<>();
    boolean press = true, release = true, countPresent = false, Tag_Every_is_Present = false;
    static Robot robot;
    static String supportedKeys = "Backspace\n" +
            "Insert\n" + "Home\n" + "Tab\n" + "Q\n" + "W\n" + "E\n" + "R\n" + "T\n" + "Y\n" + "U\n" + "I\n" +
            "O\n" + "P\n" + "Delete\n" + "End\n" + "Up\n" + "A\n" + "S\n" + "D\n" + "F\n" + "G\n" + "H\n" + "J\n" + "K\n" + "L\n" +
            "Semicolon\n" + "Quote\n" + "Enter\n" + "Clear\n" + "Shift\n" + "Z\n" + "X\n" + "C\n" +
            "V\n" + "B\n" + "N\n" + "M\n" + "Comma\n" + "Period\n" + "Slash\n" + "Ctrl\n" + "Meta\n" +
            "Alt\n" + "Space\n" + "Left\n" + "Down\n" + "Right\n" + "Space";
    static ArrayList<Integer> keyCodesNotReleased = new ArrayList<>();

    public static void main(String[] args) throws AWTException {
        // write your code here

        args = new String[]{"-wait","200000","-release","false","-keys","a,b,c"};

        robot = new Robot();
        if (args.length > 0) {
            if (args[0].equals("help") || args[0].equals("-help")) {
                System.out.println(help);
                exit("");
            } else {
                try {
                    // Get the logger for "org.jnativehook" and set the level to off.
                    Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
                    logger.setLevel(Level.OFF);
                    // Don't forget to disable the parent handlers.
                    logger.setUseParentHandlers(false);
                    GlobalScreen.registerNativeHook();
                    Type type = new Type(args);
                    GlobalScreen.addNativeKeyListener(type);
                    GlobalScreen.addNativeMouseMotionListener(type);
                    GlobalScreen.addNativeMouseListener(type);
                } catch (NativeHookException ex) {
                    System.err.println("There was a problem registering the native hook.");
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
            }

        } else {
            //no args, give description, give hint to call -help
        }

    }

    public Type(String[] args) {
        createKeyMap();
        new Thread(new Runnable() {
            @Override
            public void run() {
                setVarsFromArgs(args);
                //wait startDelay
                if (startDelay > 0) {
                    try {
                        Thread.sleep(startDelay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //start a thread that waits for -durations millis, then terminates program *if not 0
                if (duration > 0)
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(duration);
                                exit("Exit due to -duration " + duration);
                            } catch (InterruptedException e) {
                                exit("InterruptedException in -duration Thread");
                            }

                        }
                    }).start();


                int iterations = 0;
                //while need to click more to meet target count,   or  if -every and not -count.
                while (iterations < targetLoopCount || (Tag_Every_is_Present && !countPresent)) {

                    long iterationStartTime = System.currentTimeMillis();

                    //iterate over list of tags:
                    //-text
                    //-shortcuts
                    //-keys type keys listed
                    //-autoWait  set robot auto wait
                    //-wait   wait millis
                    //press ONLY VALID FOR -keys tag
                    //release  ONLY VALID FOR -keys tag
                    int index = 0;
                    for (String[] tag : tags) {
                        switch (tag[0]) {
                            case "-text":
                                typeText(tag[1]);
                                break;
                            case "-shortcut":
                                typeShortcut(tag[1]);
                                break;
                            case "-keys":
                                typeKeys(tag[1]);
                                break;
                            case "-autoWait":
                                robot.setAutoDelay(Integer.parseInt(tag[1]));
                                break;
                            case "-wait":
                                try {
                                    //print out
                                    System.out.println("Waiting: " + tag[1] + " mills");
                                    Thread.sleep(Integer.parseInt(tag[1]));
                                } catch (InterruptedException e) {
                                    exit("-wait " + tag[1] + " failed: InterruptedException");
                                }
                                break;
                            case "-press":
                                press = Boolean.parseBoolean(tag[1]);
                                break;
                            case "-release":
                                release = Boolean.parseBoolean(tag[1]);
                                break;
                        }


                    }


                    //calculate time to wait
                    long iterationEndTime = System.currentTimeMillis();
                    long timeIterationTook = (iterationEndTime - iterationStartTime);
                    long timeToSleep = (valueOfTag_every - timeIterationTook) + afterLoopDelay;
                    try {
                        if (timeToSleep > 0) Thread.sleep(timeToSleep);
                    } catch (InterruptedException e) {
                        exit("InterruptedException on afterLoopSleep");
                    }


                    iterations++;
                }
                exit("Iterations=" + iterations);
                //on exit, release all pressed keys.
            }
        }).start();
    }

    private void typeShortcut(String shortcut) {
        System.out.println("Typing Shortcut:" + shortcut);
        for (String key : shortcut.split(",")) {
            int code = keyCodeMap.get(key);
            pressKeyCode(code);
        }
        for (String key : shortcut.split(",")) {
            int code = keyCodeMap.get(key);
            robot.keyRelease(code);
        }

    }


    private void typeText(String text) {
        System.out.println("Typing Text:" + text);
        for (char c : text.toCharArray()) {
            String cstring = "" + c;
            boolean shift = keyNeedsShift(cstring);
            int code = keyCodeMap.get(cstring);
            if (shift) pressKeyCode(KeyEvent.VK_SHIFT);
            pressKeyCode(code);
            releaseKeyCode(code);
            if (shift) releaseKeyCode(KeyEvent.VK_SHIFT);
        }
    }
    void pressKeyCode(int code){
        if(!keyCodesNotReleased.contains(code))keyCodesNotReleased.add(code);
        robot.keyPress(code);
    }
    void releaseKeyCode(int code){
        if(keyCodesNotReleased==null)System.out.println("ARRAY_NULL");
        if(keyCodesNotReleased.contains(code))keyCodesNotReleased.remove((Object)code);
        robot.keyRelease(code);
    }
    static synchronized void releaseAllPressedKeys(){
        int oldDelay=robot.getAutoDelay();
        robot.setAutoDelay(0);
        for(int code:keyCodesNotReleased){
            robot.keyRelease(code);
        }
        robot.setAutoDelay(oldDelay);
        keyCodesNotReleased.clear();
    }
    private void typeKeys(String keys) {

        System.out.println("Typing Keys:" + keys);
        for (String key : keys.split(",")) {
            int code = keyCodeMap.get(key);
            if(press)pressKeyCode(code);
            if(release)releaseKeyCode(code);
        }
    }

    private static void exit(String s) {
        System.out.println(s);
        if(keyCodesNotReleased.size()>0)System.out.println("Releasing "+keyCodesNotReleased.size()+" un-released keys!");
        releaseAllPressedKeys();
        System.exit(0);
    }


    public void setVarsFromArgs(String[] args) {
        //-safety alt,leftmouse,rightmouse,mouse1
        //-duration
        //-every
        //-startDelay
        //-afterLoopDelay
        //-count
        int index = 0;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                switch (arg) {
                    case "-safety":
                        String value = args[index + 1];
                        //examples
                        // 0,0,escape,alt
                        // escape,0,0,alt
                        // escape,alt,0,0
                        safetyEnabled = !(value.equals("off") || value.equals("false"));
                        System.out.println("Safety="+safetyEnabled);
                        if (safetyEnabled) {
                            ArrayList<String> supportedKeys = null;
                            //if safety enabled
                            String[] safetyStrings = value.split(",");//split by comma
                            int safetyIndex = 0;
                            boolean skipIndex = false;
                            for (String safetyString : safetyStrings) {// escape,0,7

                                try {
                                    if (!skipIndex) {
                                        int x = Integer.parseInt(safetyString);//fails if not a number
                                        int y = Integer.parseInt(safetyStrings[safetyIndex + 1]);
                                        if (safetyLocations == null) safetyLocations = new ArrayList<>();
                                        safetyLocations.add(new Point(x, y));
                                        System.out.println("Safety location: "+x+","+y);
                                        skipIndex = true;
                                    } else {
                                        skipIndex = false;
                                        //this is definitely a y value, ignore it!
                                    }


                                } catch (NumberFormatException e) {
                                    //a key
                                    if (safetyKeys == null) safetyKeys = new ArrayList<>();
                                    if(safetyMouseButtons==null)safetyMouseButtons=new ArrayList<>();
                                    if (supportedKeys == null) supportedKeys = createSupportedKeysList();
                                    if (supportedKeys.contains(safetyString)) {
                                        safetyKeys.add(safetyString);
                                        System.out.println("Safety key: "+safetyString);
                                    }else if(safetyString.startsWith("mouse")){
                                        int button = Integer.parseInt(safetyString.replace("mouse",""));
                                        safetyMouseButtons.add(button);
                                        System.out.println("Safety mouse button: "+button);
                                    } else {
                                        exit("Safety key:" + safetyString + " Not supported, use 'click help' for a list of supported keys.");
                                    }


                                } catch (ArrayIndexOutOfBoundsException e) {
                                    System.err.println("Check Your Arguments!a");
                                    System.exit(0);
                                }
                                safetyIndex++;
                            }
                        }


                        break;

                    case "-duration":
                        duration = Integer.parseInt(args[index + 1]);
                        break;
                    case "-every":
                        Tag_Every_is_Present = true;
                        valueOfTag_every = Integer.parseInt(args[index + 1]);
                        break;
                    case "-startDelay":
                        startDelay = Integer.parseInt(args[index + 1]);
                        break;
                    case "-afterLoopDelay":
                        afterLoopDelay = Integer.parseInt(args[index + 1]);
                        break;
                    case "-count":
                        countPresent = true;
                        targetLoopCount = Integer.parseInt(args[index + 1]);
                        break;

                    //populate tags list, will be handled in type loop
                    case "-text":
                    case "-shortcut":
                    case "-keys":
                    case "-autoWait":
                    case "-wait":
                    case "-press":
                    case "-release":
                        tags.add(new String[]{arg, args[index + 1]});
                        break;


                }


            }
            index++;
        }
        if(safetyEnabled && safetyKeys==null &&safetyLocations==null && safetyMouseButtons==null){
            System.out.println("Set default safety: "+defaultSafetyString);
            safetyKeys = new ArrayList<>();
            safetyKeys.add("Meta");
            safetyLocations=new ArrayList<>();
            safetyLocations.add(new Point(0,0));
            safetyMouseButtons=new ArrayList<>();
            safetyMouseButtons.add(3);
        }


    }


    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {

    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        //check if is a safety key
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
        String text = NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode());
        if(safetyEnabled && (safetyKeys.contains(text)|| safetyKeys.contains(text.toLowerCase()))){
            exit("Safety key pressed! "+text);
        }


    }


    @Override
    public void nativeMouseClicked(NativeMouseEvent nativeMouseEvent) {

    }

    @Override
    public void nativeMousePressed(NativeMouseEvent nativeMouseEvent) {

    }

    @Override
    public void nativeMouseReleased(NativeMouseEvent nativeMouseEvent) {
        int button = nativeMouseEvent.getButton();
        if(safetyEnabled && safetyMouseButtons.contains(button)){
            exit("Safety mouse button pressed! "+button);
        }
    }

    @Override
    public void nativeMouseMoved(NativeMouseEvent nativeMouseEvent) {
        Point p = nativeMouseEvent.getPoint();
        if(safetyEnabled && safetyLocations.contains(p)){
            exit("Safety location entered! "+p.x+","+p.y);
        }

    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent nativeMouseEvent) {
        Point p = nativeMouseEvent.getPoint();
        if(safetyEnabled && safetyLocations.contains(p)){
            exit("Safety location entered! "+p.x+","+p.y);
        }
    }


    public HashMap<String, Integer> keyCodeMap;
    public String charsThatRequireShift = "~!@#$%^&*()_+QWERTYUIOP{}|ASDFGHJKL:\"ZXCVBNM<>?;";

    public void createKeyMap() {
        keyCodeMap = new HashMap<>();
        keyCodeMap.put("Escape", KeyEvent.VK_ESCAPE);
        keyCodeMap.put("ESC", KeyEvent.VK_ESCAPE);
        keyCodeMap.put("F1", KeyEvent.VK_F1);
        keyCodeMap.put("F2", KeyEvent.VK_F2);
        keyCodeMap.put("F3", KeyEvent.VK_F3);
        keyCodeMap.put("F4", KeyEvent.VK_F4);
        keyCodeMap.put("F5", KeyEvent.VK_F5);
        keyCodeMap.put("F6", KeyEvent.VK_F6);
        keyCodeMap.put("F7", KeyEvent.VK_F7);
        keyCodeMap.put("F8", KeyEvent.VK_F8);
        keyCodeMap.put("F9", KeyEvent.VK_F9);
        keyCodeMap.put("F10", KeyEvent.VK_F10);
        keyCodeMap.put("F11", KeyEvent.VK_F11);
        keyCodeMap.put("F12", KeyEvent.VK_F12);
        keyCodeMap.put("1", 49);
        keyCodeMap.put("2", 50);
        keyCodeMap.put("3", 51);
        keyCodeMap.put("4", 52);
        keyCodeMap.put("5", 53);
        keyCodeMap.put("6", 54);
        keyCodeMap.put("7", 55);
        keyCodeMap.put("8", 56);
        keyCodeMap.put("9", 57);
        keyCodeMap.put("0", 48);
        keyCodeMap.put("Backspace", KeyEvent.VK_BACK_SPACE);
        keyCodeMap.put("Insert", KeyEvent.VK_INSERT);
        keyCodeMap.put("Home", KeyEvent.VK_HOME);
        keyCodeMap.put("PageUp", KeyEvent.VK_PAGE_UP);
        keyCodeMap.put("Delete", KeyEvent.VK_DELETE);
        keyCodeMap.put("End", KeyEvent.VK_END);
        keyCodeMap.put("PageDown", KeyEvent.VK_PAGE_DOWN);
        keyCodeMap.put("PrintScreen", KeyEvent.VK_PRINTSCREEN);
        keyCodeMap.put("PS", KeyEvent.VK_PRINTSCREEN);
        keyCodeMap.put("&", 55);
        keyCodeMap.put("*", 56);
        keyCodeMap.put("(", 57);
        keyCodeMap.put("$", 52);
        keyCodeMap.put("%", 53);
        keyCodeMap.put("^", 54);
        keyCodeMap.put("!", 49);
        keyCodeMap.put("@", 50);
        keyCodeMap.put("#", 51);
        keyCodeMap.put(")", 48);
        keyCodeMap.put("Tab", KeyEvent.VK_TAB);
        keyCodeMap.put("Q", 81);
        keyCodeMap.put("W", 87);
        keyCodeMap.put("E", 69);
        keyCodeMap.put("R", 82);
        keyCodeMap.put("T", 84);
        keyCodeMap.put("Y", 89);
        keyCodeMap.put("U", 85);
        keyCodeMap.put("I", 73);
        keyCodeMap.put("O", 79);
        keyCodeMap.put("P", 80);
        keyCodeMap.put("q", 81);
        keyCodeMap.put("w", 87);
        keyCodeMap.put("e", 69);
        keyCodeMap.put("r", 82);
        keyCodeMap.put("t", 84);
        keyCodeMap.put("y", 89);
        keyCodeMap.put("u", 85);
        keyCodeMap.put("i", 73);
        keyCodeMap.put("o", 79);
        keyCodeMap.put("p", 80);
        keyCodeMap.put("CapsLock", KeyEvent.VK_CAPS_LOCK);
        keyCodeMap.put("Caps", KeyEvent.VK_CAPS_LOCK);
        keyCodeMap.put("A", 65);
        keyCodeMap.put("S", 83);
        keyCodeMap.put("D", 68);
        keyCodeMap.put("F", 70);
        keyCodeMap.put("G", 71);
        keyCodeMap.put("H", 72);
        keyCodeMap.put("J", 74);
        keyCodeMap.put("K", 75);
        keyCodeMap.put("L", 76);
        keyCodeMap.put("a", 65);
        keyCodeMap.put("s", 83);
        keyCodeMap.put("d", 68);
        keyCodeMap.put("f", 70);
        keyCodeMap.put("g", 71);
        keyCodeMap.put("h", 72);
        keyCodeMap.put("j", 74);
        keyCodeMap.put("k", 75);
        keyCodeMap.put("l", 76);
        keyCodeMap.put("Z", 90);
        keyCodeMap.put("X", 88);
        keyCodeMap.put("C", 67);
        keyCodeMap.put("V", 86);
        keyCodeMap.put("B", 66);
        keyCodeMap.put("N", 78);
        keyCodeMap.put("M", 77);
        keyCodeMap.put("z", 90);
        keyCodeMap.put("x", 88);
        keyCodeMap.put("c", 67);
        keyCodeMap.put("v", 86);
        keyCodeMap.put("b", 66);
        keyCodeMap.put("n", 78);
        keyCodeMap.put("m", 77);
        keyCodeMap.put("Up", KeyEvent.VK_UP);
        keyCodeMap.put("Down", KeyEvent.VK_DOWN);
        keyCodeMap.put("Left", KeyEvent.VK_LEFT);
        keyCodeMap.put("Right", KeyEvent.VK_RIGHT);
        keyCodeMap.put("Ctrl", KeyEvent.VK_CONTROL);
        keyCodeMap.put("Control", KeyEvent.VK_CONTROL);
        keyCodeMap.put("Meta", KeyEvent.VK_META);
        keyCodeMap.put("Super", KeyEvent.VK_META);
        keyCodeMap.put("Win", KeyEvent.VK_META);
        keyCodeMap.put("Alt", KeyEvent.VK_ALT);
        keyCodeMap.put("Space", KeyEvent.VK_SPACE);
        keyCodeMap.put(" ", KeyEvent.VK_SPACE);
        keyCodeMap.put("ContextMenu", KeyEvent.VK_CONTEXT_MENU);
        keyCodeMap.put("Context", KeyEvent.VK_CONTEXT_MENU);
        keyCodeMap.put("Enter", KeyEvent.VK_ENTER);
        keyCodeMap.put("Shift", KeyEvent.VK_SHIFT);


        keyCodeMap.put("BackQuote", KeyEvent.VK_BACK_QUOTE);
        keyCodeMap.put("Semicolon", KeyEvent.VK_SEMICOLON);
        keyCodeMap.put("Quote", KeyEvent.VK_QUOTE);
        keyCodeMap.put("Comma", KeyEvent.VK_COMMA);
        keyCodeMap.put("Period", KeyEvent.VK_PERIOD);
        keyCodeMap.put("Slash", KeyEvent.VK_SLASH);
        keyCodeMap.put("OpenBracket", KeyEvent.VK_OPEN_BRACKET);
        keyCodeMap.put("CloseBracket", KeyEvent.VK_CLOSE_BRACKET);
        keyCodeMap.put("BackSlash", KeyEvent.VK_BACK_SLASH);
        keyCodeMap.put("Minus", KeyEvent.VK_MINUS);
        keyCodeMap.put("Equals", KeyEvent.VK_EQUALS);
        keyCodeMap.put("Slash", KeyEvent.VK_SLASH);


        keyCodeMap.put("`", KeyEvent.VK_BACK_QUOTE);
        keyCodeMap.put(";", KeyEvent.VK_SEMICOLON);
        keyCodeMap.put("'", KeyEvent.VK_QUOTE);
        keyCodeMap.put(",", KeyEvent.VK_COMMA);
        keyCodeMap.put(".", KeyEvent.VK_PERIOD);
        keyCodeMap.put("/", KeyEvent.VK_SLASH);
        keyCodeMap.put("[", KeyEvent.VK_OPEN_BRACKET);
        keyCodeMap.put("]", KeyEvent.VK_CLOSE_BRACKET);
        keyCodeMap.put("\\", KeyEvent.VK_BACK_SLASH);
        keyCodeMap.put("-", KeyEvent.VK_MINUS);
        keyCodeMap.put("=", KeyEvent.VK_EQUALS);
        keyCodeMap.put("/", KeyEvent.VK_SLASH);

        //make lower/upper case entry(s)
        ArrayList<Object[]> temp = new ArrayList<>();
        for (String text : keyCodeMap.keySet()) {
            String lower = text.toLowerCase();
            String upper = text.toUpperCase();
            if (!lower.equals(text)) {
                temp.add(new Object[]{lower, keyCodeMap.get(text)});
            }
            if (!upper.equals(text)) {
                temp.add(new Object[]{upper, keyCodeMap.get(text)});
            }
        }
        for (Object[] o : temp) {
            keyCodeMap.put((String) o[0], (Integer) o[1]);
        }



    }

    public boolean keyNeedsShift(String keyText) {
        return charsThatRequireShift.contains(keyText);
    }

    public ArrayList<String> createSupportedKeysList() {

        String[] keys = supportedKeys.split("\\n");
        ArrayList<String> keysList = new ArrayList<>();
        for (String key : keys) {
            keysList.add(key);
            keysList.add(key.toLowerCase());
        }
        return keysList;

    }
    static String help = "OPTIONS (think like settings)\n" +
            "-safety 0,0,j,space,meta,mouse1\n" +
            "\n" +
            "-duration X \n" +
            "\tterminate program after X milliseconds (timer starts after startDelay)\n" +
            "\n" +
            "-startDelay X \n" +
            "\ttime to wait before starting execution of actions\n" +
            "\n" +
            "-count X \n" +
            "\titterate X number of times over all actions\n" +
            "\n" +
            "-afterLoopDelay X\n" +
            "\twait X milliseconds after each iteration of all tasks regardless of how long it took to complete\n" +
            "\n" +
            "-every X \n" +
            "\tdo all actions every X milliseconds. It may take longer than X to complete an iteration\n" +
            "\tbut will always take at least X to complete an iteration\n" +
            "\n" +
            "\n" +
            "\n" +
            "ACTIONS (all actions are run in order, think like tasks in list)\n" +
            "\n" +
            "-autoWait X\n" +
            "\tset auto wait time between each press or release\n" +
            "\n" +
            "-wait X\n" +
            "\twait for x milliseconds before moving doing next action\n" +
            "\n" +
            "-text “Hello World!”\n" +
            "\ttype the text inside the quotes\n" +
            "\n" +
            "-shortcut ctrl,alt,delete\n" +
            "\tperform the shortcut, each key is separated by commas \n" +
            "\t*(do not put a comma as a key, it won’t be recognised.)\n" +
            "\n" +
            "\n" +
            "*press and release only pertain to -keys\n" +
            "\t-press true (keys in -key tag are pressed if true, not if false.)\n" +
            "\t-release true\n" +
            "-keys J,K,L,1,Meta\n" +
            "\tpress and release each key (press J, release J, press K, release K…)\n" +
            "\n" +
            "\n" +
            "\n" +
            "EXAMPLES\n" +
            "type -every 200 -duration 60000 -safety 0,0,mouse2,space -shortcut ctrl,alt,t -wait 1500 -text “ip address” -keys enter,enter\n" +
            "\n" +
            "\n" +
            "\n" +
            "SUPPORTED SAFETY KEYS\n" +
            "Backspace\n" +
            "backspace\n" +
            "Insert\n" +
            "insert\n" +
            "Home\n" +
            "home\n" +
            "Tab\n" +
            "tab\n" +
            "Q\n" +
            "q\n" +
            "W\n" +
            "w\n" +
            "E\n" +
            "e\n" +
            "R\n" +
            "r\n" +
            "T\n" +
            "t\n" +
            "Y\n" +
            "y\n" +
            "U\n" +
            "u\n" +
            "I\n" +
            "i\n" +
            "O\n" +
            "o\n" +
            "P\n" +
            "p\n" +
            "Delete\n" +
            "delete\n" +
            "End\n" +
            "end\n" +
            "Up\n" +
            "up\n" +
            "A\n" +
            "a\n" +
            "S\n" +
            "s\n" +
            "D\n" +
            "d\n" +
            "F\n" +
            "f\n" +
            "G\n" +
            "g\n" +
            "H\n" +
            "h\n" +
            "J\n" +
            "j\n" +
            "K\n" +
            "k\n" +
            "L\n" +
            "l\n" +
            "Semicolon\n" +
            "semicolon\n" +
            "Quote\n" +
            "quote\n" +
            "Enter\n" +
            "enter\n" +
            "Clear\n" +
            "clear\n" +
            "Shift\n" +
            "shift\n" +
            "Z\n" +
            "z\n" +
            "X\n" +
            "x\n" +
            "C\n" +
            "c\n" +
            "V\n" +
            "v\n" +
            "B\n" +
            "b\n" +
            "N\n" +
            "n\n" +
            "M\n" +
            "m\n" +
            "Comma\n" +
            "comma\n" +
            "Period\n" +
            "period\n" +
            "Slash\n" +
            "slash\n" +
            "Ctrl\n" +
            "ctrl\n" +
            "Meta\n" +
            "meta\n" +
            "Alt\n" +
            "alt\n" +
            "Space\n" +
            "space\n" +
            "Left\n" +
            "left\n" +
            "Down\n" +
            "down\n" +
            "Right\n" +
            "right\n" +
            "Space\n" +
            "space\n" +
            "\n" +
            "\n" +
            "SUPPORTED KEYS FOR TYPEING\n" +
            "Delete\n" +
            "Meta\n" +
            "PS\n" +
            "Left\n" +
            "ps\n" +
            "shift\n" +
            "Insert\n" +
            "CONTEXT\n" +
            "MINUS\n" +
            "CAPSLOCK\n" +
            "quote\n" +
            "BACKSLASH\n" +
            "Space\n" +
            " \n" +
            "!\n" +
            "META\n" +
            "openbracket\n" +
            "#\n" +
            "$\n" +
            "LEFT\n" +
            "%\n" +
            "&\n" +
            "'\n" +
            "(\n" +
            ")\n" +
            "*\n" +
            ",\n" +
            "-\n" +
            ".\n" +
            "/\n" +
            "0\n" +
            "1\n" +
            "2\n" +
            "3\n" +
            "4\n" +
            "5\n" +
            "left\n" +
            "6\n" +
            "meta\n" +
            "7\n" +
            "8\n" +
            "9\n" +
            ";\n" +
            "=\n" +
            "Minus\n" +
            "Backspace\n" +
            "@\n" +
            "A\n" +
            "minus\n" +
            "B\n" +
            "C\n" +
            "D\n" +
            "E\n" +
            "ctrl\n" +
            "F\n" +
            "G\n" +
            "H\n" +
            "I\n" +
            "J\n" +
            "K\n" +
            "L\n" +
            "M\n" +
            "N\n" +
            "O\n" +
            "P\n" +
            "SPACE\n" +
            "Q\n" +
            "R\n" +
            "DELETE\n" +
            "S\n" +
            "T\n" +
            "U\n" +
            "V\n" +
            "PAGEDOWN\n" +
            "W\n" +
            "X\n" +
            "Y\n" +
            "Z\n" +
            "[\n" +
            "\\\n" +
            "]\n" +
            "escape\n" +
            "backslash\n" +
            "^\n" +
            "Context\n" +
            "`\n" +
            "a\n" +
            "b\n" +
            "c\n" +
            "d\n" +
            "e\n" +
            "f\n" +
            "g\n" +
            "contextmenu\n" +
            "h\n" +
            "i\n" +
            "j\n" +
            "k\n" +
            "right\n" +
            "l\n" +
            "m\n" +
            "n\n" +
            "o\n" +
            "p\n" +
            "Semicolon\n" +
            "q\n" +
            "r\n" +
            "s\n" +
            "t\n" +
            "CTRL\n" +
            "u\n" +
            "v\n" +
            "w\n" +
            "x\n" +
            "y\n" +
            "z\n" +
            "CloseBracket\n" +
            "DOWN\n" +
            "f1\n" +
            "f2\n" +
            "f3\n" +
            "down\n" +
            "f4\n" +
            "f5\n" +
            "f6\n" +
            "f7\n" +
            "f8\n" +
            "f9\n" +
            "printscreen\n" +
            "BACKSPACE\n" +
            "up\n" +
            "UP\n" +
            "Shift\n" +
            "F1\n" +
            "Ctrl\n" +
            "F2\n" +
            "F3\n" +
            "F4\n" +
            "F5\n" +
            "F6\n" +
            "F7\n" +
            "F8\n" +
            "F9\n" +
            "Enter\n" +
            "End\n" +
            "END\n" +
            "Up\n" +
            "CONTROL\n" +
            "QUOTE\n" +
            "ContextMenu\n" +
            "caps\n" +
            "SHIFT\n" +
            "Control\n" +
            "backspace\n" +
            "end\n" +
            "Home\n" +
            "Escape\n" +
            "Down\n" +
            "F10\n" +
            "F12\n" +
            "F11\n" +
            "BackSlash\n" +
            "ESCAPE\n" +
            "home\n" +
            "Quote\n" +
            "pagedown\n" +
            "PageDown\n" +
            "CAPS\n" +
            "HOME\n" +
            "OpenBracket\n" +
            "f10\n" +
            "f12\n" +
            "f11\n" +
            "CapsLock\n" +
            "SEMICOLON\n" +
            "context\n" +
            "PRINTSCREEN\n" +
            "enter\n" +
            "Caps\n" +
            "period\n" +
            "Right\n" +
            "ENTER\n" +
            "Comma\n" +
            "PAGEUP\n" +
            "CONTEXTMENU\n" +
            "ESC\n" +
            "RIGHT\n" +
            "CLOSEBRACKET\n" +
            "COMMA\n" +
            "backquote\n" +
            "OPENBRACKET\n" +
            "comma\n" +
            "Tab\n" +
            "esc\n" +
            "TAB\n" +
            "equals\n" +
            "capslock\n" +
            "closebracket\n" +
            "BACKQUOTE\n" +
            "Alt\n" +
            "ALT\n" +
            "space\n" +
            "Super\n" +
            "tab\n" +
            "Slash\n" +
            "PageUp\n" +
            "alt\n" +
            "Period\n" +
            "PERIOD\n" +
            "INSERT\n" +
            "Win\n" +
            "WIN\n" +
            "PrintScreen\n" +
            "insert\n" +
            "pageup\n" +
            "delete\n" +
            "win\n" +
            "semicolon\n" +
            "SUPER\n" +
            "EQUALS\n" +
            "SLASH\n" +
            "control\n" +
            "super\n" +
            "Equals\n" +
            "BackQuote\n" +
            "slash";
}
