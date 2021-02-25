package com.codeErza.androidx86installer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.*;
import java.util.ArrayList;


import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    String bbox = null;
    String fs[] = {"None","ext4"};
    String workspace = "/data/local/tmp/mnt/andIns";
    String tBlock = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bbox = "/data/data/"+getPackageName()+"/busybox";
        copyAsset(getAssets(),"busybox",bbox);

        setSpinner(R.id.spnDevice,fetchDevices());
        setSpinner(R.id.spnFormat,fs);

    }


    public void setSpinner(int iId,String array[]){
        Spinner spinner = findViewById(iId);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(this,   android.R.layout.simple_spinner_item, array);
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setOnItemSelectedListener(this);
        spinner.setAdapter(spinnerArrayAdapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        tBlock = parent.getItemAtPosition(position).toString();
        runSu(bbox+" mkdir -p "+workspace);
        runSu(bbox+" mount "+tBlock+" "+workspace);
        setStatus(runSu(bbox+" ls -1 "+workspace));
        runSu(bbox+" umount "+workspace);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public String[] fetchDevices(){
        String[] data = null;
        String out = runShell("ls /dev/block/");

        Pattern r = Pattern.compile("([^\\n]+)");

        Matcher m = r.matcher(out);

        ArrayList<String> devices = new ArrayList<String>();

        while(m.find()){
            devices.add("/dev/block/"+m.group(1));
        }
        data = new String[devices.size()];

        Object[] objArr = devices.toArray();

        int i = 0;
        for (Object obj : objArr) {
            data[i++] = (String)obj;
        }
        return data;
    }

    public String runShell(String comnd){
        String data = null;
        try{
            Process sh = Runtime.getRuntime().exec("sh");
            DataOutputStream outputStream = new DataOutputStream(sh.getOutputStream());
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(sh.getInputStream()));
            BufferedReader stdErr = new BufferedReader(new InputStreamReader(sh.getErrorStream()));

            outputStream.writeBytes(comnd+"\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            sh.waitFor();
            data = "";
            String line = null;
            while ((line = stdInput.readLine()) != null) {
                data = data + "\n" + line;
            }
            while ((line = stdErr.readLine()) != null) {
                data = data + "\n" + line;
            }
        }catch(IOException e){
            reporter(e);
        }catch(InterruptedException e){
            reporter(e);
        }
        return data;
    }

    public String runSu(String comnd){
        String data = null;
        try{
            Process sh = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(sh.getOutputStream());
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(sh.getInputStream()));
            BufferedReader stdErr = new BufferedReader(new InputStreamReader(sh.getErrorStream()));

            outputStream.writeBytes(comnd+"\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            sh.waitFor();
            data = "";
            String line = null;
            while ((line = stdInput.readLine()) != null) {
                data = data + "\n" + line;
            }
            while ((line = stdErr.readLine()) != null) {
                data = data + "\n" + line;
            }
        }catch(IOException e){
            reporter(e);
        }catch(InterruptedException e){
            reporter(e);
        }
        return data;
    }

    public void reporter(IOException report){
        System.out.println(report);
    }

    public void reporter(InterruptedException report){
        System.out.println(report);
    }

    public void reporter(String report){
        System.out.println(report);
    }

    public void setStatus(String outTxt){
        TextView txt = findViewById(R.id.txtList);
        txt.setMovementMethod(new ScrollingMovementMethod());
        txt.setText(outTxt);
    }

    private boolean copyAsset(AssetManager assetManager,
                              String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    public void install(View view){
        EditText inputName = findViewById(R.id.inpName);
        EditText inputSize = findViewById(R.id.inpSize);
        String folderName = inputName.getText().toString();
        CheckBox box = findViewById(R.id.chkConfirm);

        String strSize = inputSize.getText().toString();
        if( strSize.equals("")){
            setStatus("Invalid size");
            return;
        }

        int size = Integer.parseInt(strSize);

        Pattern r = Pattern.compile("([a-zA-Z0-9_]+)");

        Matcher m = r.matcher(folderName);

        if(m.find()){
            if (folderName.equals(m.group(1))){
                if(box.isChecked()){
                    setStatus("Installing..");
                    runSu(bbox+" mount "+tBlock+" "+workspace);
                    runSu(bbox+" mkdir -p "+workspace+"/"+folderName);
                    setStatus("Copying system files..");
                    runSu(bbox+" cp /gearlock/gearroot/system.sfs "+workspace+"/"+folderName+"/");
                    runSu(bbox+" cp /gearlock/gearroot/kernel "+workspace+"/"+folderName+"/");
                    runSu(bbox+" cp /gearlock/gearroot/initrd.img "+workspace+"/"+folderName+"/");
                    runSu(bbox+" cp /gearlock/gearroot/ramdisk.img "+workspace+"/"+folderName+"/");
                    runSu(bbox+" cp /gearlock/gearroot/gearlock "+workspace+"/"+folderName+"/");
                    runSu(bbox+" dd if=/dev/zero of="+workspace+"/"+folderName+"/data.img bs=1G count="+size);
                    runSu(bbox+" mke2fs -t ext4 "+workspace+"/"+folderName+"/data.img");
                    runSu(bbox+" umount "+workspace);
                    setStatus("Install finished.");
                }else{
                    setStatus("Confirm partition");
                }
            }else{
                setStatus("Invalid name");
            }
        }else{
            setStatus("Invalid name");
        }
    }
}